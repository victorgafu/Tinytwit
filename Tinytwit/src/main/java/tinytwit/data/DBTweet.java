package tinytwit.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import java.net.URL;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tinytwit.activities.R;

/**
 * Class of the database
 *
 * Created by Victor on 01/06/2014.
 */
public class DBTweet {
    public static final String TAG = "DBInterface";

    // Database and table
    public static final String DB_NAME = "BDTweets";
    public static final String DB_TABLE = "tweets";
    public static final int VERSION = 1;

    // Fields
    public static final String KEY_ID = "_id";
    public static final String TWEET_ID = "tweet_id";
    public static final String USER_ID = "user_id";
    public static final String NAME = "name";
    public static final String TEXT = "text";
    public static final String FAVORITES = "favorites";
    public static final String RETWEETS = "retweets";
    public static final String PUBLICATION_DATE = "date";
    public static final String URL = "url";
    public static final String PHOTO = "photo";
    public static final String MEDIA = "media";

    // Table creation
    public static final String BD_CREATE = "CREATE TABLE " + DB_TABLE + "("
            + KEY_ID + " INTEGER PRIMARY KEY, " + TWEET_ID + " INTEGER NOT NULL, " + USER_ID
            + " TEXT NOT NULL, " + NAME + " TEXT NOT NULL, " + TEXT
            + " TEXT NOT NULL, " + FAVORITES + " INTEGER NOT NULL, "
            + RETWEETS + " INTEGER NOT NULL, " + PUBLICATION_DATE
            + " TEXT NOT NULL, " + URL + " TEXT, " + PHOTO + " TEXT, "
            + MEDIA + " TEXT)";

    // Array of the fields for help with the consultation
    private String[] columns = new String[] { KEY_ID, TWEET_ID, USER_ID,
            NAME, TEXT, FAVORITES, RETWEETS,
            PUBLICATION_DATE, URL, PHOTO, MEDIA };

    private HelpDB help;
    private SQLiteDatabase bd;
    private Context context;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat();

    /**
     * The constructor creates an instance of HelpDB
     *
     * @param context
     */
    public DBTweet(Context context) {
        this.context = context;
        help = new HelpDB(context);
    }
    /**
     * Opens the database
     *
     * @return this object
     * @throws SQLException
     *             si hi ha un error al obrir la base de dades.
     */
    public DBTweet open() throws SQLException {
        bd = help.getWritableDatabase();
        return this;
    }
    /**
     * Closes the database
     */
    public void close() {
        try {
            help.close();
        } catch (SQLException e) {
            // If there's an error it will be showed at Log
            Log.e(TAG, context.getResources().getString(R.string.error_db));
        }
    }
    /**
     * Inserts a tweet whit the id of the argument
     *
     * @param id
     *            key of Tweet.
     * @param tweet
     *            tweet for insert.
     * @return this object.
     * @throws SQLException
     *             if there's an error on the tweet insert.
     */
    public DBTweet insertTweet(int id, Tweet tweet) throws SQLException {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ID, id);
        initialValues.put(TWEET_ID, tweet.id);
        initialValues.put(USER_ID, tweet.idUser);
        initialValues.put(NAME, tweet.name);
        initialValues.put(TEXT, tweet.text);
        initialValues.put(FAVORITES, tweet.favorites);
        initialValues.put(RETWEETS, tweet.retweets);
        initialValues.put(PHOTO, tweet.photo);
        initialValues.put(MEDIA, tweet.media);

        // Convert the date to string
        initialValues.put(PUBLICATION_DATE,
                dateFormat.format(tweet.publicationDate));

        // Convert the URL to string
        if (tweet.url != null) {
            initialValues.put(URL, tweet.url.toString());
        }

        // Insert the values
        bd.insert(DB_TABLE, null, initialValues);

        return this;
    }

    /**
     * Inserts the list of tweets and uses the index of the list as a primary key
     *
     * @param tweets
     *            Tweets list
     * @return this object.
     * @throws SQLException
     *             if there's an error on the tweet insert.
     */
    public DBTweet insertTweets(List<Tweet> tweets) throws SQLException {
        for (int i = 0, len = tweets.size(); i < len; i++) {
            insertTweet(i, tweets.get(i));
        }
        return this;
    }
    /**
     * Delete all the saved Tweets.
     *
     * @return this object.
     * @throws SQLException
     *             if there's an error deleting the tweets.
     */
    public DBTweet deleteTweets() throws SQLException {
        bd.delete(DB_TABLE, null, null);
        return this;
    }
    /**
     * Returns the tweet with the primary key
     *
     * @param id
     *            key of the tweet to obtain
     * @return the tweet with the primary key or null if is not found
     * @throws SQLException
     *             if there's an error obtaining the tweet.
     */
    public Tweet obtainTweet(long id) throws SQLException {
        Tweet tweet = null;

        Cursor cursor = bd.query(true, DB_TABLE, columns, TWEET_ID + " = " + id,
                null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            tweet = convertCursorToTweet(cursor);
        }

        return tweet;
    }
    /**
     * Returns a list with all the tweets at de database
     *
     * @return list of all the tweets found
     * @throws SQLException
     *             if there's an error obtaining the tweet.
     */
    public List<Tweet> obtainTweets() throws SQLException {
        List<Tweet> tweets = new ArrayList<Tweet>();
        Cursor mCursor = bd.query(DB_TABLE, columns, null, null, null, null,
                null);

        // We check the cursor and we add the tweets to the list.
        if (mCursor.moveToFirst()) {
            do {
                tweets.add(convertCursorToTweet(mCursor));
            } while (mCursor.moveToNext());
        }

        return tweets;
    }
    /**
     * Return a tweet with the information found at the actual position of the cursor.
     *
     * @param cursor
     *            cursor to obtain the tweet.
     * @return the generated tweet
     *
     */
    private Tweet convertCursorToTweet(Cursor cursor) {
        Tweet tweet = null;
        Date date = null;
        URL url = null;

        // Convert the date of the database to Date
        try {
            date = dateFormat.parse(cursor.getString(7));
        } catch (ParseException e) {
            Log.e(TAG,
                    context.getResources()
                            .getString(R.string.error_create_date),e);
        }

        // Convert the URL of the database to URL
        if (cursor.getString(8) != null) {
            try {
                url = new URL(cursor.getString(8));
            } catch (MalformedURLException e) {
                Log.e(TAG,
                        context.getResources().getString(
                                R.string.error_create_url),e);
            }
        }

        // We create a tweet with the cursor
        try {
            tweet = new Tweet(cursor.getLong(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), date, url, cursor.getString(9), cursor.getString(10));
        } catch (Exception e) {
            Log.e(TAG,
                    context.getResources().getString(
                            R.string.error_create_tweet),e);
        }

        return tweet;
    }

    /**
     * Class that helps to manage the database
     */
    private static class HelpDB extends SQLiteOpenHelper {
        HelpDB(Context con) {
            super(con, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(BD_CREATE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            Log.w(TAG, "Updating the database from the version "
                    + oldVersion + " to " + newVersion
                    + ". All the data will be destroyed");
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);

            onCreate(db);
        }
    }
}
