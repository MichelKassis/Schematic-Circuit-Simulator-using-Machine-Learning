package ca.mcgill.schematicreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.OCR.OCR;
import ca.mcgill.schematicreader.bitmap.BitmapConverter;
import ca.mcgill.schematicreader.bitmap.BitmapScaler;
import ca.mcgill.schematicreader.interfaces.JNIImageProcessor;
import ca.mcgill.schematicreader.model.Box;
import ca.mcgill.schematicreader.model.ProcessingResult;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;
import ca.mcgill.schematicreader.model.electriccircuit.Label;
import ca.mcgill.schematicreader.neuralnetwork.ImageClassifier;
import ca.mcgill.schematicreader.utility.ImageStorage;

import static ca.mcgill.schematicreader.ConfirmPhotoActivity.PROCESSING_RESULT;
import static ca.mcgill.schematicreader.ConfirmPhotoActivity.SCALED_PHOTO;
import static ca.mcgill.schematicreader.HomeActivity.CROPPED_PHOTO;

public class ProcessingActivity extends AppCompatActivity {
    public static final String ELEMENT_LIST = "ca.mcgill.schematicreader.ELEMENT_LIST";

    private static final int DELAY_BEFORE_DISPATCH = 2000;
    private final static int IMAGE_PROCESSOR_HEIGHT = 500;
    private final static int IMAGE_PROCESSOR_BYTES_PER_PIXEL = 4;

    private CheckBox mSegmentationCheckbox;
    private CheckBox mClassificationCheckBox;
    private ProgressBar mProgressBar;

    private ImageStorage imageStorage;

    private ProcessingResult mProcessingResult;
    private Bitmap mBitmap;
    private ImageClassifier mImageClassifier;
    private OCR mOCR;
    private String mImagePath;
    private String mScaledImagePath;

    private ArrayList<CircuitElement> mCircuitElements;
    private ArrayList<Label> mLabels;

    private Bitmap mScaledBitmap;
    private Bitmap mBwBitmap;
    private byte[] mBitmapAsBytes;
    private byte[] mBwBitmapAsBytes;

    private float mBwThreshold = -1f;

    private JNIImageProcessor ip;

