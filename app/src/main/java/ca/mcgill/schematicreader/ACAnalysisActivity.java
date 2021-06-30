package ca.mcgill.schematicreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.bitmap.Drawing;
import ca.mcgill.schematicreader.model.AnalysisDetail;
import ca.mcgill.schematicreader.model.ProcessingResult;
import ca.mcgill.schematicreader.model.electriccircuit.Circuit;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;
import ca.mcgill.schematicreader.utility.NodePositioning;
import ca.mcgill.schematicreader.utility.PaintFactory;

import static ca.mcgill.schematicreader.ProcessingActivity.ELEMENT_LIST;
import static ca.mcgill.schematicreader.ResultViewActivity.PAINT_COLOR;

public class ACAnalysisActivity extends AppCompatActivity {
    public static String ANALYSIS_DETAIL = "ANALYSIS_DETAIL";

    private static int MAX_BOUND = 1000000;

    private ProcessingResult mProcessingResult;
    private File mImageFile;
    private ArrayList<CircuitElement> mElementList;
    private Circuit mCircuit;

    private Bitmap mDrawBitmap;
    private Canvas mCanvas;
    private ImageView mImageView;

    private EditText mNodeNumber;
    private EditText mStartFrequency;
    private EditText mEndFrequency;
    private EditText mSteps;

    private NodePositioning mNodePositioning;

    private Drawing mDrawing;
    private Paint mPaint;
    private Paint mTextPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acanalysis);

        Intent intent = getIntent();
        mProcessingResult = intent.getParcelableExtra(ConfirmPhotoActivity.PROCESSING_RESULT);
        mImageFile = new File(intent.getStringExtra(ConfirmPhotoActivity.SCALED_PHOTO));
        mElementList = intent.getParcelableArrayListExtra(ELEMENT_LIST);

        mCircuit = new Circuit(mElementList);

        mPaint = PaintFactory.createPaint(PAINT_COLOR);
        mTextPaint = PaintFactory.createTextPaint(PAINT_COLOR);
        mDrawing = new Drawing();

        mNodePositioning = new NodePositioning(mCircuit);

        Bitmap imageBitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath());
        mDrawBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mDrawBitmap);
        mCanvas.drawBitmap(imageBitmap, 0, 0, null);

        mDrawing.drawNodes(mNodePositioning, mCanvas, mPaint, mTextPaint);

        mImageView = findViewById(R.id.ac_analysis_view);
        mImageView.setImageBitmap(mDrawBitmap);

        mNodeNumber = findViewById(R.id.node_input);
        mStartFrequency = findViewById(R.id.start_frequency_input);
        mEndFrequency = findViewById(R.id.end_frequency_input);
        mSteps = findViewById(R.id.steps_input);

        findViewById(R.id.run_analysis_button).setOnClickListener((view) -> {
            AnalysisDetail analysisDetail = parseInputs();
            if (analysisDetail == null) {
                Toast.makeText(this, "Input error", Toast.LENGTH_LONG).show();
                return;
            }
            Intent graphIntent = new Intent(this, AnalysisGraphActivity.class);
            graphIntent.putExtra(ANALYSIS_DETAIL, analysisDetail);
            graphIntent.putParcelableArrayListExtra(ELEMENT_LIST, mElementList);
            startActivity(graphIntent);
        });
    }

    private AnalysisDetail parseInputs() {
        AnalysisDetail analysisDetail = new AnalysisDetail();
        try {
            int nodeNumber = Integer.parseInt(mNodeNumber.getText().toString());
            if (nodeNumber < 1 || nodeNumber > mCircuit.getNodeNumber()) {
                return null;
            }

            double startFrequency = Double.parseDouble(mStartFrequency.getText().toString());
            double endFrequency = Double.parseDouble(mEndFrequency.getText().toString());

            if (startFrequency > endFrequency
                    || startFrequency < 0
                    || endFrequency < 0
                    || startFrequency > MAX_BOUND
                    || endFrequency > MAX_BOUND) {
                return null;
            }

            int steps = Integer.parseInt(mSteps.getText().toString());

            if (steps < 1 || steps > MAX_BOUND) {
                return null;
            }

            analysisDetail.setNode(nodeNumber - 1);
            analysisDetail.setStartFrequency(startFrequency);
            analysisDetail.setEndFrequency(endFrequency);
            analysisDetail.setSteps(steps);

        } catch (Exception e) {
            return null;
        }
        return analysisDetail;
    }
}
