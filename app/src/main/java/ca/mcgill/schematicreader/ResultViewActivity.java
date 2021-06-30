package ca.mcgill.schematicreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.bitmap.Drawing;
import ca.mcgill.schematicreader.model.Box;
import ca.mcgill.schematicreader.model.ProcessingResult;
import ca.mcgill.schematicreader.model.electriccircuit.Capacitor;
import ca.mcgill.schematicreader.model.electriccircuit.Circuit;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;
import ca.mcgill.schematicreader.model.electriccircuit.CurrentSource;
import ca.mcgill.schematicreader.model.electriccircuit.Inductor;
import ca.mcgill.schematicreader.model.electriccircuit.Resistor;
import ca.mcgill.schematicreader.model.electriccircuit.VoltageSource;
import ca.mcgill.schematicreader.utility.PaintFactory;
import ca.mcgill.schematicreader.utility.Translator;

import static ca.mcgill.schematicreader.ConfirmPhotoActivity.PROCESSING_RESULT;
import static ca.mcgill.schematicreader.ConfirmPhotoActivity.SCALED_PHOTO;
import static ca.mcgill.schematicreader.ProcessingActivity.ELEMENT_LIST;

public class ResultViewActivity extends AppCompatActivity {
    public static final int PAINT_COLOR = Color.CYAN;
    public static final int PAINT_COLOR_HIGHLIGHTED = Color.YELLOW;

    public static final String NETLIST_STRING = "NETLIST_STRING";

    private static final int RESISTOR_INDEX = 0;
    private static final int CAPACITOR_INDEX = 1;
    private static final int INDUCTOR_INDEX = 2;
    private static final int VOLTAGE_SOURCE_INDEX = 3;
    private static final int CURRENT_SOURCE_INDEX = 4;

    private static final int AC_ANALYSIS_INDEX = 0;
    private static final int DC_ANALYSIS_INDEX = 1;

    private int mImageWidth;
    private int mImageHeight;

    ProcessingResult mProcessingResult;
    File mImageFile;
    ImageView mResultView;
    Canvas mCanvas;
    Bitmap mDrawBitmap;

    ArrayList<CircuitElement> mElementList;
    Circuit mCircuit;

    CircuitElement mSelectedElement;

    private TextView mElementTypeTextView;
    private TextView mElementValueTextView;

    AlertDialog mElementTypeDialog;
    AlertDialog mElementValueDialog;
    AlertDialog mAnalysisDialog;

    private Drawing mDrawing;

