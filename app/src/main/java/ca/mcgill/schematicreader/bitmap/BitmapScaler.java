package ca.mcgill.schematicreader.bitmap;

import android.graphics.Bitmap;

public class BitmapScaler {
    public static Bitmap scaleBitmap(final Bitmap originalBitmap, final int goalHeight) {
        final int originalWidth = originalBitmap.getWidth();
        final int originalHeight = originalBitmap.getHeight();

        if (originalHeight < goalHeight) {
            return originalBitmap;
        }

        final int goalWidth = (int) ((double) originalWidth / ((double) originalHeight / (double) goalHeight));

        return Bitmap.createScaledBitmap(originalBitmap, goalWidth, goalHeight, false);
    }
}
