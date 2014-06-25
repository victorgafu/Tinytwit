package tinytwit.utilities;

import java.io.File;

/**
 * Disk utilities class
 *
 * Created by Victor on 01/06/2014.
 */
public class DiskUtilities {
    private static final String TAG = "DiskUtilities";

    /**
     * Delete all the files from the directory
     *
     * @param folder
     *            path of the directory.
     */
    public static void clearDirectory(String folder) {
        clearDirectory(new File(folder));
    }
    /**
     * Delete all the files from the directory
     *
     * @param folder
     *            path of the directory.
     */
    public static void clearDirectory(File folder) {
        // If the directory doesn't exists
        if (!folder.exists()) {
            return;
        }

        // Delete all the files of the directory
        String[] children = folder.list();
        for (int i = 0; i < children.length; i++) {
            new File(folder, children[i]).delete();
        }
    }
}
