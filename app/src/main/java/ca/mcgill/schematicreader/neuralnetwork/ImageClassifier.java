package ca.mcgill.schematicreader.neuralnetwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import ca.mcgill.schematicreader.utility.StreamUtil;

public class ImageClassifier {
    private static final String TAG = "ImageClassifier";
    private static final String MODEL_FILE = "optimized_graph.lite";
    private static final String LABEL_FILE = "retrained_labels.txt";

    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 3;

    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;

    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    private ByteBuffer imgData;

    private Interpreter tflite;
    private List<String> labelList;
    private float[][] labelProbArray;

    public ImageClassifier(Context context) throws IOException {
        File modelFile = StreamUtil.streamToFile(context.getAssets().open(MODEL_FILE));

        tflite = new Interpreter(modelFile);
        labelList = loadLabelList(context.getAssets().open(LABEL_FILE));

        labelProbArray = new float[1][labelList.size()];
        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
    }

    public List<String> getLabelList() {
        return labelList;
    }

    public String classifyFrame(Bitmap bitmap) {
        Bitmap scaledBitmap = scaleBitmapToSize(bitmap);
        convertBitmapToByteBuffer(scaledBitmap);

        long startTime = SystemClock.uptimeMillis();
        tflite.run(imgData, labelProbArray);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

        int argMax = 0;
        float max = 0;
        for (int i = 0; i < labelProbArray[0].length; i++) {
            if (labelProbArray[0][i] > max) {
                max = labelProbArray[0][i];
                argMax = i;
            }
        }

        return labelList.get(argMax);
    }

    private List<String> loadLabelList(InputStream inputStream) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private Bitmap scaleBitmapToSize(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, false);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
    }
}