    private Paint mHighlightPaint;
    private Paint mPaint;

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_view);

        mHighlightPaint = PaintFactory.createPaint(PAINT_COLOR_HIGHLIGHTED);
        mPaint = PaintFactory.createPaint(PAINT_COLOR);

        mDrawing = new Drawing();

        mElementTypeDialog = buildElementTypeDialog();
        findViewById(R.id.change_element_type_button).setOnClickListener((view) -> {
            if (mSelectedElement != null) {
                mElementTypeDialog.show();
            }
        });

        mElementValueDialog = buildElementValueDialog();
        findViewById(R.id.change_element_value_button).setOnClickListener((view) -> {
            if (mSelectedElement != null) {
                mElementValueDialog.show();
            }
        });

        findViewById(R.id.export_button).setOnClickListener((view) -> {
            String netlist = mCircuit.toNetlist();
            Intent intent = new Intent(this, CircuitExportActivity.class);
            intent.putExtra(NETLIST_STRING, netlist);
            startActivity(intent);
        });

        mAnalysisDialog = buildAnalysisDialog();
        findViewById(R.id.analysis_button).setOnClickListener((view) -> mAnalysisDialog.show());

        mElementTypeTextView = findViewById(R.id.element_type_value);
        mElementValueTextView = findViewById(R.id.element_value_value);

        Intent intent = getIntent();
        mProcessingResult = intent.getParcelableExtra(ConfirmPhotoActivity.PROCESSING_RESULT);
        mImageFile = new File(intent.getStringExtra(ConfirmPhotoActivity.SCALED_PHOTO));
        mElementList = intent.getParcelableArrayListExtra(ELEMENT_LIST);

        mCircuit = new Circuit(mElementList);

        mResultView = findViewById(R.id.result_view);

        Bitmap imageBitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath());

        mImageWidth = imageBitmap.getWidth();
        mImageHeight = imageBitmap.getHeight();

        mDrawBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mDrawBitmap);
        mCanvas.drawBitmap(imageBitmap, 0, 0, null);

        mProcessingResult.setBoxArray(Arrays.copyOfRange(mProcessingResult.getBoxArray(), 0, mProcessingResult.getConnections()));
        mDrawing.drawBoxes(mProcessingResult.getBoxArray(), mCanvas, mPaint);

        mResultView.setImageBitmap(mDrawBitmap);

        mResultView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Matrix inverse = new Matrix();
                mResultView.getImageMatrix().invert(inverse);
                float[] touchPoint = new float[]{event.getX(), event.getY()};
                inverse.mapPoints(touchPoint);
                handleTap(touchPoint[0], touchPoint[1]);
            }
            return false;
        });
    }

    private void handleTap(float tapX, float tapY) {
        if (tapX < 0 || tapX > mImageWidth || tapY < 0 || tapY > mImageHeight) {
            selectElement(null);
            return;
        }

        CircuitElement tappedElement = findBox(tapX, tapY);
        if (tappedElement == null) {
            selectElement(null);
            return;
        }

        selectElement(tappedElement);
    }

    private void selectElement(CircuitElement circuitElement) {
        if (circuitElement == null) {
            if (mSelectedElement != null) {
                mDrawing.drawBox(mSelectedElement.getBox(), mCanvas, mPaint);
                mResultView.setImageBitmap(mDrawBitmap);
                mElementTypeTextView.setText(null);
                mElementValueTextView.setText(null);
                mSelectedElement = null;
            }
            return;
        }
        if (mSelectedElement != null) {
            mDrawing.drawBox(mSelectedElement.getBox(), mCanvas, mPaint);
        }

        mDrawing.drawBox(circuitElement.getBox(), mCanvas, mHighlightPaint);
        mElementTypeTextView.setText(Translator.translateElementType(circuitElement.getElementType()));
        mElementValueTextView.setText(circuitElement.getValueString());
        mResultView.setImageBitmap(mDrawBitmap);
        mSelectedElement = circuitElement;
    }

    private CircuitElement findBox(float x, float y) {
        for (CircuitElement circuitElement : mElementList) {
            Box box = circuitElement.getBox();
            if ((int) x >= box.getLeft() &&
                    (int) x <= box.getRight() &&
                    (int) y >= box.getTop() &&
                    (int) y <= box.getBottom()) {
                return circuitElement;
            }
        }
        return null;
    }

    private AlertDialog buildElementValueDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);

        builder.setTitle(R.string.element_value_dialog_title);

        builder.setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String inputText = input.getText().toString();
                    if (inputText.length() < 1) {
                        return;
                    }
                    mSelectedElement.setValue(Double.parseDouble(inputText));
                    selectElement(mSelectedElement);
                    input.setText("");
                    dialog.cancel();
                })
                .setNegativeButton(R.string.cancel_value_button, (dialog, which) -> {
                    input.setText("");
                    dialog.cancel();
                });

        builder.setCancelable(true);

        return builder.create();
    }

    private AlertDialog buildAnalysisDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.analysis_title);
        builder.setItems(R.array.analysis_options, (dialog, which) -> {
            switch (which) {
                case AC_ANALYSIS_INDEX:
                    Intent acAnalysisIntent = new Intent(this, ACAnalysisActivity.class);
                    acAnalysisIntent.putExtra(SCALED_PHOTO, mImageFile.getAbsolutePath());
                    acAnalysisIntent.putExtra(PROCESSING_RESULT, mProcessingResult);
                    acAnalysisIntent.putParcelableArrayListExtra(ELEMENT_LIST, mElementList);
                    startActivity(acAnalysisIntent);
                    break;
                case DC_ANALYSIS_INDEX:
                    Intent dcAnalysisIntent = new Intent(this, DCAnalysisActivity.class);
                    dcAnalysisIntent.putExtra(SCALED_PHOTO, mImageFile.getAbsolutePath());
                    dcAnalysisIntent.putExtra(PROCESSING_RESULT, mProcessingResult);
                    dcAnalysisIntent.putParcelableArrayListExtra(ELEMENT_LIST, mElementList);
                    startActivity(dcAnalysisIntent);
                    break;
            }
        });
        return builder.create();
    }

    private AlertDialog buildElementTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.element_type_dialog_title);
        builder.setItems(R.array.element_type_options, (dialog, which) -> {
            CircuitElement newElement;
            switch (which) {
                case RESISTOR_INDEX:
                    newElement = new Resistor(mSelectedElement);
                    break;
                case INDUCTOR_INDEX:
                    newElement = new Inductor(mSelectedElement);
                    break;
                case VOLTAGE_SOURCE_INDEX:
                    newElement = new VoltageSource(mSelectedElement);
                    break;
                case CURRENT_SOURCE_INDEX:
                    newElement = new CurrentSource(mSelectedElement);
                    break;
                case CAPACITOR_INDEX:
                    newElement = new Capacitor(mSelectedElement);
                    break;
                default:
                    return;
            }
            if (mSelectedElement.getElementType() == newElement.getElementType()) {
                return;
            }
            mElementList.remove(mSelectedElement);
            mElementList.add(newElement);
            selectElement(newElement);
        });
        return builder.create();
    }
}
