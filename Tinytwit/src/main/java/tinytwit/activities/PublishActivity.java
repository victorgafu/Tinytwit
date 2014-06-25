package tinytwit.activities;


import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import tinytwit.data.DBTweet;
import tinytwit.data.Tweet;
import tinytwit.utilities.NetworkReceiver;
import tinytwit.ui.SimpleToast;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Activity that allows to publish tweets on internet.
 *
 * @author Victor Galvez
 *
 */

public class PublishActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "PublishActivity";

    // Twitter configuration
    private SharedPreferences prefs;
    private ConfigurationBuilder cb;
    private TwitterFactory factory;
    private Twitter twitter;
    private AccessToken accessToken;

    // Elements UI
    private TextView textViewUser;
    private EditText editTextTweet;
    private Button publishButton;
    private Button cancelButton;

    private SimpleToast toaster;
    private NetworkReceiver receiver;

    // Database
    private DBTweet db = new DBTweet(this);

    // Tweet
    private Tweet tweet;

    // Control
    private boolean isReply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        init();
    }
    /**
     * Initialize the objects, data and widgets.
     */
    private void init() {
        // Load preferences and obtain the name
        prefs = getSharedPreferences("TinytwitsPreferences", MODE_PRIVATE);
        toaster = new SimpleToast(this);
        receiver = new NetworkReceiver(this);
        registerWidgets();
        obtainTweet();
        dataInitTwitter();
    }
    /**
     * Registers the widgets.
     */
    private void registerWidgets() {
        editTextTweet = (EditText) findViewById(R.id.editTextPublish);

        publishButton = (Button) findViewById(R.id.buttonSend);
        publishButton.setOnClickListener(this);

        cancelButton = (Button) findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(this);

        textViewUser= (TextView) findViewById(R.id.textViewUser);
        textViewUser.setText(prefs.getString("SCREEN_NAME", ""));
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
     * Class that create the object tweet if it exists
     * @return true if exists false if not
     */
    private void obtainTweet() {
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            // Obtain the Tweet id
            long id = extras.getLong("id");
            // Obtain the correspondent tweet from the database
            try {
                db.open();
                tweet = db.obtainTweet(id);
                db.close();
                editTextTweet.setText("@"+tweet.idUser+" ");
                editTextTweet.setSelection(editTextTweet.getText().length());
                isReply = true;
            } catch (Exception e) {
                toaster.toast(R.string.error_db);
                isReply = false;
            }
        }else {
            isReply = false;
        }
    }

    @Override
    public void onClick(View view) {
        if (view == cancelButton) {
            // Returns to the main activity
            finish();
        } else if (view == publishButton) {
            // Try to send Tweet
            sendTweet();
        }
    }
    /**
     * Check if the message of EditText have the correct size and if it have
     * connection, if it's all ok try to publish.
     */
    public void sendTweet() {
        String text = editTextTweet.getText().toString();
        // Check if the message have between 1 and 150 characters.
        if (text.length() == 0 || text.length() > 140) {
            toaster.toast(R.string.error_size_tweet);
            return;
        } else if (!receiver.checkConnection()) {
            return;
        }
        publishTweet(text);
    }
    /**
     * Publish the argument message to twitter using the authentification data saved
     * at the preferences file.
     *
     * @param text
     *            text to send.
     */
    private void publishTweet(String text) {
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
        new PublishAsyncTask().execute(text);
    }
    /**
     * Task for publish a new Twitter message
     *
     */
    private class PublishAsyncTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean correct = false;
            String text = params[0];

            try {
                //If its a reply make a reply if not write the normal answer
               if(isReply){
                    StatusUpdate status = new StatusUpdate(text);
                    status.setInReplyToStatusId(tweet.id);
                    twitter.updateStatus(status);
                    correct = true;
               }else {
                    twitter.updateStatus(text);
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
                toaster.toast(R.string.tweet_sent);

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
}
