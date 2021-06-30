package ca.mcgill.schematicreader.interfaces;

import ca.mcgill.schematicreader.model.ProcessingResult;

public class JNIImageProcessor {

    public native String stringFromJNI();

    public native byte[] doBwConversion(
            int width,
            int height,
            int bytesPerPixel,
            byte[] bitmap,
            int widthInBytes,
            int numberOfBytes,
            float threshold);

    public native float guessBwThreshold(
            int width,
            int height,
            int bytesPerPixel,
            byte[] bitmap,
            int widthInBytes,
            int numberOfBytes);

    public native ProcessingResult process(
            int width,
            int height,
            int bytesPerPixel,
            byte[] bitmap,
            int widthInBytes,
            int numberOfBytes);

    static {
        System.loadLibrary("image-processor");
    }
}
