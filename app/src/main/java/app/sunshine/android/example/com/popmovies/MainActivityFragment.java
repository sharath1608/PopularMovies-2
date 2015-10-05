package app.sunshine.android.example.com.popmovies;


import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import com.android.popmovies.app.data.MoviesContract.MoviesEntry;
import com.android.popmovies.app.data.MoviesContract.FavoriteEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ArrayAdapter<GridViewObject> movieGridAdapter;
    private static List<String> movieIds;
    private CursorAdapter favCursorAdapter;
    private static List<GridViewObject> gridViewObjects;
    private String sortByValue;
    private String movieKey = "movieKey";
    private int position;
    private GridView gridView;
    private Toast commonToast;
    private static int pagenum;
    private int rogueCounter;
    private boolean orientationChanged;
    private final String spinPosKey = "sortPos";
    private final String spinStringKey = "sortString";
    private boolean nextPageCalling;
    private String favSortString;
    private SharedPreferences prefs;
    private ProgressDialog progDialog;
    private CursorLoader favLoader;
    private String[] cursorSelection = {MoviesEntry.TABLE_NAME + "." + MoviesEntry._ID, MoviesEntry.COLUMN_MOVIE_ID, MoviesEntry.COLUMN_MOVIE_TITLE, MoviesEntry.COLUMN_POSTER};
    private String savedSpinnerPos;

    public MainActivityFragment() {

    }

    @Override
    public void onStart() {
        super.onStart();
        checkSpinnerAndFetch();
    }


    /*  This method checks the spinner position from prefs and performs two actions
     *  If the sortByValue is null, fetch movies with default spinner option.
     *  If sortByValue is favorites, fetch from DB, else make HTTPrequest for movies.
     */
    public void checkSpinnerAndFetch(){
        savedSpinnerPos = prefs.getString(spinPosKey, "");
        if(savedSpinnerPos!=null && !savedSpinnerPos.isEmpty()) {
            sortByValue = returnSpinnerText(Integer.parseInt(savedSpinnerPos));
        }

        //Check prefs if the sort is set. If null, fetch movies.
        if (sortByValue != null && sortByValue.equals(favSortString)) {
            if (favLoader == null) {
                viewFavorites();
            } else {
                getLoaderManager().restartLoader(0, null, this);
            }
        } else if (gridViewObjects.isEmpty()) {
            fetchMovies();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        gridView = (GridView) view.findViewById(R.id.fragment_main_gridView);
        favSortString = getString(R.string.favorites);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        nextPageCalling = false;
        Configuration config = getResources().getConfiguration();

        if (savedInstanceState != null) {
            gridViewObjects = (List<GridViewObject>) savedInstanceState.get(movieKey);
            position = savedInstanceState.getInt("position");
            orientationChanged = true;
        } else {
            rogueCounter = 1;
            gridViewObjects = new ArrayList<>();
        }

        favCursorAdapter = new CustomFavViewAdapter(getActivity(), null, 0);
        movieGridAdapter = new GridViewAdapter(getActivity(), R.layout.grid_item_layout, gridViewObjects);
        gridView.setAdapter(movieGridAdapter);

        // Set grid view columns for tablets.
        if (getResources().getBoolean(R.bool.isTablet)) {
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                gridView.setNumColumns(getResources().getInteger(R.integer.numCols_land));
            } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                gridView.setNumColumns(getResources().getInteger(R.integer.numCols_port));
            }
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                rogueCounter = 2;
                String movieId = movieIds.get(position);
                ((Callback) getActivity())
                        .onItemSelected(movieId);
            }
        });

        gridView.setSelection(position);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                // If reached the end of page, fetch the next page from the request. This should happen only if the sort option is *not* favorites
                if ((firstVisibleItem + visibleItemCount >= totalItemCount)
                        && nextPageCalling
                        && !sortByValue.equals(favSortString)) {
                    nextPageCalling = false;
                    pagenum++;
                    fetchMovies();
                }
            }
        });
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        // Initialize and display progress dialog.
        if (Utilities.isNetworkAvailable(getActivity())) {
            progDialog = new ProgressDialog(getActivity());
            progDialog.setMessage(getString(R.string.progress_text_main));
            progDialog.setIndeterminate(true);
            progDialog.show();
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commonToast != null)
            commonToast.cancel();
    }

    public interface Callback {
        void onItemSelected(String id);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        rogueCounter = 0;
        outState.putParcelableArrayList(movieKey, (ArrayList) gridViewObjects);
        outState.putInt("position", gridView.getFirstVisiblePosition());
    }

    public String returnSpinnerText(int spinnerPos) {

        switch (spinnerPos) {
            case 0:
                return getString(R.string.now_showing_sort);
            case 1:
                return getString(R.string.popularity_sort);
            case 2:
                return getString(R.string.earnings_sort);
            case 3:
                return getString(R.string.favorites);
            default:
                return null;
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
        String savedSpinnerPos = prefs.getString(spinPosKey, "");
        final Spinner sortingSpinner = (Spinner) menu.findItem(R.id.sort_spinner).getActionView();
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getActivity().getApplication(), R.array.sort_option, android.R.layout.simple_spinner_dropdown_item);
        sortingSpinner.setAdapter(spinnerAdapter);
        if (savedSpinnerPos != null && !savedSpinnerPos.isEmpty()) {
            sortingSpinner.setSelection(Integer.parseInt(savedSpinnerPos));
        }
        sortingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (rogueCounter <= 0 && !orientationChanged) {
                    SharedPreferences.Editor prefEditor = prefs.edit();
                    prefEditor.putString(spinPosKey, String.valueOf(position)).apply();
                    if (commonToast != null) {
                        commonToast.cancel();
                    }
                    gridView.setAdapter(movieGridAdapter);
                    sortByValue = returnSpinnerText(position);

                    // Depending on the sort criteria, set appropriate adapter for the view. If favorites, set favCursorAdapter, else set movieListAdapter
                    prefEditor.putString(spinStringKey, sortByValue).apply();
                    if (sortByValue.equals(favSortString)) {
                        viewFavorites();
                    }
                    
                    // Not-so-elegant hack : OnItemSelected is triggered erroneously :
                    // Once when orientation changes
                    // Once when spinner position is 0 AND a grid item is selected.
                    // Twice when spinner position is not zero and item is selected.
                    // rogueCounter is used to count the number of erroneous calls and pass through when the counter is 0.
                    // The movies must be fetched ONLY when the user makes a selection.
                    // Need a replacement for spinners
                    gridViewObjects = new ArrayList<>();
                    if (!sortByValue.equals(favSortString)) {
                        pagenum = 0;
                        movieIds = new ArrayList<>();
                        fetchMovies();
                    } else {
                        progDialog.dismiss();
                    }
                } else {
                    orientationChanged = false;
                    rogueCounter--;
                    if (position == 0)
                        rogueCounter = 0;
                    progDialog.dismiss();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void viewFavorites() {
        gridView.setAdapter(favCursorAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    /* As of now, only favorites is being persisted in DB.
     * favCursorAdapter is set to display favorites only.
     */
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        favLoader = new CursorLoader(getActivity(),
                FavoriteEntry.CONTENT_URI,
                cursorSelection,
                null,
                null,
                null);

        return favLoader;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        movieIds = new ArrayList<>();
        favCursorAdapter.swapCursor(data);
        if (data.moveToFirst()) {
            do {
                movieIds.add(data.getString(data.getColumnIndex(MoviesEntry.COLUMN_MOVIE_ID)));
            } while (data.moveToNext());
            progDialog.dismiss();
        } else {
            displayToastMessage(getString(R.string.no_fav_text));
        }
    }


    /* This method disables any progress dialogue and displays the toast.
    */

    public void displayToastMessage(String message) {
        if (progDialog != null && progDialog.isShowing()) {
            progDialog.dismiss();
        }
        commonToast = Toast.makeText(getActivity(),
                message,
                Toast.LENGTH_SHORT);
        commonToast.setGravity(Gravity.CENTER, 0, 0);
        commonToast.show();
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    public void fetchMovies() {
        FetchMoviesTask fetchMoviesTask = new FetchMoviesTask();
        if (!Utilities.isNetworkAvailable(getActivity())) {
            displayToastMessage(getString(R.string.no_connect_string));
        } else
            fetchMoviesTask.execute();
    }

    public class FetchMoviesTask extends AsyncTask<Void, Void, List<GridViewObject>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<GridViewObject> doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            int connectionTimeout = 3000;
            String apiKey = getString(R.string.api_key);
            String moviesJsonString;
            String LOG_TAG = FetchMoviesTask.class.getSimpleName();
            String MOVIES_BASE_URI = getString(R.string.tmdb_base_uri);
            String posterSize = getString(R.string.grid_poster_size);
            String API_REQ_STRING = "api_key";
            String SORT_BY_REQ = "sort_by";
            String PAGE_NUM = "page";


            try {
                if (sortByValue == null || sortByValue.isEmpty()) {
                    sortByValue = getString(R.string.now_showing_sort);
                }

                // URL example :  new URL("http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key=[YOUR API KEY]");
                Uri builtUri = Uri.parse(MOVIES_BASE_URI).buildUpon().build();

                if (sortByValue.equals(getString(R.string.now_showing_sort))) {
                    builtUri = builtUri.buildUpon()
                            .appendPath(getString(R.string.movie_tag))
                            .appendPath(getString(R.string.now_showing_sort))
                            .build();
                } else if (sortByValue.equals(getString(R.string.favorites))) {
                    return null;
                } else {
                    builtUri = builtUri.buildUpon()
                            .appendPath(getString(R.string.discover_tag))
                            .appendPath(getString(R.string.movie_tag))
                            .appendQueryParameter(SORT_BY_REQ, sortByValue).build();
                }

                pagenum = pagenum <= 1 ? 1 : pagenum;
                builtUri = builtUri.buildUpon().appendQueryParameter(PAGE_NUM, String.valueOf(pagenum)).appendQueryParameter(API_REQ_STRING, apiKey).build();

                try {
                    URL url = new URL(builtUri.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(connectionTimeout); // in milliseconds
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                } catch (ConnectException e) {
                    Log.e(LOG_TAG, "Error", e);
                }

                InputStream streamReader = urlConnection.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();

                if (streamReader == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(streamReader));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                if (stringBuilder.length() == 0) {
                    return null;
                }
                moviesJsonString = stringBuilder.toString();
                try {
                    gridViewObjects.addAll(getMoviesFromJson(moviesJsonString, posterSize));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "error", e);
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream");
                        return null;
                    }
                }
            }
            return gridViewObjects;
        }

        private List<GridViewObject> getMoviesFromJson(String moviesJsonString, String posterSize)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String POSTER_TAG = "poster_path";
            final String RESULTS_TAG = "results";
            final String BASE_POSTER_PATH = getString(R.string.poster_base_path);
            final String IDS_TAG = "id";
            final String TITLE_TAG = "title";
            List<GridViewObject> movieGridObjects;
            JSONObject moviesJSON = new JSONObject(moviesJsonString);
            // This check is for scrolling multiple pages. The previous movies need to be retained when fetching the next page of movies.
            if (movieIds == null)
                movieIds = new ArrayList<>();

            movieGridObjects = new ArrayList<>();
            JSONArray resultsArray = moviesJSON.getJSONArray(RESULTS_TAG);
            for (int i = 0; i < resultsArray.length(); i++) {
                GridViewObject gridViewObject = new GridViewObject();
                JSONObject resultObject = resultsArray.getJSONObject(i);
                gridViewObject.setMovieUrl(BASE_POSTER_PATH + posterSize + resultObject.getString(POSTER_TAG));
                gridViewObject.setMovieTag(resultObject.getString(TITLE_TAG));
                movieGridObjects.add(gridViewObject);
                movieIds.add(resultObject.getString(IDS_TAG));
            }
            return movieGridObjects;
        }

        @Override
        protected void onPostExecute(List<GridViewObject> res) {
            if (res != null) {
                List<GridViewObject> result = new ArrayList<>(res);
                super.onPostExecute(res);
                movieGridAdapter.clear();
                movieGridAdapter.addAll(result);
                movieGridAdapter.notifyDataSetChanged();
                progDialog.dismiss();
            }
            nextPageCalling = true;
        }
    }
}