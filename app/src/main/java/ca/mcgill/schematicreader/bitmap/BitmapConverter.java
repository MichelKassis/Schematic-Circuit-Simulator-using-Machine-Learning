package ca.mcgill.schematicreader.bitmap;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BitmapConverter {
    public static byte[] convertBitmapToByteArray(final Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }

    public static void writeBitmapToFile(File destination, Bitmap bitmap) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(destination);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
