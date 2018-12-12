package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {


    public static final String ARG_ITEM_ID = "item_id";
    private long mItemId;
    private Cursor mCursor;
    private static final String TAG = "ArticleDetailFragment";
    private View mRootView;
    private ProgressBar loadingPb;
    private TextView mBylineView;
    private TextView mTitleView;
    private TextView mBodyView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private int mMutedColor = 0xFFFFFFFF;
    private ImageView mPhotoView;


    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mBylineView = mRootView.findViewById(R.id.article_byline);
        mTitleView = mRootView.findViewById(R.id.article_title);
        mBodyView = mRootView.findViewById(R.id.article_body);
        mPhotoView = mRootView.findViewById(R.id.photo);
        loadingPb = mRootView.findViewById(R.id.loading_pb);

        mBylineView.setMovementMethod(new LinkMovementMethod());

        getActivityCast().setSupportActionBar((Toolbar) mRootView.findViewById(R.id.toolbar));
        getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getActivityCast().getSupportActionBar().setTitle(mTitleView.getText().toString());


        ///

        Typeface font = Typeface.createFromAsset(getActivityCast().getAssets() , "OkojoPro-Regular.otf");
        mBodyView.setTypeface(font);

        ///

        Toolbar mToolbar = mRootView.findViewById(R.id.toolbar);
        if (mToolbar != null) {

            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
        }



        bindViews();
        return mRootView;



 }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }



    private void bindViews() {
        if (mRootView == null) return;


        if (mCursor != null) {


            setLoading(false);

            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                            //    mMutedColor = p.getDarkMutedColor(mMutedColor);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mRootView.findViewById(R.id.meta_bar)
                                    //    .setBackgroundColor(mMutedColor)
                                ;
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            volleyError.printStackTrace();
                        }
                    });
        } else {
            setLoading(true);

        }

    }



    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            loadingPb.setVisibility(View.VISIBLE);
            mRootView.findViewById(R.id.scrollview).setVisibility(View.INVISIBLE);
        }else{
            loadingPb.setVisibility(View.INVISIBLE);
            mRootView.findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;

    }

    private void showSnackbar(String text) {
        Snackbar.make(mRootView.findViewById(R.id.root_layout), text, Snackbar.LENGTH_SHORT)
                .show();
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            showSnackbar(getString(R.string.error_reading_cursor));

            mCursor.close();
            mCursor = null;
        }


        bindViews();

    }


}