    public static void assignLabels(ArrayList<CircuitElement> components, ArrayList<Label> labels) {
        int num_components = components.size();
        int num_labels = labels.size();


        // Let only handle values for now.
        for (int i = 0; i < num_labels; i++) {
            if (labels.get(i).type != Label.LabelType.VALUE) {
                labels.remove(labels.get(i--));
                num_labels--;
                continue;
            }
        }

        // Create preference list for labels and components based on distance

        int[][] label_assignment_priorities = new int[num_labels][num_components]; // List the order of preference of each label.
        for (int i = 0; i < num_labels; i++) {
            double[] distances = new double[num_components];
            int[] sorted_distance_indices = new int[num_components];

            for (int j = 0; j < num_components; j++) {
                distances[j] = Box.getMinDistance2(components.get(j).getBox(), labels.get(i).box);
                sorted_distance_indices[j] = j;
            }

            // Sort distances
            for (int n = 0; n < num_components; n++) {
                for (int m = 0; m < num_components; m++) {
                    if (n == m) continue;
                    if (distances[n] < distances[m]) {
                        double temp = distances[n];
                        distances[n] = distances[m];
                        distances[m] = temp;

                        int temp2 = sorted_distance_indices[n];
                        sorted_distance_indices[n] = sorted_distance_indices[m];
                        sorted_distance_indices[m] = temp2;
                    }
                }
            }

            label_assignment_priorities[i] = sorted_distance_indices;
        }

        int[][] component_assignment_priorities = new int[num_components][num_labels]; // List the order of preference of each component.

        for (int i = 0; i < num_components; i++) {
            double[] distances = new double[num_labels];
            int[] sorted_distance_indices = new int[num_labels];

            for (int j = 0; j < num_labels; j++) {
                distances[j] = Box.getMinDistance2(components.get(i).getBox(), labels.get(j).box);
                sorted_distance_indices[j] = j;
            }

            // Sort distances
            for (int n = 0; n < num_labels; n++) {
                for (int m = 0; m < num_labels; m++) {
                    if (n == m) continue;
                    if (distances[n] < distances[m]) {
                        double temp = distances[n];
                        distances[n] = distances[m];
                        distances[m] = temp;

                        int temp2 = sorted_distance_indices[n];
                        sorted_distance_indices[n] = sorted_distance_indices[m];
                        sorted_distance_indices[m] = temp2;
                    }
                }
            }

            component_assignment_priorities[i] = sorted_distance_indices;
        }

        // Do the matching

        int[] label_matches = new int[num_labels];
        int[] component_matches = new int[num_components];

        // Initialize matches
        for (int i = 0; i < num_labels; i++) {
            label_matches[i] = -1;
        }

        for (int i = 0; i < num_components; i++) {
            component_matches[i] = -1;
        }

        for (int round = 0; round < num_components; round++) {
            ArrayList<Integer> yielded_labels = new ArrayList<>();
            for (int i = 0; i < num_labels; i++) {
                yielded_labels.add(i);
            }

            boolean got_a_match = true;
            while (!yielded_labels.isEmpty() && got_a_match) {
                got_a_match = false;
                int i = yielded_labels.get(0);
                yielded_labels.remove(0);
                if (label_matches[i] == -1) {
                    int attempt = label_assignment_priorities[i][round];

                    boolean can_match = false;
                    for (int j = 0; j < num_labels; j++) {
                        if (component_assignment_priorities[attempt][j] == i) {
                            can_match = true;
                            break;
                        }
                        if (label_matches[component_assignment_priorities[attempt][j]] != -1) {
                            if (component_matches[attempt] == component_assignment_priorities[attempt][j]) {
                                int currently_matched_index = -1;
                                for (int k = 0; k < num_components; k++) {
                                    if (label_assignment_priorities[component_matches[attempt]][k] == attempt) {
                                        currently_matched_index = k;
                                        break;
                                    }
                                }
                                if (Box.getMinDistance2(labels.get(i).box, components.get(label_assignment_priorities[i][round + 1]).getBox())
                                        > Box.getMinDistance2(labels.get(component_matches[attempt]).box, components.get(label_assignment_priorities[component_matches[attempt]][currently_matched_index + 1]).getBox())) {
                                    label_matches[i] = attempt;
                                    label_matches[component_matches[attempt]] = -1;
                                    component_matches[attempt] = i;
                                    got_a_match = true;
                                }
                                can_match = false;
                                break;
                            }
                        } else {
                            yielded_labels.add(i);
                            break;
                        }
                    }

                    if (can_match) {
                        label_matches[i] = attempt;
                        component_matches[attempt] = i;
                        got_a_match = true;
                    }
                }
            }
        }

        for (int i = 0; i < num_components; i++) {
            if (component_matches[i] != -1) {
                components.get(i).setValue(Double.parseDouble(labels.get(component_matches[i]).string.replaceAll("[^\\d.]", "")));
                System.out.println(components.get(i).getElementType().toString() + " has value " + components.get(i).getValue());
            }
        }
    }

    private void processPhoto() {
        int widthInBytes = mBwBitmap.getWidth() * IMAGE_PROCESSOR_BYTES_PER_PIXEL;
        mProcessingResult = ip.process(
                mBwBitmap.getWidth(),
                mBwBitmap.getHeight(),
                IMAGE_PROCESSOR_BYTES_PER_PIXEL,
                mBwBitmapAsBytes,
                widthInBytes,
                mBwBitmap.getByteCount()
        );
    }

