package tinytwit.utilities;

/**
 * Created by Victor on 01/06/2014.
 */
public interface UpdatableStatus {

    /**
     * When the NetworkReceiver state changes calls to this method
     *
     * @param status true if it's connected,  false if it's disconnected
     */

    void onUpdateStatus(boolean status);
}
