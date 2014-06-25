package tinytwit.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import tinytwit.activities.MainActivity;
import tinytwit.activities.R;
import tinytwit.data.Tweet;

/**
 * Created by Victor on 01/06/2014.
 */
public class TweetArrayAdapter extends ArrayAdapter<Tweet>{
    private static final String TAG = "MobileArrayAdapter";
    private final Context context;
    private final List<Tweet> tweets;

    /**
     * Class constructor, requires a list of tweet objects and the application context
     *
     * @param context
     *            application to be implemented adapter.
     * @param tweets
     *            Tweet list to show.
     */
    public TweetArrayAdapter(Context context, List<Tweet> tweets) {
        super(context, R.layout.list_tweet, tweets);
        this.context = context;
        this.tweets = tweets;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Obtain the layout
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_tweet, parent, false);

        // Obtain the references to the widgets
        TextView textViewName = (TextView) rowView.findViewById(R.id.name);
        TextView textViewId = (TextView) rowView.findViewById(R.id.id);
        TextView textViewText = (TextView) rowView.findViewById(R.id.text);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.logo);

        // Apply the tweet values
        Tweet tweet = tweets.get(position);
        textViewName.setText(tweet.name);
        textViewId.setText("@" + tweet.idUser);
        textViewText.setText(tweet.text);
        imageView.setImageBitmap(carregarImatgeProfile(tweet));

        // Return the view of the row
        return rowView;
    }

    @Override
    public void clear() {
        // Clear the list
        tweets.clear();
        notifyDataSetChanged();
    }

    /**
     * Load the profile image of a tweet user
     *
     * @param tweet
     *            from who obtain the image
     * @return the image as a bitmap
     */
    private Bitmap carregarImatgeProfile(Tweet tweet) {
        String filename = tweet.photo;
        File file = new File(context.getFilesDir()
                + MainActivity.PROFILES_FOLDER, filename);
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        return image;
    }
}
