package tinytwit.activities;

/**
 * Created by Victor on 01/06/2014.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tinytwit.data.DBTweet;
import tinytwit.data.Tweet;
import tinytwit.utilities.DiskUtilities;
import tinytwit.utilities.DownloadImageAsyncTask;
import tinytwit.utilities.DownloadManager;
import tinytwit.utilities.NetworkReceiver;
import tinytwit.utilities.UpdatableStatus;
import tinytwit.ui.SimpleToast;
import tinytwit.ui.TweetArrayAdapter;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends Activity implements MenuItem.OnMenuItemClickListener,
        View.OnClickListener, AdapterView.OnItemClickListener, UpdatableStatus, DownloadManager {
    private static final String TAG = "tinytwit";

    public static final String PROFILES_FOLDER = "/profiles";
    public static final String AVATAR_FOLDER = "/avatar";
    public static final String MEDIA_FOLDER = "/media";

    private static int TWO_MINUTES = 2 * 60 * 1000;
    private static final int MAX_TWEETS_TO_SHOW = 50;

    // Twitter configuration
    static final String TWITTER_API_KEY = "0rWnrJIN9rrcicMANODAGg";
    static final String TWITTER_API_SECRET = "jvY5SSuKGHI4mL6TgnzrbfbAf8iGHwUvEKiLXGrL84M";
    static final String CALLBACK = "tinytwit:///";

    private ConfigurationBuilder cb;
    private TwitterFactory factory;
    private Twitter twitter;
    private RequestToken rqToken;
    private AccessToken accessToken;
    private String verifier;

    // Widgets
    private Button connectButton;
    private TextView textViewUsuari;
    private ImageView imageViewProfile;
    private ListView listViewFeeds;
    private ProgressBar progressBar;
    private ArrayAdapter<Tweet> adapter;

    private SimpleToast toaster;

    // ActionBar
    private MenuItem menuItemRefresh;
    private MenuItem menuItemPublish;

    // User
    private SharedPreferences prefs;
    private boolean authenticated = false;
    private String userPhoto;

    // Feed
    private List<Tweet> tweets;

    // Database
    private DBTweet db = new DBTweet(this);

    // Broadcast Receiver
    private NetworkReceiver receiver;

    // Task manager
    private boolean feed_updated;
    private boolean downloadingFeed;
    private int downloads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Code executed only on create
        initiateTwitterData();
        registerWidgets();

        // Define the SharedPreferences file
        prefs = getSharedPreferences("TinytwitsPreferences", MODE_PRIVATE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actionbar, menu);

        // Define the MenuItem
        menuItemRefresh = menu.findItem(R.id.action_refresh);
        menuItemRefresh.setOnMenuItemClickListener(this);

        menuItemPublish = menu.findItem(R.id.action_publish);
        menuItemPublish.setOnMenuItemClickListener(this);

        menuItemRefresh.setEnabled(authenticated);
        menuItemRefresh.setVisible(authenticated);

        menuItemPublish.setEnabled(authenticated);
        menuItemPublish.setVisible(authenticated);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Configures the initial objects for instance objects Twitter
     */
    private void initiateTwitterData() {
        cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(TWITTER_API_KEY);
        cb.setOAuthConsumerSecret(TWITTER_API_SECRET);
        Configuration conf = cb.build();
        factory = new TwitterFactory(conf);
    }

    /**
     * Registers the widgets of the activity and they listeners
     */
    private void registerWidgets() {
        connectButton = (Button) findViewById(R.id.buttonConnection);
        connectButton.setOnClickListener(this);

        textViewUsuari = (TextView) findViewById(R.id.textViewUser);
        textViewUsuari.setText("");

        imageViewProfile = (ImageView) findViewById(R.id.imageViewProfile);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        listViewFeeds = (ListView) findViewById(R.id.listViewFeed);
        listViewFeeds.setOnItemClickListener(this);

        toaster = new SimpleToast(this);
        receiver = new NetworkReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Code executed each time the application returns
        receiver.register();
        restoreSession();
        updateView();
        updateFeed();
    }

    /**
     * Update the status of the session with the saved information at the
     * preferences file
     *
     */
    private void restoreSession() {
        // Check if we have the authentication information saved.
        if (prefs.contains("OAUTH_TOKEN")
                && prefs.contains("OAUTH_TOKEN_SECRET")) {
            String token = prefs.getString("OAUTH_TOKEN", "");
            String secret = prefs.getString("OAUTH_TOKEN_SECRET", "");
            userPhoto = prefs.getString("PHOTO", "");
            accessToken = new AccessToken(token, secret);
            twitter = factory.getInstance(accessToken);
            connect(true);
        }
    }

    /**
     * Updates the status of the buttons and the user name according with the state of the
     * session
     */
    private void updateView() {
        // Check the  MenuItem
        if (menuItemRefresh != null) {
            menuItemRefresh.setEnabled(authenticated);
            menuItemRefresh.setVisible(authenticated);
            menuItemPublish.setEnabled(authenticated);
            menuItemPublish.setVisible(authenticated);
        }
        // Force the creation of the OptionsMenu
        invalidateOptionsMenu();
        textViewUsuari.setText(prefs.getString("SCREEN_NAME", ""));
    }

    @Override
    public void onPause() {
        super.onPause();

        // Code executed each time the activity is paused
        receiver.unregister();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();

        // If it's not null and starts with "tinytwit:///"
        if (uri != null && uri.toString().startsWith(CALLBACK)) {

            // Save the verifier (it's a String)
            verifier = uri.getQueryParameter("oauth_verifier");

            // AsyncTask for obtain the AccessToken
            new AccessTokenAsyncTask().execute();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem == menuItemRefresh) {
            // If we have connection, update feed
            if (!receiver.checkConnection()) {
                return true;
            }
            refreshLastUpdate(false);
            updateFeed();
            return true;

        } else if (menuItem == menuItemPublish) {
            // Start activity
            Intent intent = new Intent(MainActivity.this,
                    PublishActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == connectButton) {
            authenticate();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        // Start activity detail_activity_actionbar with the id
        Tweet t = adapter.getItem(position);
        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        intent.putExtra("id", t.id);
        startActivity(intent);
    }

    /**
     * Update the value of the last update in the preferences file to te current time or delete
     *
     * @param update
     *            true for update or false for delete
     */
    private void refreshLastUpdate(boolean update) {
        SharedPreferences.Editor editor = prefs.edit();
        if (update) {
            editor.putLong("LAST_UPDATE", System.currentTimeMillis());
        } else {
            editor.remove("LAST_UPDATE");
        }
        editor.apply();
    }
    /**
     * Starts the authentication if it's not, or logout if it was
     */
    private void authenticate() {
        if (!authenticated) {
            // If we don't have network the connection is impossible
            if (!receiver.checkConnection()) {
                return;
            }

            // Start the authentication
            twitter = factory.getInstance();
            new RequestTokenAsyncTask().execute();

        } else {
            // Disconnect
            connect(false);
        }
    }
    /**
     * Changes the status
     *
     * @param connect
     *            true for login or false for logout.
     */
    private void connect(boolean connect) {
        authenticated = connect;
        if (connect) {
            // Change the button to disconnect
            connectButton.setText(R.string.disconnect);
            loadAvatar(true);

        } else {
            // All the objects will be null
            twitter.setOAuthAccessToken(null);
            twitter.shutdown();

            // Change the button to connect
            connectButton.setText(R.string.connect);

            // Delete the preferences data
            savePreferences(false);

            // Clear the database
            db.open().deleteTweets().close();

            // Delete all the saves images
            DiskUtilities.clearDirectory(getFilesDir() + PROFILES_FOLDER);
            DiskUtilities.clearDirectory(getFilesDir() + MEDIA_FOLDER);
            DiskUtilities.clearDirectory(getFilesDir() + AVATAR_FOLDER);
        }

        updateView();
        updateFeed();
    }
    /**
     * Save or delete the session data at the preferences file.
     *
     * @param save
     *            true for save o false for delete.
     */
    private void savePreferences(boolean save) {
        SharedPreferences.Editor editor = prefs.edit();

        if (save) {
            // Save the preferences
            editor.putString("OAUTH_TOKEN", accessToken.getToken());
            editor.putString("OAUTH_TOKEN_SECRET", accessToken.getTokenSecret());
            if (accessToken.getScreenName() == null) {
                editor.putString("SCREEN_NAME",
                        prefs.getString("SCREEN_NAME", ""));
            } else {
                editor.putString("SCREEN_NAME",
                        "@" + accessToken.getScreenName());
            }
            editor.putString("PHOTO", userPhoto);
        } else {
            // Delete the data from preferences
            editor.remove("OAUTH_TOKEN");
            editor.remove("OAUTH_TOKEN_SECRET");
            editor.remove("SCREEN_NAME");
            editor.remove("LAST_UPDATE");
            editor.remove("PHOTO");
            loadAvatar(false);
        }
        editor.apply();
    }
    /**
     * Update the feed list. If the user isn't authenticated deletes the list,
     * otherwise the list will be updated with information from Internet if two
     * minutes has past since the last update.
     *
     * If the time is less than two minutes or the network is unavailable and the list
     * is empty, it will be filled with the database information
     *
     */
    private void updateFeed() {
        // Check if the feed is updating
        if (downloadingFeed) {
            return;
        }

        // Check if its authenticated, if not delete the feed and return
        if (!authenticated) {
            if (adapter != null) {
                adapter.clear();
            }
            tweets = null;
            return;
        }

        // Check if its necessary refresh the information or use the database.
        long lastUpdate = prefs.getLong("LAST_UPDATE", 0);
        long now = System.currentTimeMillis();

        if (lastUpdate + TWO_MINUTES < now
                && receiver.checkSilentConnetion()) {
            new LoadFeed().execute();

        } else if (adapter == null || adapter.isEmpty()) {
            // If it's not connected and the list is empty, load the feed from the database
            try {
                tweets = db.open().obtainTweets();
            } catch (SQLException e) {
                // On error the list will be empty.
                toaster.toast(R.string.error_db);
                tweets = new ArrayList<Tweet>();
            } finally {
                db.close();
            }
            fillFeed();
        }
    }
    /**
     * Fill the adaptor of the list with information of the tweets on memory
     */
    private void fillFeed() {
        // If there's nothing to show return
        if (tweets == null || tweets.isEmpty()) {
            return;
        }

        adapter = new TweetArrayAdapter(this, tweets);
        listViewFeeds.setAdapter(adapter);

        if (feed_updated) {
            toaster.toast(R.string.feed_updated);
            feed_updated = false;
        }
    }

    @Override
    public void onUpdateStatus(boolean connection) {
        // If the status of the connection changes update the feed
        if (connection) {
            updateFeed();
        }
    }
    /**
     * This method is called on start and finish the data download and images.
     * While the download is in process the progress bar will be visible and the
     * refresh button will be deactivated.
     *
     * @param download
     *            if true the download has started, false it's finished.
     */
    @Override
    public synchronized void downloadInProcess(boolean download) {
        // Update the counter
        if (download) {
            downloads++;
        } else {
            downloads--;
        }

        if (downloads > 0) {
            // There are downloads pending

            // Put the update button deactivated and the progress bar visible
            if (menuItemRefresh != null) {
                menuItemRefresh.setEnabled(false);
            }
            progressBar.setVisibility(View.VISIBLE);

        } else {
            // All the downloads are finished

            // fill the feed
            fillFeed();

            // Show the avatar
            loadAvatar(authenticated);

            // Activate the refresh button
            menuItemRefresh.setEnabled(true);

            // Hide the progress bar
            progressBar.setVisibility(View.GONE);
        }
    }
    /**
     * Shows or hides the avatar
     *
     * @param load
     *            if true the avatar will be visible if false will be hidden and the file deleted.
     */
    private void loadAvatar(boolean load) {
        if (load) {
            File file = new File(getFilesDir() + AVATAR_FOLDER, userPhoto);
            if (file.exists()) {
                Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
                imageViewProfile.setImageBitmap(image);
            }
        } else {
            DiskUtilities.clearDirectory(getFilesDir() + AVATAR_FOLDER);
            imageViewProfile.setImageBitmap(null);
        }
    }
    /**
     * This class creates a request for the RequestToken and opens a web browser
     * and ask for the  user and password. At the end the token will be saved at
     * rqToken
     */
    private class RequestTokenAsyncTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "RequestTokenAsyncTask";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                rqToken = twitter.getOAuthRequestToken(CALLBACK);
            } catch (TwitterException e) {
                Log.e(TAG, "Twitter Exception", e);
                Log.e(TAG,
                        getResources().getString(
                                R.string.error_twitter_exception));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rqToken
                    .getAuthenticationURL()));
            startActivity(intent);
        }
    }
    /**
     * This class uses the atributes rqtoken and verifier for obtain one AccessToken
     * for finish the authentication. This token will be saved after at accessToken
     * and the accesToken and RequestToken at the preferences file.
     */
    private class AccessTokenAsyncTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "AccessTokenAsyncTask";
        private String url;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                accessToken = twitter.getOAuthAccessToken(rqToken, verifier);
            } catch (TwitterException e) {
                Log.e(TAG, "Twitter Exception", e);
            }

            // Obtain the URL of the users avatar
            try {
                url = twitter.showUser(twitter.getId())
                        .getProfileImageURL();
            } catch (TwitterException e) {
                Log.e(TAG, "Twitter Exception", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Download the avatar
            new DownloadImageAsyncTask(MainActivity.this, getFilesDir()
                    + AVATAR_FOLDER).execute(url);
            userPhoto = Uri.parse(url).getLastPathSegment();

            // End of the authentication
            savePreferences(true);
            connect(true);
            progressBar.setVisibility(View.GONE);
        }
    }
    /**
     * Load the tweet feed, fills the list and save them at the database.
     */
    private class LoadFeed extends AsyncTask<Void, Void, List<Tweet>> {
        private static final String TAG = "CarregarFeed";

        // Used Set instead a list to avoid download duplicated URL
        Set<String> photos = new HashSet<String>();
        Set<String> medias = new HashSet<String>();

        @Override
        protected void onPreExecute() {
            downloadInProcess(true);
            downloadingFeed = true;
        }

        @Override
        protected List<Tweet> doInBackground(Void... params) {
            List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();
            List<Tweet> feed = new ArrayList<Tweet>();

            Paging paging = new Paging(1, MAX_TWEETS_TO_SHOW);

            try {
                statuses = twitter.getHomeTimeline(paging);
            } catch (TwitterException e) {
                Log.e(TAG, "Twitter Exception", e);
            }

            for (twitter4j.Status status : statuses) {
                feed.add(new Tweet(status));
                photos.add(status.getUser().getProfileImageURL());

                // If this status have other media it will be added to the list
                if (status.getMediaEntities().length > 0) {
                    medias.add(status.getMediaEntities()[0].getMediaURL());
                }
            }

            // Update the database
            try {
                db.open().deleteTweets().insertTweets(feed);
                refreshLastUpdate(true);
            } catch (SQLException e) {
                toaster.toast(R.string.error_db);
            } finally {
                db.close();
            }

            return feed;
        };

        @Override
        protected void onPostExecute(List<Tweet> feed) {
            // Delete the older profiles and download the news.
            String folder = getFilesDir() + PROFILES_FOLDER;
            String[] urlsPhotos = photos.toArray(new String[photos.size()]);
            DiskUtilities.clearDirectory(folder);
            new DownloadImageAsyncTask(MainActivity.this, folder)
                    .execute(urlsPhotos);

            // Delete the older media and download the news
            String[] urlsMedia = medias.toArray(new String[medias.size()]);
            folder = getFilesDir() + MEDIA_FOLDER;
            DiskUtilities.clearDirectory(folder);
            new DownloadImageAsyncTask(MainActivity.this, folder)
                    .execute(urlsMedia);

            tweets = feed;
            feed_updated = true;
            downloadInProcess(false);
            downloadingFeed = false;
        }
    }

}
