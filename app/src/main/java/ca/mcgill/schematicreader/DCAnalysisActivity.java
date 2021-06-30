package ca.mcgill.schematicreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.bitmap.Drawing;
import ca.mcgill.schematicreader.model.ProcessingResult;
import ca.mcgill.schematicreader.model.electriccircuit.Circuit;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;
import ca.mcgill.schematicreader.utility.NodePositioning;
import ca.mcgill.schematicreader.utility.PaintFactory;

import static ca.mcgill.schematicreader.ProcessingActivity.ELEMENT_LIST;
import static ca.mcgill.schematicreader.ResultViewActivity.PAINT_COLOR;

public class DCAnalysisActivity extends AppCompatActivity {

    private ProcessingResult mProcessingResult;
    private File mImageFile;
    private ArrayList<CircuitElement> mElementList;
    private Circuit mCircuit;
    private double[] mDcResult;

    private Bitmap mDrawBitmap;
    private Canvas mCanvas;
    private ImageView mImageView;

    private NodePositioning mNodePositioning;

    private Drawing mDrawing;
    private Paint mPaint;
    private Paint mTextPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dcanalysis);

        Intent intent = getIntent();
        mProcessingResult = intent.getParcelableExtra(ConfirmPhotoActivity.PROCESSING_RESULT);
        mImageFile = new File(intent.getStringExtra(ConfirmPhotoActivity.SCALED_PHOTO));
        mElementList = intent.getParcelableArrayListExtra(ELEMENT_LIST);

        mCircuit = new Circuit(mElementList);

        mPaint = PaintFactory.createPaint(PAINT_COLOR);
        mTextPaint = PaintFactory.createTextPaint(PAINT_COLOR);
        mDrawing = new Drawing();

        mNodePositioning = new NodePositioning(mCircuit);

        mDcResult = mCircuit.do_DC_simulation();

        Bitmap imageBitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath());
        mDrawBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mDrawBitmap);
        mCanvas.drawBitmap(imageBitmap, 0, 0, null);

        mDrawing.drawValues(mNodePositioning, mCanvas, mPaint, mTextPaint, mDcResult);

        mImageView = findViewById(R.id.dc_analysis_view);
        mImageView.setImageBitmap(mDrawBitmap);
    }
}
