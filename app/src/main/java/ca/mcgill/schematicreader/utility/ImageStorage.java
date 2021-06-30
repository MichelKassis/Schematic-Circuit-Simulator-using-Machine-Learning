package ca.mcgill.schematicreader.utility;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageStorage {

    private static final String SCALED_SUBDIRECTORY = "scaled";
    private static final String BW_SUBDIRECTORY = "bw";
    private static final String IMAGE_FILE_PREFIX = "SchematicReader_";

    private File imageDirectoryBase;
    private File scaledImageDirectory;
    private File bwImageDirectory;

    private boolean mkdirFailed = false;

    public ImageStorage(File imageDirectory) {
        this.imageDirectoryBase = imageDirectory;

        this.scaledImageDirectory = getOrCreateDir(imageDirectoryBase.getAbsolutePath() +
                File.separator + SCALED_SUBDIRECTORY);

        this.bwImageDirectory = getOrCreateDir(imageDirectoryBase.getAbsolutePath() +
                File.separator + BW_SUBDIRECTORY);
    }

    public File createScaledImageFile() throws IOException {
        return File.createTempFile(
                getImageFileName(), /* prefix */ ".jpg", /* suffix */ scaledImageDirectory /* directory */);
    }

    public File createBwImageFile() throws IOException {
        return File.createTempFile(
                getImageFileName(), /* prefix */ ".jpg", /* suffix */ bwImageDirectory /* directory */);
    }

    public File createImageFile() throws IOException {
        return File.createTempFile(
                getImageFileName(), /* prefix */ ".jpg", /* suffix */ imageDirectoryBase /* directory */);
    }

    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return IMAGE_FILE_PREFIX + timeStamp + "_";
    }

    private File getOrCreateDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdir()) {
                mkdirFailed = true;
            }
        }
        return file;
    }
}
