package tinytwit.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import tinytwit.activities.R;
import tinytwit.ui.SimpleToast;

/**
 * This class controls if the internet connection is available or not
 *
 * Created by Victor on 01/06/2014.
 */
public class NetworkReceiver extends BroadcastReceiver{
    private static final String TAG = "NetworkReceiver";

    private static final int DEFAULT_MESSAGE = R.string.error_connection;

    private Context context;
    private ConnectivityManager connMgr;
    private NetworkInfo networkInfo;

    private SimpleToast toaster;

    public NetworkReceiver(Context context) {
        this.context = context;
        toaster = new SimpleToast(context);
    }
    /**
     * Returns the network state
     *
     * @return true if it's connected false if not
     */
    public boolean checkSilentConnetion() {
        return checkConnection("");
    }

    /**
     * Returns the network state with the default toast message
     *
     * @return true if it's connected false if not
     */
    public boolean checkConnection() {
        return checkConnection(DEFAULT_MESSAGE);
    }

    /**
     * Returns the network state with the default message
     *
     * @param id
     *            id of the error message
     * @return true if it's connected false if not
     */
    boolean checkConnection(int id) {
        return checkConnection(context.getResources().getString(id));
    }

    /**
     * Returns the network state with the default message
     *
     * @param message
     *            message error.
     * @return
     */
    boolean checkConnection(String message) {
        // Obtain the connectivity manager
        connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        // Obtain the network state
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;

        } else {
            if (message != null && message.length() > 0) {
                toaster.toast(message);
            }
            return false;
        }
    }

    /**
     * Register the receiver at the activity
     */
    public void register() {
        IntentFilter filter = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, filter);
    }

    /**
     * Cancels the receiver at the activity
     */
    public void unregister() {
        try {
            context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // Added to avoid possible synchronization errors
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean status = checkSilentConnetion();

        // If the class implements UpdateableStatus, calls to
        // the method updateStatus when is changed.
        if (context instanceof UpdatableStatus) {
            ((UpdatableStatus) context).onUpdateStatus(status);
        }

    }
}
