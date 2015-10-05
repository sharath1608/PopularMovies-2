package app.sunshine.android.example.com.popmovies;


import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;
import com.android.popmovies.app.data.MoviesContract.FavoriteEntry;
import com.android.popmovies.app.data.MoviesContract.MoviesEntry;
import com.android.popmovies.app.data.MoviesContract.ReviewsEntry;
import com.android.popmovies.app.data.MoviesContract.TrailerEntry;
import com.android.popmovies.app.data.MoviesContract.CastEntry;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private String movieId;
    private final int MOVIE_LOADER = 0;
    private final int FAV_LOADER = 1;
    private final int TRAILER_LOADER = 2;
    private final int REVIEW_LOADER = 3;
    private final int CAST_LOADER = 4;
    private Toast commonToast;
    private Target loadTarget;
    private final String API_REQ_STRING = "api_key";
    private final String detail_key = "detail_key";
    private boolean isReviewInDB;
    private boolean isTrailerInDB;
    private CastViewAdapter castAdapter;
    private RequestQueue mRequestQueue;
    private ShareActionProvider mShareActionProvider;
    private DetailMovieData thisMovieData;
    private String LOG_TAG;
    private boolean isFavorite;
    private boolean isMovieInDB;
    private boolean isCastInDB;
    private boolean isSavedState;
    private FloatingActionButton fab;
    private Snackbar favSnackBar;
    private int viewCount;
    private TextView movieRatingView;
    private TextView movieTitleView;
    private TextView movieDateView;
    private TextView movieDurationView;
    private TextView movieDescriptionView;
    private ImageView movieImageView;
    private String favToggleString;
    private ProgressDialog progDialog;


    public DetailActivityFragment(){
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        isTrailerInDB = false;
        isCastInDB = false;
        isReviewInDB = false;
        isMovieInDB = false;
        isSavedState = false;
        mRequestQueue = Volley.newRequestQueue(getActivity());
        thisMovieData = new DetailMovieData();
        movieId = getArguments().getString(Intent.EXTRA_TEXT);

        if (savedInstanceState != null) {
            thisMovieData = (DetailMovieData) savedInstanceState.get(detail_key);
            setFlags();
            isSavedState = true;
        }

        progDialog = new ProgressDialog(getActivity());
        progDialog.setMessage(getString(R.string.progress_text_detail));
        progDialog.setIndeterminate(true);
        progDialog.show();

        movieTitleView = (TextView) view.findViewById(R.id.movie_title_detail);
        movieRatingView = (TextView) view.findViewById(R.id.move_rating_detail);
        movieDateView = (TextView) view.findViewById(R.id.movie_date_detail);
        movieDurationView = (TextView) view.findViewById(R.id.movie_time_detail);
        movieDescriptionView = (TextView) view.findViewById(R.id.movie_description_detail);
        movieImageView = (ImageView) view.findViewById(R.id.movie_image_detail);

        castAdapter = new CastViewAdapter(new ArrayList<CastViewObject>());
        fab = (FloatingActionButton) view.findViewById(R.id.movie_fav_fab);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.cast_list_view);
        LinearLayoutManager castLayoutManager = new LinearLayoutManager(getActivity());
        castLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        castLayoutManager.requestSimpleAnimationsInNextLayout();
        recyclerView.setLayoutManager(castLayoutManager);
        castAdapter.setOnItemClickListener(new CastViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                CastViewObject chosenCast = thisMovieData.getCasts().get(position);
                mRequestQueue.add(getPersonRequest(chosenCast.getCastId()));
            }
        });
        recyclerView.setAdapter(castAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        viewCount = 0;
        if(!isSavedState) {
            getLoaderManager().initLoader(MOVIE_LOADER, null, this);
            getLoaderManager().initLoader(FAV_LOADER, null, this);
            getLoaderManager().initLoader(REVIEW_LOADER, null, this);
            getLoaderManager().initLoader(TRAILER_LOADER, null, this);
            getLoaderManager().initLoader(CAST_LOADER, null, this);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Loader detailLoader = null;
        switch (id) {
            case MOVIE_LOADER:
                detailLoader = new CursorLoader(getActivity(),
                        MoviesEntry.CONTENT_URI,
                        null,
                        MoviesEntry.TABLE_NAME + "." + MoviesEntry.COLUMN_MOVIE_ID + " = ? ",
                        new String[]{movieId},
                        null);
                break;
            case FAV_LOADER:
                detailLoader = new CursorLoader(getActivity(),
                        FavoriteEntry.buildFavDetailWithID(movieId),
                        new String[]{FavoriteEntry.TABLE_NAME+"."+FavoriteEntry._ID},
                        null,
                        null,
                        null);
                break;
            case REVIEW_LOADER:
                detailLoader = new CursorLoader(getActivity(),
                        ReviewsEntry.buildReviewsWithID(movieId),
                        new String[]{ReviewsEntry._ID},
                        null,
                        null,
                        null);
                break;

            case TRAILER_LOADER:
                detailLoader = new CursorLoader(getActivity(),
                        TrailerEntry.buildTrailerswithID(movieId),
                        new String[]{TrailerEntry._ID},
                        null,
                        null,
                        null);
                break;
            case CAST_LOADER:
                detailLoader = new CursorLoader(getActivity(),
                        CastEntry.buildCastWithID(movieId),
                        new String[]{CastEntry.COLUMN_CAST_ID},
                        null,
                        null,
                        null);
                break;
        }
        return detailLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()) {
            case MOVIE_LOADER:
                viewCount++;
                if (data.moveToFirst()) {
                    isMovieInDB = true;
                    thisMovieData.setMovieID(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_MOVIE_ID)));
                    thisMovieData.setImageUrl(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_POSTER)));
                    thisMovieData.setDescription(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_OVERVIEW)));
                    thisMovieData.setDuration(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_RUNTIME)));
                    thisMovieData.setRating(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_RATING)));
                    thisMovieData.setYear(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_RELEASE_YEAR)));
                    thisMovieData.setTitle(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_MOVIE_TITLE)));
                    thisMovieData.setBackdropUrl(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_BACKDROP)));
                } else {
                    isMovieInDB = false;
                }

                break;
            case FAV_LOADER:
                isFavorite = data.moveToFirst();
                setFabColor();
                break;

            case REVIEW_LOADER:
                viewCount++;
                Review[] reviewsDB;
                if (data.moveToFirst()) {
                    isReviewInDB = true;
                    reviewsDB = new Review[data.getCount()];
                    int index = 0;
                    do {
                        String author = data.getString(data.getColumnIndex(ReviewsEntry.COLUMN_REVIEW_AUTHOR));
                        String reviewText = data.getString(data.getColumnIndex(ReviewsEntry.COLUMN_REVIEW_TEXT));
                        Review thisReview = new Review();
                        thisReview.setAuthor(author);
                        thisReview.setReviewText(reviewText);
                        reviewsDB[index++] = thisReview;
                    } while (data.moveToNext());
                } else {
                    Review reviewObject = new Review();
                    reviewObject.setReviewText(getString(R.string.no_review_text));
                    reviewObject.setAuthor("");
                    reviewsDB = new Review[]{reviewObject};
                }
                thisMovieData.setReviews(reviewsDB);
                break;

            case TRAILER_LOADER:
                viewCount++;
                if (data.moveToFirst()) {
                    isTrailerInDB = true;
                    int index = 0;
                    Trailer[] trailersDB = new Trailer[data.getCount()];
                    do {
                        Trailer thisTrailer = new Trailer();
                        thisTrailer.setTrailerId(data.getString(data.getColumnIndex(TrailerEntry.COLUMN_TRAILER_ID)));
                        thisTrailer.setTrailerName(data.getString(data.getColumnIndex(TrailerEntry.COLUMN_TRAILER_TITLE)));
                        thisTrailer.setSource(data.getString(data.getColumnIndex(TrailerEntry.COLUMN_TRAILER_URI)));
                        trailersDB[index++] = thisTrailer;
                    } while (data.moveToNext());
                    thisMovieData.setTrailers(trailersDB);
                }
                break;

            case CAST_LOADER:
                viewCount++;
                if (data.moveToFirst()) {
                    ArrayList<CastViewObject> castsDB = new ArrayList<CastViewObject>();
                    isCastInDB = true;
                    do {
                        String castName = data.getString(data.getColumnIndex(CastEntry.COLUMN_CAST_NAME));
                        String castURI = data.getString(data.getColumnIndex(CastEntry.COLUMN_CAST_URI));
                        String castID = data.getString(data.getColumnIndex(CastEntry.COLUMN_CAST_ID));
                        CastViewObject cast = new CastViewObject(castName, castURI, castID);
                        castsDB.add(cast);
                    } while (data.moveToNext());
                    thisMovieData.setCasts(castsDB);
                }
                break;
            default:
                throw new UnsupportedOperationException("loader could not be matched to pre-defined loaders");
        }

        // If movie details are not present in DB, spawn volley requests to fetch from DB and insert it.
        if (viewCount == 4 ) {
            if (isMovieInDB) {
                setFragmentViews();
            }else{
                if (!Utilities.isNetworkAvailable(getActivity())) {
                    if(progDialog!=null && progDialog.isShowing())
                        progDialog.dismiss();
                    commonToast = Toast.makeText(getActivity(), getString(R.string.no_connect_string), Toast.LENGTH_SHORT);
                    commonToast.show();
                }
                requestMovieDetails();
            }
            viewCount = 0;
        }
    }

    public void onMovieIDChanged(String id){
        movieId = id;
        getLoaderManager().restartLoader(MOVIE_LOADER,null,this);
        getLoaderManager().restartLoader(FAV_LOADER,null,this);
        getLoaderManager().restartLoader(REVIEW_LOADER,null,this);
        getLoaderManager().restartLoader(TRAILER_LOADER, null, this);
        getLoaderManager().restartLoader(FAV_LOADER, null, this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void saveMovieDetails() {
        LOG_TAG = getClass().getSimpleName();
        long movieRowID;
        int numRows;
        if (!isMovieInDB) {
            ContentValues movieValues = new ContentValues();
            ContentValues favValues = new ContentValues();
            movieValues.put(MoviesEntry._ID,42);
            movieValues.put(MoviesEntry.COLUMN_MOVIE_ID, thisMovieData.getMovieID());
            movieValues.put(MoviesEntry.COLUMN_MOVIE_TITLE, thisMovieData.getTitle());
            movieValues.put(MoviesEntry.COLUMN_RUNTIME, thisMovieData.getDuration());
            movieValues.put(MoviesEntry.COLUMN_BACKDROP, thisMovieData.getBackdropUrl());
            movieValues.put(MoviesEntry.COLUMN_POSTER, thisMovieData.getImageUrl());
            movieValues.put(MoviesEntry.COLUMN_RELEASE_YEAR, thisMovieData.getYear());
            movieValues.put(MoviesEntry.COLUMN_OVERVIEW, thisMovieData.getDescription());
            movieValues.put(MoviesEntry.COLUMN_RATING, thisMovieData.getRating());
            favValues.put(FavoriteEntry.COLUMN_MOVIE_ID, movieId);

            Uri movieUri = getActivity().getContentResolver().insert(MoviesEntry.CONTENT_URI, movieValues);
            movieRowID = ContentUris.parseId(movieUri);
            if (!(movieRowID > 0)) {
                Log.e(LOG_TAG, "Error inserting favorite to database");
            } else {
                isMovieInDB = true;
            }
        }

        if (!isTrailerInDB) {
            ContentValues[] trailerValues = new ContentValues[thisMovieData.getTrailers().length];
            int index = 0;
            for (Trailer trailer : thisMovieData.getTrailers()) {
                ContentValues value = new ContentValues();
                value.put(TrailerEntry.COLUMN_MOVIE_ID, movieId);
                value.put(TrailerEntry.COLUMN_TRAILER_TITLE, trailer.getTrailerName());
                value.put(TrailerEntry.COLUMN_TRAILER_URI, trailer.getSource());
                value.put(TrailerEntry.COLUMN_TRAILER_ID, trailer.getTrailerId());
                trailerValues[index++] = value;
            }
            numRows = getActivity().getContentResolver().bulkInsert(TrailerEntry.buildTrailerswithID(movieId), trailerValues);

            // If no trailers/reviews/cast present, the viewCount must increment.
            if (numRows > 0) {
                isTrailerInDB = true;
            } else{
                viewCount++;
            }
        }

        if (!isCastInDB) {
            List<CastViewObject> casts = thisMovieData.getCasts();
            ContentValues[] castValues = new ContentValues[casts.size()];
            int index = 0;
            for (CastViewObject cast : casts) {
                ContentValues value = new ContentValues();
                value.put(CastEntry.COLUMN_CAST_ID, cast.getCastId());
                value.put(CastEntry.COLUMN_CAST_NAME, cast.getCastName());
                value.put(CastEntry.COLUMN_CAST_URI, cast.getCastImageUrl());
                value.put(CastEntry.COLUMN_MOVIE_ID, thisMovieData.getMovieID());
                castValues[index++] = value;
            }
            numRows = getActivity().getContentResolver().bulkInsert(CastEntry.buildCastWithID(movieId), castValues);
            if (numRows > 0) {
                isCastInDB = true;
            } else{
                viewCount++;
            }
        }
    }

    public void saveReviewDetails() {
        if (!isReviewInDB) {
            ContentValues[] reviewValues = new ContentValues[thisMovieData.getReviews().length];
            int index = 0;
            Review[] movieReviews = thisMovieData.getReviews();
            for (Review review : movieReviews) {
                ContentValues values = new ContentValues();
                values.put(ReviewsEntry.COLUMN_REVIEW_AUTHOR, "-"+review.getAuthor());
                values.put(ReviewsEntry.COLUMN_MOVIE_ID, movieId);
                values.put(ReviewsEntry.COLUMN_REVIEW_TEXT, review.getReviewText());
                reviewValues[index++] = values;
            }
            getActivity().getContentResolver().bulkInsert(ReviewsEntry.buildReviewsWithID(movieId), reviewValues);
        }
    }

    public void saveFlags(){
        thisMovieData.setMovieInDB(String.valueOf(isMovieInDB));
        thisMovieData.setFavorite(String.valueOf(isFavorite));
        thisMovieData.setCastInDB(String.valueOf(isCastInDB));
        thisMovieData.setReviewInDB(String.valueOf(isReviewInDB));
        thisMovieData.setTrailerInDB(String.valueOf(isTrailerInDB));
    }

    public void setFlags(){
        isMovieInDB = thisMovieData.getMovieInDB();
        isFavorite = thisMovieData.getFavorite();
        isCastInDB = thisMovieData.getCastInDB();
        isReviewInDB = thisMovieData.getReviewInDB();
        isTrailerInDB = thisMovieData.getTrailerInDB();
    }

    public void insertFavorite(DetailMovieData movieData) {
        LOG_TAG = getClass().getSimpleName();

        if (isMovieInDB) {

            // insert movie as favorite if not already present.
            if (!isFavorite) {
                ContentValues favValues = new ContentValues();
                favValues.put(FavoriteEntry.COLUMN_MOVIE_ID, movieData.getMovieID());
                Uri favUri = getActivity().getContentResolver().insert(FavoriteEntry.CONTENT_URI, favValues);

                long favRowID = ContentUris.parseId(favUri);
                if (!(favRowID > 0)) {
                    Log.e(LOG_TAG, "Error inserting favorite to database");
                } else {
                    if (favSnackBar != null) {
                        favSnackBar.dismiss();
                    }
                    favToggleString = getString(R.string.fav_add_success);
                }

                // If already marked as favorite, remove from DB.
            } else {

                getActivity().getContentResolver().delete(FavoriteEntry.CONTENT_URI,
                        FavoriteEntry.COLUMN_MOVIE_ID + "= ?",
                        new String[]{movieData.getMovieID()});
                if (favSnackBar != null) {
                    favSnackBar.dismiss();
                }
                favToggleString = getString(R.string.fav_remove_success);
            }
            isFavorite = !isFavorite;
            setFabColor();
        } else {
            favToggleString = getString(R.string.fav_add_failure);
        }
        commonToast = Toast.makeText(getActivity(), favToggleString, Toast.LENGTH_SHORT);
        commonToast.show();
    }

    @Override
    public void onStart() {
        super.onStart();

        if(isSavedState){
            isSavedState = false;
            setFragmentViews();
        }
    }

    public void setFabColor(){
        if(isFavorite){
            fab.setBackgroundColor(getResources().getColor(R.color.yellow_A400));
        }else{
            fab.setBackgroundColor(getResources().getColor(R.color.white_common));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveFlags();
        outState.putParcelable(detail_key, thisMovieData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commonToast != null)
            commonToast.cancel();
    }

    public JsonObjectRequest getBasicInfoRequest() {
        final String apiKey = getString(R.string.api_key);
        final String DETAIL_BASE_URI = getString(R.string.details_base_path);
        String appendAttr = getString(R.string.appendAttr);
        final String APPEND_TO_RESPONSE = "append_to_response";

        Uri baseRequestUri = Uri.parse(DETAIL_BASE_URI).buildUpon().appendPath(movieId)
                .appendQueryParameter(API_REQ_STRING, apiKey).appendQueryParameter(APPEND_TO_RESPONSE, appendAttr).build();

        final String LOG_TAG = getClass().getSimpleName();
        JsonObjectRequest primaryJsonReq = new JsonObjectRequest(Request.Method.GET, baseRequestUri.toString(), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {
                    String posterSize = getString(R.string.detail_poster_size);
                    parseDetailsFromJson(response.toString(), posterSize);
                    if (!isMovieInDB) {
                        saveMovieDetails();
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error reading the response");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error getting response for the JSON request");
            }
        });
        return primaryJsonReq;
    }

    public JsonObjectRequest getReviewInfoRequest() {
        final String REVIEW_TAG = "reviews";
        final String apiKey = getString(R.string.api_key);
        final String DETAIL_BASE_URI = getString(R.string.details_base_path);


        Uri reviewRequestUri = Uri.parse(DETAIL_BASE_URI).buildUpon().appendPath(movieId).appendPath(REVIEW_TAG)
                .appendQueryParameter(API_REQ_STRING, apiKey).build();
        final String LOG_TAG = getClass().getSimpleName();


        JsonObjectRequest reviewJasonRequest = new JsonObjectRequest(Request.Method.GET, reviewRequestUri.toString(), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {
                    parseReviewDetails(response);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error reading the response");
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error getting response for the JSON request");
            }
        });

        return reviewJasonRequest;
    }


    public void parseReviewDetails(JSONObject JsonResp) throws JSONException {
        final String RESULT_TAG = "results";
        final String CONTENT_TAG = "content";
        final String AUTHOR_TAG = "author";
        JSONArray resultsArray = JsonResp.getJSONArray(RESULT_TAG);
        Review[] reviewArray = new Review[resultsArray.length()];

        // Parse review content and add to reviewList to be displayed on the detail screen and fullReviewList which will be displayed in a dialog.
        for (int i = 0; i < resultsArray.length(); i++) {
            Review reviewObject = new Review();
            JSONObject result = resultsArray.getJSONObject(i);
            String reviewString = result.getString(CONTENT_TAG);
            String author = result.getString(AUTHOR_TAG);
            reviewObject.setAuthor(author);
            reviewObject.setReviewText(reviewString);
            reviewArray[i] = reviewObject;
        }

        if (resultsArray.length() == 0) {
            Review reviewObject = new Review();
            reviewObject.setReviewText(getString(R.string.no_review_text));
            reviewObject.setAuthor(" ");
            reviewArray = new Review[]{reviewObject};
        }

        // While reviewArray contains data for both scenarios where reviews exist and not, text is saved only when it exists.
        thisMovieData.setReviews(reviewArray);

        if (resultsArray.length() > 0) {
            saveReviewDetails();
        } else {
            viewCount++;
        }
    }

    public void setReviewViews() {
        final Review[] reviewObjects = thisMovieData.getReviews();
        LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.review_layout);
        for (int pos = 0; pos < reviewObjects.length; pos++) {
            final String thisReview = reviewObjects[pos].getReviewText();
            final String thisAuthor = reviewObjects[pos].getAuthor();
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String shortenedView = Utilities.snipTextForView(thisReview) + thisAuthor;
            View v = inflater.inflate(R.layout.review_item_layout, null);
            TextView reviewText = (TextView) v.findViewById(R.id.movie_review_text);

            if(isReviewInDB) {
                reviewText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        extendedInfoPopUp(thisReview + thisAuthor, getString(R.string.review));
                    }
                });
            }

            reviewText.setText(shortenedView);
            layout.addView(v);
        }
    }

    // Launch another volley request to fetch cast Id in order to get imdb ID subsequently.
    public JsonObjectRequest getPersonRequest(String personId) {
        final String PERSON_TAG = "person";
        final String apiKey = getString(R.string.api_key);
        final String BASE_URI = getString(R.string.tmdb_base_uri);
        Uri reviewRequestUri = Uri.parse(BASE_URI).buildUpon().appendPath(PERSON_TAG).appendPath(personId)
                .appendQueryParameter(API_REQ_STRING, apiKey).build();
        final String LOG_TAG = getClass().getSimpleName();

        final JsonObjectRequest personJsonRequest = new JsonObjectRequest(Request.Method.GET, reviewRequestUri.toString(), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    String imdbId = fetchImdbId(response.toString());
                    if (!imdbId.equals("")) {
                        Uri imdbPage = Uri.parse(getString(R.string.imdb_base_uri) + imdbId);
                        Intent mIntent = new Intent(Intent.ACTION_VIEW, imdbPage);
                        if (mIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(mIntent);
                        }
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.no_detals_text), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error reading the response");
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error getting response for the JSON request");
            }
        });
        return personJsonRequest;
    }

    public String fetchImdbId(String JsonRespString) throws JSONException {
        JSONObject jsonObject = new JSONObject(JsonRespString);
        return jsonObject.getString(getString(R.string.imdbId));
    }

    public void requestMovieDetails() {
        mRequestQueue.add(getBasicInfoRequest());
        mRequestQueue.add(getReviewInfoRequest());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem shareMenuItem;
        super.onCreateOptionsMenu(menu, inflater);

        shareMenuItem = menu.findItem(R.id.menu_movie_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
        if(thisMovieData.getTrailers()!=null){
            setShareProvider();
        }

    }

    public void parseDetailsFromJson(String detailsJsonString, String posterSize) throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String MOVIE_ID = "id";
        final String POSTER_TAG = "poster_path";
        final String BASE_POSTER_PATH = getString(R.string.poster_base_path);
        final String TITLE_TAG = "title";
        final String OVERVIEW_TAG = "overview";
        final String RUNTIME_TAG = "runtime";
        final String VOTE_AVERAGE_TAG = "vote_average";
        final String RELEASE_DATE_TAG = "release_date";
        final String BACKDROP_TAG = "backdrop_path";
        final String TRAILERS_TAG = "trailers";
        final String YOUTUBE_TAG = "youtube";
        final String TRAILER_NAME_TAG = "name";
        final String TRAILER_SOURCE_TAG = "source";
        final String CREDITS_TAG = "credits";
        final String CAST_TAG = "cast";
        final String CAST_NAME_TAG = "name";
        final String CAST_PROFILE_TAG = "profile_path";
        final String CAST_ID = "id";
        ArrayList<CastViewObject> castObjectArray;

        JSONObject detailsJson = new JSONObject(detailsJsonString);
        String imageUrl = BASE_POSTER_PATH + posterSize + detailsJson.getString(POSTER_TAG);
        String backdropUrl = BASE_POSTER_PATH + "/w1920" + detailsJson.getString(BACKDROP_TAG);
        String date = detailsJson.getString(RELEASE_DATE_TAG);
        String[] parsedDate = date.split("-");

        // Set the detailMovieData object
        thisMovieData.setMovieID(detailsJson.getString(MOVIE_ID));
        thisMovieData.setImageUrl(imageUrl);
        thisMovieData.setDescription(detailsJson.getString(OVERVIEW_TAG));
        thisMovieData.setDuration(detailsJson.getString(RUNTIME_TAG));
        thisMovieData.setRating(detailsJson.getString(VOTE_AVERAGE_TAG));
        thisMovieData.setYear(parsedDate[0]);
        thisMovieData.setTitle(detailsJson.getString(TITLE_TAG));
        thisMovieData.setBackdropUrl(backdropUrl);

        JSONObject trailerObject = detailsJson.getJSONObject(TRAILERS_TAG);
        JSONArray YtTrailerArray = trailerObject.getJSONArray(YOUTUBE_TAG);

        Trailer[] trailers = new Trailer[YtTrailerArray.length()];
        for (int i = 0; i < YtTrailerArray.length(); i++) {
            String trailerName = YtTrailerArray.getJSONObject(i).getString(TRAILER_NAME_TAG);
            String trailerId = YtTrailerArray.getJSONObject(i).getString(TRAILER_SOURCE_TAG);
            String trailerUrl = getString(R.string.youtube_base_uri) + trailerId;
            trailers[i] = new Trailer(trailerName, trailerUrl, trailerId);
        }

        JSONObject creditsObject = detailsJson.getJSONObject(CREDITS_TAG);
        JSONArray castsJsonArray = creditsObject.getJSONArray(CAST_TAG);
        castObjectArray = new ArrayList<>();
        for (int i = 0; i < castsJsonArray.length(); i++) {
            String castName = castsJsonArray.getJSONObject(i).getString(CAST_NAME_TAG);
            String castProfile = castsJsonArray.getJSONObject(i).getString(CAST_PROFILE_TAG);
            String castImageUrl = getString(R.string.cast_profile_baseURI) + castProfile;
            String castID = castsJsonArray.getJSONObject(i).getString(CAST_ID);
            castObjectArray.add(new CastViewObject(castName, castImageUrl, castID));
        }
        thisMovieData.setTrailers(trailers);
        thisMovieData.setCasts(castObjectArray);
    }

    public void setShareProvider() {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    public Intent createShareIntent() {
        Intent shareIntent;
        shareIntent = new Intent(Intent.ACTION_SEND);
        String shareString = getString(R.string.share_base_string_1)
                + ": "
                + thisMovieData.getTrailers()[0].getSource()
                + " "
                + getString(R.string.share_base_string_2)
                + " '"
                + thisMovieData.getTitle()
                + "' "
                + getString(R.string.share_base_string3);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareString);
        return shareIntent;
    }

    public void loadBackgroundImage(String backdropUrl) {
        if (loadTarget == null) {
            loadTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                    // This is to fix crash that occurs rarely, when accessing details in very quick succession.
                    if(getActivity()!=null) {
                        LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.movie_backdrop_layout);
                        layout.setBackground(new BitmapDrawable(bitmap));
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            PicassoImageCache.
                    getPicassoInstance(getActivity())
                    .load(backdropUrl)
                    .error(R.drawable.user_placeholder_image)
                    .into(loadTarget);
        }
    }


    public void setFragmentViews() {
        String LOG_TAG = this.getClass().getSimpleName();
        try {

            ImageView starImageView = (ImageView) getActivity().findViewById(R.id.starImage);
            starImageView.setImageResource(R.drawable.full_star);
            setFabColor();
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertFavorite(thisMovieData);
                }
            });

            PicassoImageCache
                    .getPicassoInstance(getActivity())
                    .load(thisMovieData.getImageUrl())
                    .error(R.drawable.user_placeholder_image)
                    .into(movieImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            loadBackgroundImage(thisMovieData.getBackdropUrl());
                        }

                        @Override
                        public void onError() {

                        }
                    });

            // Enable screen-wide touch and disable progress dialog.
            //touchmask.enable_touchmask(false);
            progDialog.dismiss();

            // Some foreign movies display duration = 0 and description as "null".
            movieRatingView.setText(thisMovieData.getRating());
            movieDateView.setText(thisMovieData.getYear());

            if (thisMovieData.getDuration() == null || Integer.parseInt(thisMovieData.getDuration()) == 0) {
                movieDurationView.setText("N/A");
            } else {
                movieDurationView.setText(thisMovieData.getDuration() + getString(R.string.minutes));
            }

            if (thisMovieData.getDescription() == null || (thisMovieData.getDescription().length() == 0)) {
                movieDescriptionView.setText(getString(R.string.no_overview));
            } else {
                final String description = thisMovieData.getDescription();
                String shortDescription = Utilities.snipTextForView(description);
                movieDescriptionView.setText(shortDescription);
                movieDescriptionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        extendedInfoPopUp(description, getString(R.string.overview));
                    }
                });
            }

            setCastView();
            movieTitleView.setText(thisMovieData.getTitle());
            setTrailerViews();
            setReviewViews();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in displaying movie details");
        }
    }


    public void extendedInfoPopUp(String extendedInfo, String popupTitle) {
        final AlertDialog builder = new AlertDialog.Builder(getActivity()).create();
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.review_dialog_layout, null);
        builder.setView(view);
        TextView textView = (TextView) view.findViewById(R.id.review_dialog_text);
        textView.setText(extendedInfo);
        TextView title = new TextView(getActivity());
        title.setText(popupTitle);
        title.setPadding(10, 25, 10, 20);
        title.setTextSize(21);
        title.setTextColor(getResources().getColor(R.color.title_text_color));
        title.setBackgroundColor(getResources().getColor(R.color.indigo_500));
        title.setGravity(Gravity.CENTER);
        builder.setCustomTitle(title);
        Button dismissButton = (Button) view.findViewById(R.id.dialog_dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
            }
        });

        builder.show();
    }

    public void setCastView() {
        List<CastViewObject> castViewList = thisMovieData.getCasts();
        castAdapter.setCastViewList(castViewList);
        castAdapter.notifyDataSetChanged();
    }

    public void setTrailerViews() {
        Trailer[] trailerData = thisMovieData.getTrailers();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.trailer_layout);

        if (trailerData == null || trailerData.length == 0) {
            View v = inflater.inflate(R.layout.trailer_item_layout, null);
            TextView infoText = (TextView) v.findViewById(R.id.trailer_name);
            ImageView playImage = (ImageView) v.findViewById(R.id.video_play_image);
            playImage.setVisibility(View.INVISIBLE);
            infoText.setText(getString(R.string.no_trailer_text));
            layout.addView(v);
        } else {
            for (int pos = 0; pos < trailerData.length; pos++) {
                View v = inflater.inflate(R.layout.trailer_item_layout, null);
                TextView trailerText = (TextView) v.findViewById(R.id.trailer_name);
                ImageView playImage = (ImageView) v.findViewById(R.id.video_play_image);
                final Trailer currentTrailer = trailerData[pos];
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent ytIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd:youtube:" + currentTrailer.getTrailerId()));
                            startActivity(ytIntent);
                        } catch (ActivityNotFoundException e) {
                            Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentTrailer.getSource()));
                            startActivity(uriIntent);
                        }
                    }
                });

                trailerText.setText(trailerData[pos].getTrailerName());
                setShareProvider();
                playImage.setVisibility(View.VISIBLE);
                layout.addView(v);
            }
        }
    }
}