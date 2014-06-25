package tinytwit.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import tinytwit.data.Tweet;

/**
 * Created by Victor on 01/06/2014.
 */
public class DownloadImageAsyncTask extends AsyncTask<String, Void, Void> {
    public static final String TAG = "DownloadImageTask";
    public static final int TIMEOUT = 1000;

    private File folder;
    private DownloadManager manager;

    /**
     * This constructor accepts the folder as a String
     *
     * @param manager
     *            Activity that implements DownloadManager
     * @param folder
     *            destination folder
     * @see DownloadManager
     */
    public DownloadImageAsyncTask(DownloadManager manager, String folder) {
        this(manager, new File(folder));
    }
    /**
     * This constructor accepts the folder as a File
     *
     * @param manager
     *            Activity that implements DownloadManager
     * @param folder
     *            destination folder
     * @see DownloadManager
     */
    public DownloadImageAsyncTask(DownloadManager manager, File folder) {
        this.folder = folder;
        this.manager = manager;
        checkFolder();
    }
    /**
     * Check if the folder doesn't exists and if it doesn't exists creates the folder
     */
    private void checkFolder() {
        if (!folder.exists()) {
            folder.mkdir();
        }
    }
    @Override
    protected void onPreExecute() {
        manager.downloadInProcess(true);
    }

    @Override
    protected Void doInBackground(String... urls) {
        Bitmap image;
        File file;
        String filename = null;
        InputStream in;
        FileOutputStream out = null;

        // Browse the list of URL and download each one of the images
        for (String url : urls) {

            try {
                // Download the image, if it takes too much to connect it goes to the next one
                URLConnection con = new URL(url).openConnection();
                con.setConnectTimeout(TIMEOUT);
                in = con.getInputStream();

                image = BitmapFactory.decodeStream(in);

                // Save the image into a file
                filename = Tweet.obtainFileName(url);
                file = new File(folder, filename);
                out = new FileOutputStream(file);

                image.compress(CompressFormat.PNG, 100, out);
                out.flush();

            } catch (Exception e) {
                // Ignore any error.
            } finally {
                close(out);
            }
        }

        return null;
    }
    @Override
    protected void onPostExecute(Void result) {
        manager.downloadInProcess(false);
    }

    /**
     * Closes the stream
     *
     * @param out
     *            Stream to close.
     */
    private void close(FileOutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                // Ignore any error.
            }
        }
    }
}
