package tinytwit.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import tinytwit.data.DBTweet;
import tinytwit.data.Tweet;
import tinytwit.fragments.BrowserFragment;
import tinytwit.fragments.MediaFragment;
import tinytwit.ui.SimpleToast;
import tinytwit.utilities.NetworkReceiver;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * This class shows the detail_activity_actionbar of a saved tweet at the database
 *
 * @author Victor Galvez
 *
 */

public class DetailActivity extends FragmentActivity implements MenuItem.OnMenuItemClickListener {
    private static final String TAG = "TweetDetail";

    // Twitter configuration
    private SharedPreferences prefs;
    private ConfigurationBuilder cb;
    private TwitterFactory factory;
    private Twitter twitter;
    private AccessToken accessToken;

    // Fragments
    private Fragment webFragment;
    private Fragment mediaFragment;
    private FragmentManager fm;
    private FragmentTransaction ft;

    // Database
    private DBTweet db = new DBTweet(this);

    // UI
    private ImageView imageViewProfile;
    private TextView textViewName;
    private TextView textViewTweetText;
    private TextView textViewFavoriteCount;
    private TextView textViewRetweetCount;
    private TextView textViewPubDate;

    // ActionBar
    private MenuItem menuItemReply;
    private MenuItem menuItemRetweet;

    private SimpleToast toaster;
    private NetworkReceiver receiver;
    private Tweet tweet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        initWidgets();
        obtainTweet();
        loadTweet();
        loadProfileImage();
        dataInitTwitter();
        addFragment(additions());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detail_activity_actionbar, menu);

        // Define the MenuItem
        menuItemReply = menu.findItem(R.id.action_reply);
        menuItemReply.setOnMenuItemClickListener(this);
        menuItemRetweet = menu.findItem(R.id.action_retweet);
        menuItemRetweet.setOnMenuItemClickListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Obtain all the widget references
     */
    private void initWidgets() {
        prefs = getSharedPreferences("TinytwitsPreferences", MODE_PRIVATE);
        toaster = new SimpleToast(this);
        receiver = new NetworkReceiver(this);

        mediaFragment = new MediaFragment();
        webFragment = new BrowserFragment();
        imageViewProfile = (ImageView) findViewById(R.id.imageViewProfile);
        textViewName = (TextView) findViewById(R.id.textViewName);
        textViewTweetText = (TextView) findViewById(R.id.textViewTweetText);
        textViewFavoriteCount = (TextView) findViewById(R.id.textViewFavoriteCount);
        textViewRetweetCount = (TextView) findViewById(R.id.textViewRetweetCount);
        textViewPubDate = (TextView) findViewById(R.id.textViewPubDate);
    }
    /**
     * Obtain the correspondent tweet
     */
    private void obtainTweet() {
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            // Obtain the Tweet id
            long id = extras.getLong("id");

            // Obtain the correspondent tweet from the database
            try {
                db.open();
                tweet = db.obtainTweet(id);
                db.close();
            } catch (Exception e) {
                toaster.toast(R.string.error_db);
            }
        }
    }
    /**
     * Loads the data tweet information at the widgets of the activity
     */
    private void loadTweet() {
        // If the tweet is not found.
        if (tweet == null) {
            toaster.toast(R.string.tweet_not_found);
            return;
        }

        textViewName.setText(tweet.name);
        textViewTweetText.setText(tweet.text);
        Linkify.addLinks(textViewTweetText, Linkify.WEB_URLS);
        textViewFavoriteCount.setText(tweet.favorites + "");
        textViewRetweetCount.setText(tweet.retweets + "");
        textViewPubDate.setText(tweet.publicationDate.toString());
        setTitle("@" + tweet.idUser);
    }

    /**
     * Load the profile image
     */
    private void loadProfileImage() {
        // Check if the profile image exists
        if (tweet.photo == null || tweet.photo.length() == 0) {
            // If not returns
            return;
        }

        String filename = tweet.photo;
        File file = new File(getFilesDir() + MainActivity.PROFILES_FOLDER,
                filename);
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        imageViewProfile.setImageBitmap(image);
    }
    /**
     * Configures the initial objects for instance the Twitter objects.
     */
    private void dataInitTwitter() {
        cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(MainActivity.TWITTER_API_KEY);
        cb.setOAuthConsumerSecret(MainActivity.TWITTER_API_SECRET);
        Configuration conf = cb.build();
        factory = new TwitterFactory(conf);
    }

    /**
     * Creates the object Twitter loading the token from preferences
     *
     */
    private void sendTwitter(MenuItem item) {
        // Check if there's saved preferences
        if (prefs.contains("OAUTH_TOKEN")
                && prefs.contains("OAUTH_TOKEN_SECRET")) {
            // Load the tokens and obtain a new Twitter
            String token = prefs.getString("OAUTH_TOKEN", "");
            String secret = prefs.getString("OAUTH_TOKEN_SECRET", "");
            accessToken = new AccessToken(token, secret);
            twitter = factory.getInstance(accessToken);
        } else {
            toaster.toast(R.string.error_authentication);
            return;
        }
        // Call the publish task
        new PublishAsyncTask().execute(item);

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if(menuItem == menuItemReply) {
            //If we have connection return true
            if (!receiver.checkConnection()) {
                return true;
            }
            //Start activity
            Intent intent = new Intent(DetailActivity.this, PublishActivity.class);
            intent.putExtra("id", tweet.id);
            startActivity(intent);
            return true;
        }else if(menuItem == menuItemRetweet) {
            sendTwitter(menuItemRetweet);
            return true;
        }
        return false;
    }

    /**
     * Task for publish a new Twitter message
     *
     */
    private class PublishAsyncTask extends AsyncTask<MenuItem, Void, Boolean> {

        @Override
        protected Boolean doInBackground(MenuItem... params) {
            boolean correct = false;
            MenuItem item = params[0];

            try {
                //If its a retweet make a retweet
                if(item == menuItemRetweet){
                    twitter.retweetStatus(tweet.id);
                    correct = true;
                }
            } catch (TwitterException e) {
                Log.e(TAG, "Twitter Exception", e);
            }
            return correct;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // If it's sent show the message and returns to the main activity
            if (result) {
               toaster.toast(R.string.retweet);
                // Delete de date of the last update
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("LAST_UPDATE");
                editor.apply();
                // Returns to the main activity
                finish();

            } else {
                toaster.toast(R.string.error_publish);

            }
        }
    }
    /**
     * Class for assign the fragment
     */
    private void addFragment(int i){
        fm = getSupportFragmentManager();
        ft = fm.beginTransaction();
        if(i == 0){
            Bundle arguments = new Bundle();
            arguments.putString("media", tweet.media);
            mediaFragment.setArguments(arguments);
            ft.add(R.id.container, mediaFragment);
            ft.commit();
        }else if (i == 1){
            Bundle arguments = new Bundle();
            arguments.putString("url", tweet.url.toString());
            webFragment.setArguments(arguments);
            ft.add(R.id.container, webFragment);
            ft.commit();
        }else {
            ft.commit();
            fm = null;
            ft = null;
            return;
        }
    }

    /**
     * Class that checks if the tweet contains media or url
     * @return 0 url 1 media 2 nothing
     */
    private int additions(){
        if(tweet.media != null && tweet.media.length() > 0){
            return 0;
        }else if(tweet.url != null){
            return 1;
        }else{
            return 2;
        }
    }
}
