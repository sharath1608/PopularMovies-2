package app.sunshine.android.example.com.popmovies;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Asus1 on 9/28/2015.
 */
public class Utilities {

    public static String snipTextForView(String overviewText){
        int start = 0;
        int end = 300;
        return overviewText.length()>end?(overviewText.substring(start,end) + " ..."):overviewText;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork!=null && activeNetwork.isConnectedOrConnecting());
    }
}
