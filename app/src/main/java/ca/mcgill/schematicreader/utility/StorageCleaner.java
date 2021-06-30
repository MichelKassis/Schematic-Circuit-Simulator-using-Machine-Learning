package ca.mcgill.schematicreader.utility;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class StorageCleaner {

    private File storageDirectory;
    private File cacheDirectory;

    public StorageCleaner(File storageDirectory, File cacheDirectory) {
        this.storageDirectory = storageDirectory;
        this.cacheDirectory = cacheDirectory;
    }

    public void cleanStorage() throws IOException {
        cleanDirectory(storageDirectory);
    }

    public void cleanCache() throws IOException {
        deleteDir(cacheDirectory);
    }

    private void cleanDirectory(File directory) throws IOException {
        if (directory != null) {
            FileUtils.cleanDirectory(directory);
        }
    }

    // Note: This is a workaround because using FileUtils to clean the cache directory causes
    // a fatal exception and the application crashes. Seems to be an issue with too old API.
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
