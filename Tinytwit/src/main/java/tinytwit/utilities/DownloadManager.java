package tinytwit.utilities;

/**
 * Created by Victor on 01/06/2014.
 */
public interface DownloadManager {

    /**
     * It's called when a download starts or ends
     *
     * @param download
     *            true on start and false at the end.
     * @see DownloadImageAsyncTask
     */

    void downloadInProcess(boolean download);
}