    private void convertImageToBw() {
        int widthInBytes = mScaledBitmap.getWidth() * IMAGE_PROCESSOR_BYTES_PER_PIXEL;
        mBwBitmapAsBytes = ip.doBwConversion(
                mScaledBitmap.getWidth(),
                mScaledBitmap.getHeight(),
                IMAGE_PROCESSOR_BYTES_PER_PIXEL,
                mBitmapAsBytes,
                widthInBytes,
                mScaledBitmap.getByteCount(),
                mBwThreshold
        );
        mBwBitmap = Bitmap.createBitmap(mScaledBitmap.getWidth(), mScaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mBwBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(mBwBitmapAsBytes));
    }

    private void guessBwValue() {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap photoBitmap = BitmapFactory.decodeFile(mImagePath, decodeOptions);
        mScaledBitmap = BitmapScaler.scaleBitmap(photoBitmap, IMAGE_PROCESSOR_HEIGHT);

        try {
            File scaledImageFile = imageStorage.createScaledImageFile();
            BitmapConverter.writeBitmapToFile(scaledImageFile, mScaledBitmap);
            mScaledImagePath = scaledImageFile.getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBitmapAsBytes = BitmapConverter.convertBitmapToByteArray(mScaledBitmap);
        int widthInBytes = mScaledBitmap.getWidth() * IMAGE_PROCESSOR_BYTES_PER_PIXEL;

        mBwThreshold = ip.guessBwThreshold(
                mScaledBitmap.getWidth(),
                mScaledBitmap.getHeight(),
                IMAGE_PROCESSOR_BYTES_PER_PIXEL,
                mBitmapAsBytes,
                widthInBytes,
                mScaledBitmap.getByteCount()
        );
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        mSegmentationCheckbox = findViewById(R.id.segmentation_checkbox);
        mClassificationCheckBox = findViewById(R.id.classification_checkbox);

        imageStorage = new ImageStorage(getExternalFilesDir(Environment.DIRECTORY_PICTURES));

        ip = new JNIImageProcessor();

        mProgressBar = findViewById(R.id.progressBar);

        mCircuitElements = new ArrayList<>();
        mLabels = new ArrayList<>();

        try {
            mImageClassifier = new ImageClassifier(getApplicationContext());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Could not construct image classifier.", Toast.LENGTH_LONG).show();
            finish();
        }

        Intent intent = getIntent();

        /*
         * The CROPPED_PHOTO key is only set if the user is coming here straight from the Home
         * activity. Otherwise, debug mode is on and the user is coming here from the
         * Confirm Photo activity which is only used for debugging.
         */
        if (intent.hasExtra(CROPPED_PHOTO)) {
            mImagePath = Uri.parse(intent.getStringExtra(CROPPED_PHOTO)).getPath();
            guessBwValue();

            new Thread(() -> {
                mOCR = new OCR(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getApplicationContext());
                convertImageToBw();
                processPhoto();
                runOnUiThread(() -> {
                    mSegmentationCheckbox.setChecked(true);
                    mClassificationCheckBox.setVisibility(View.VISIBLE);
                });
                mBitmap = BitmapFactory.decodeFile(mScaledImagePath);
                classify();
                parseConnections();
                assignLabels(mCircuitElements, mLabels);
                runOnUiThread(this::endProcessing);
            }).start();
        } else {
            new Thread(() -> {
                mOCR = new OCR(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getApplicationContext());
                runOnUiThread(() -> {
                    mSegmentationCheckbox.setChecked(true);
                    mClassificationCheckBox.setVisibility(View.VISIBLE);
                });
                mProcessingResult = intent.getParcelableExtra(ConfirmPhotoActivity.PROCESSING_RESULT);
                mScaledImagePath = intent.getStringExtra(ConfirmPhotoActivity.SCALED_PHOTO);
                mBitmap = BitmapFactory.decodeFile(mScaledImagePath);
                classify();
                parseConnections();
                assignLabels(mCircuitElements, mLabels);
                runOnUiThread(this::endProcessing);
            }).start();
        }
    }

    public static void checkPrefix(Label label) {
        try {
            if (label.string.contains("G")) {
                double x = (Double.parseDouble(label.string.split("G")[0]) * Math.pow(10.0, 9.0));
                label.string = Double.toString(x);
            } else if (label.string.contains("M")) {
                double x = (Double.parseDouble(label.string.split("M")[0]) * Math.pow(10.0, -6.0)); //// TEMPORARY
                label.string = Double.toString(x);
            } else if (label.string.contains("k")) {
                double x = (Double.parseDouble(label.string.split("k")[0]) * Math.pow(10.0, 3.0));
                label.string = Double.toString(x);
            } else if (label.string.contains("c")) {
                double x = (Double.parseDouble(label.string.split("c")[0]) * Math.pow(10.0, -2.0));
                label.string = Double.toString(x);
            } else if (label.string.contains("m")) {
                double x = (Double.parseDouble(label.string.split("m")[0]) * Math.pow(10.0, -3.0));
                label.string = Double.toString(x);
            } else if (label.string.contains("u")) {
                double x = (Double.parseDouble(label.string.split("u")[0]) * Math.pow(10.0, -6.0));
                label.string = Double.toString(x);
            } else if (label.string.contains("n")) {
                double x = (Double.parseDouble(label.string.split("n")[0]) * Math.pow(10.0, -9.0));
                label.string = Double.toString(x);
            }
        }
        catch(Exception e){
            Log.e("Parsing Error:","Error while checking prefix");

        }

    }

    private void parseLabel(String str, Box box) {
        if (str.length() == 0) return;
        if (str.contains("=")) {
            String[] parts = str.split("=");
            if ((parts[0].charAt(0) >= 'a' && (parts[0].charAt(0) <= 'z')) ||
                    (parts[0].charAt(0) >= 'A' && parts[0].charAt(0) <= 'Z')) {

                Label l1 = new Label(Label.LabelType.NAME, parts[0], box);
                mLabels.add(l1);
                checkPrefix(l1);

                if (parts[1].matches(".*\\d.*")) {
                    Label l2 = new Label(Label.LabelType.VALUE, parts[1], box);
                    checkPrefix(l2);
                    mLabels.add(l2);
                }
            } else {
                Label l1 = new Label(Label.LabelType.VALUE, parts[0], box);
                Label l2 = new Label(Label.LabelType.NAME, parts[1], box);
                checkPrefix(l1);
                mLabels.add(l1);
                mLabels.add(l2);
            }
        } else if ((str.length() == 1) && (str.contains("+") || str.contains("-"))) {
            Label l1 = new Label(Label.LabelType.POLARITY, str, box);
            mLabels.add(l1);
        } else if ((str.charAt(0) >= 'a' && str.charAt(0) <= 'z') ||
                (str.charAt(0) >= 'A' && str.charAt(0) <= 'Z')) {
            Label l1 = new Label(Label.LabelType.NAME, str, box);
            mLabels.add(l1);
        } else if (str.matches(".*\\d.*")) {
            Label l1 = new Label(Label.LabelType.VALUE, str, box);
            checkPrefix(l1);
            mLabels.add(l1);
        } else {
            return;
        }
    }

    private void parseConnections() {

        if (mCircuitElements.size() == 1) {
            mCircuitElements.get(0).setNodeIn(0);
            mCircuitElements.get(0).setNodeOut(0);
            return;
        }

        int num_connections = 0;
        for (int i = 0; i < mProcessingResult.getConnections(); i++) {
            for (int j = 0; j < mProcessingResult.getConnections(); j++) {
                if (mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] > num_connections) {
                    num_connections = mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j];
                }
            }
        }

