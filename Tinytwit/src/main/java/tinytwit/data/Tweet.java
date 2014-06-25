package tinytwit.data;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import twitter4j.Status;

/**
 * Created by Victor on 01/06/2014.
 */
public class Tweet {
    public static final String TAG = "Tweet";

    public final String idUser;
    public final String name;
    public final String text;
    public final int favorites;
    public final int retweets;
    public final Date publicationDate;
    public final URL url;
    public final String photo;
    public final String media;
    public final long id;

    /**
     * Build the Tweet with the parameters
     *
     * @param idUser
     *            identifier of the user on twitter
     * @param name
     *            full name of the user
     * @param text
     *            content of the Tweet
     * @param favorites
     *            number of favorites
     * @param retweets
     *            number of retweets
     * @param publicationDate
     *            publication date
     * @param url
     *            url in the tweet
     * @param photo
     *            name of the file with the profile picture
     * @param id
     *            id of the tweet
     *
     */
    public Tweet(Long id, String idUser, String name, String text, int favorites,
                 int retweets, Date publicationDate, URL url, String photo, String media) {
        this.id = id;
        this.idUser = idUser;
        this.name = name;
        this.text = text;
        this.favorites = favorites;
        this.retweets = retweets;
        this.publicationDate = publicationDate;
        this.url = url;
        this.photo = photo;
        this.media = media;
    }

    /**
     * Build the tweet with the object Status
     *
     * @param status
     *            Status that the information is extracted.
     */
    public Tweet(Status status) {
        this.id = status.getId();
        this.idUser = status.getUser().getScreenName();
        this.name = status.getUser().getName();
        this.favorites = status.getFavoriteCount();
        this.retweets = status.getRetweetCount();
        this.publicationDate = status.getCreatedAt();

        //We check if it's a tweet or a retweet
        if(status.isRetweet()){
            this.text = "RT "+status.getRetweetedStatus().getText();
        }else {
            this.text = status.getText();
        }

        // We check if there's any media
        String media="";
        if (status.getMediaEntities().length > 0) {
            try {
                media = obtainFileName(new URL(status.getMediaEntities()[0].getMediaURL()));
            } catch (MalformedURLException e) {
                // If the URL is malformed nothing will happens
            }
        }
        this.media = media;

        // If there's any url we try to recover it
        URL tempURL = null;
        if (status.getURLEntities().length > 0 ) {
            try {
                tempURL = new URL(status.getURLEntities()[0].getURL());
            } catch (MalformedURLException e) {
                // If the URL is malformed nothing will happens
            }
        }
        this.url = tempURL;

        // Profile
        String photo = "";
        try {
            photo = obtainFileName(new URL(status.getUser()
                    .getProfileImageURL()));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error obtaining the name of the picture", e);
        }
        this.photo = photo;

    }

    public static String obtainFileName(String url) {
        return Uri.parse(url).getLastPathSegment();
    }

    public static String obtainFileName(URL url) {
        return obtainFileName(url.getFile());
    }

    @Override
    public String toString() {
        return name + " - " + text;
    }
}
