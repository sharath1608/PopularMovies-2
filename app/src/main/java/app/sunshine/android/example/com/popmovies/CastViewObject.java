package app.sunshine.android.example.com.popmovies;


/**
 * Created by Asus1 on 8/31/2015.
 */
public class CastViewObject {


    String castImageUrl;
    String castName;
    String castId;

    public CastViewObject(String castName, String castImageUrl, String castId) {
        this.castImageUrl = castImageUrl;
        this.castName = castName;
        this.castId = castId;
    }

    public String getCastImageUrl() {
        return castImageUrl;
    }

    public void setCastImageUrl(String castImageUrl) {
        this.castImageUrl = castImageUrl;
    }

    public String getCastId() {
        return castId;
    }

    public void setCastId(String castId) {
        this.castId = castId;
    }

    public String getCastName() {
        return castName;
    }

    public void setCastName(String castName) {
        this.castName = castName;
    }

}