        for (int k = 0; k < mCircuitElements.size(); k++) {
            for (int i = 0; i < mProcessingResult.getConnections(); i++) {
                for (int j = 0; j < mProcessingResult.getConnections(); j++) {
                    if (i == j) continue;
                    if (i != k && j != k) continue;

                    if (mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] != 0) {
                        if (i > j) {
                            // Complicated math to offset by -ground node and make sure it's still within bounds
                            if (mCircuitElements.get(k).getNodeIn() > (mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] - mProcessingResult.getGroundNode() + num_connections) % num_connections) {
                                mCircuitElements.get(k).setNodeIn((mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] - mProcessingResult.getGroundNode() + num_connections) % num_connections);
                            }
                        }
                        if (i < j) {
                            if (mCircuitElements.get(k).getNodeOut() < (mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] - mProcessingResult.getGroundNode() + num_connections) % num_connections) {
                                mCircuitElements.get(k).setNodeOut(Math.max(mCircuitElements.get(k).getNodeOut(), (mProcessingResult.getConnectionMatrixWithId()[i * mProcessingResult.getConnections() + j] - mProcessingResult.getGroundNode() + num_connections) % num_connections));
                            }
                        }
                    }
                }
            }
        }

        // Second pass for sides
        for (int i = 0; i < mCircuitElements.size(); i++) {
            for (int j = 0; j < mProcessingResult.getConnections(); j++) {
                if (i == j) continue;

                if (mProcessingResult.getConnectionMatrixWithId()[j * mProcessingResult.getConnections() + i] != 0) {
                    if ((mCircuitElements.get(i).getNodeIn() == (mProcessingResult.getConnectionMatrixWithId()[j * mProcessingResult.getConnections() + i] - mProcessingResult.getGroundNode() + num_connections) % num_connections)) {
                        mCircuitElements.get(i).setSideIn(CircuitElement.Side.values()[(mProcessingResult.getConnectionMatrixWithSides()[j * mProcessingResult.getConnections() + i])]);
                    }
                    if ((mCircuitElements.get(i).getNodeOut() == (mProcessingResult.getConnectionMatrixWithId()[j * mProcessingResult.getConnections() + i] - mProcessingResult.getGroundNode() + num_connections) % num_connections)) {
                        mCircuitElements.get(i).setSideOut(CircuitElement.Side.values()[(mProcessingResult.getConnectionMatrixWithSides()[j * mProcessingResult.getConnections() + i])]);
                    }
                }
            }
        }
    }

    private void classify() {
        for (int i = 0; i < mProcessingResult.getConnections(); ++i) {
            Box box = mProcessingResult.getBoxArray()[i];
            int width = box.getRight() - box.getLeft();
            int height = box.getBottom() - box.getTop();
            Bitmap elementBitmap = Bitmap.createBitmap(mBitmap, box.getLeft(), box.getTop(), width, height);
            String elementType = mImageClassifier.classifyFrame(elementBitmap);
            mCircuitElements.add(CircuitElement.build(elementType, Integer.MAX_VALUE, 0, CircuitElement.Side.TOP, CircuitElement.Side.TOP, 1, box));
        }
        for (int i = mProcessingResult.getConnections(); i < mProcessingResult.getBoxArray().length; ++i) {
            Box box = mProcessingResult.getBoxArray()[i];
            int width = box.getRight() - box.getLeft();
            int height = box.getBottom() - box.getTop();
            Bitmap textBitmap = Bitmap.createBitmap(mBitmap, box.getLeft(), box.getTop(), width, height);
            String textRecog = mOCR.getText(textBitmap);
            Log.e("Hhhhhhhhhhhhhhhhhhhhh", textRecog);
            parseLabel(textRecog, box);
        }
    }

    private void endProcessing() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mClassificationCheckBox.setChecked(true);

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            dispatchResultViewIntent();
            finish();
        }, DELAY_BEFORE_DISPATCH);
    }

    private void dispatchResultViewIntent() {
        Intent resultViewIntent = new Intent(getApplicationContext(), ResultViewActivity.class);
        resultViewIntent.putExtra(SCALED_PHOTO, mScaledImagePath);
        resultViewIntent.putExtra(PROCESSING_RESULT, mProcessingResult);
        resultViewIntent.putParcelableArrayListExtra(ELEMENT_LIST, mCircuitElements);
        startActivity(resultViewIntent);
    }
}
