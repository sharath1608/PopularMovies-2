package app.sunshine.android.example.com.popmovies;

import android.content.Context;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;


/*
 * Credit :http://stackoverflow.com/users/1908296/ahmed-hamdy
 */
public class PicassoImageCache {

    // static picasso constructor
    private static Picasso picassoInstance = null;

    private PicassoImageCache (Context context) {

        Downloader downloader   = new OkHttpDownloader(context, Integer.MAX_VALUE);
        Picasso.Builder builder = new Picasso.Builder(context);
        builder.downloader(downloader);
        picassoInstance = builder.build();
    }

    // get picasso instance
    public static Picasso getPicassoInstance (Context context) {

        if (picassoInstance == null) {

            new PicassoImageCache(context);
            return picassoInstance;
        }
        return picassoInstance;
    }

}
