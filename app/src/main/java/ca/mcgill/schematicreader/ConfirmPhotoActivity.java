package ca.mcgill.schematicreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import ca.mcgill.schematicreader.bitmap.BitmapConverter;
import ca.mcgill.schematicreader.bitmap.BitmapScaler;
import ca.mcgill.schematicreader.interfaces.JNIImageProcessor;
import ca.mcgill.schematicreader.model.ProcessingResult;
import ca.mcgill.schematicreader.utility.ImageStorage;

public class ConfirmPhotoActivity extends AppCompatActivity {

    public static final String SCALED_PHOTO = "ca.mcgill.schematicreader.SCALED_PHOTO";
    public static final String PROCESSING_RESULT = "ca.mcgill.schematicreader.PROCESSING_RESULT";

    private final static int IMAGE_PROCESSOR_HEIGHT = 500;
    private final static int IMAGE_PROCESSOR_BYTES_PER_PIXEL = 4;
    private final static int TIMER_PERIOD = 1000;

    ImageView mConfirmImageView;
    TextView mLoadingTextView;
    Button mConfirmButton;
    Button mBwButton;
    Uri mPhotoUri;
    int mTimerCount = 1;

    JNIImageProcessor ip;

    ImageStorage imageStorage;

    Bitmap mScaledBitmap = null;
    byte[] mBitmapAsBytes = null;

    Bitmap mBwBitmap = null;
    byte[] mBwBitmapAsBytes = null;

    float mBwThreshold = -1f;

    File mScaledImageFile = null;
    ProcessingResult mProcessingResult = null;

    /*
    1. After activity created, disable buttons and get guess value.
    2. After guess value is obtained, enable the convert to BW button.
    3. After converted to BW, only enable the process button.
    4. After processing starts, disable all.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_photo);

        mConfirmButton = findViewById(R.id.confirm_photo_button);
        mConfirmButton.setOnClickListener((view) -> processPhoto());

        mBwButton = findViewById(R.id.convert_bw_button);
        mBwButton.setOnClickListener(this::convertImageToBw);

        mLoadingTextView = findViewById(R.id.loading_text);
        mLoadingTextView.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        mPhotoUri = Uri.parse(intent.getStringExtra(HomeActivity.CROPPED_PHOTO));

        mConfirmImageView = findViewById(R.id.confirm_image_view);
        Picasso.get().load(mPhotoUri).into(mConfirmImageView);

        imageStorage = new ImageStorage(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        ip = new JNIImageProcessor();

        disableButton(mConfirmButton);
        disableButton(mBwButton);
        mLoadingTextView.setText(R.string.loading_text);

        new Thread(() -> {
            mBwThreshold = getGuessBwValue();
            mLoadingTextView.post(() -> mLoadingTextView.setText(null));
            mBwButton.post(() -> {
                mBwButton.setText(getString(R.string.convert_bw_button, mBwThreshold));
                enableButton(mBwButton);
            });
        }).start();
    }

    public void convertImageToBw(View view) {
        disableButton(mBwButton);
        mLoadingTextView.setText(R.string.converting_text);
        new Thread(() -> {
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

            try {
                File bwImage = imageStorage.createBwImageFile();
                BitmapConverter.writeBitmapToFile(bwImage, mBwBitmap);
                runOnUiThread(() -> Picasso.get().load(bwImage).into(mConfirmImageView));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLoadingTextView.post(() -> mLoadingTextView.setText(null));
            mConfirmButton.post(() -> enableButton(mConfirmButton));
        }).start();
    }

    private float getGuessBwValue() {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap photoBitmap = BitmapFactory.decodeFile(mPhotoUri.getPath(), decodeOptions);
        mScaledBitmap = BitmapScaler.scaleBitmap(photoBitmap, IMAGE_PROCESSOR_HEIGHT);

        try {
            mScaledImageFile = imageStorage.createScaledImageFile();
            BitmapConverter.writeBitmapToFile(mScaledImageFile, mScaledBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBitmapAsBytes = BitmapConverter.convertBitmapToByteArray(mScaledBitmap);
        int widthInBytes = mScaledBitmap.getWidth() * IMAGE_PROCESSOR_BYTES_PER_PIXEL;

        return ip.guessBwThreshold(
                mScaledBitmap.getWidth(),
                mScaledBitmap.getHeight(),
                IMAGE_PROCESSOR_BYTES_PER_PIXEL,
                mBitmapAsBytes,
                widthInBytes,
                mScaledBitmap.getByteCount()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.deleteQuietly(new File(mPhotoUri.getPath()));
    }

    private void processPhoto() {
        disableButton(mConfirmButton);
        mLoadingTextView.setText(getString(R.string.processing_text, 0));
        Timer t = startTimer();

        new Thread(() -> {
            int widthInBytes = mBwBitmap.getWidth() * IMAGE_PROCESSOR_BYTES_PER_PIXEL;
            mProcessingResult = ip.process(
                    mBwBitmap.getWidth(),
                    mBwBitmap.getHeight(),
                    IMAGE_PROCESSOR_BYTES_PER_PIXEL,
                    mBwBitmapAsBytes,
                    widthInBytes,
                    mBwBitmap.getByteCount()
            );
            runOnUiThread(() -> {
                t.cancel();
                dispatchProcessingActivityIntent();
            });
        }).start();
    }

    private void disableButton(Button button) {
        button.setEnabled(false);
        button.setClickable(false);
        button.setAlpha(.5f);
    }

    private void enableButton(Button button) {
        button.setEnabled(true);
        button.setClickable(true);
        button.setAlpha(1f);
    }

    private Timer startTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mLoadingTextView.post(() -> {
                    mLoadingTextView.setText(getString(R.string.processing_text, mTimerCount));
                    mTimerCount++;
                });
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
        return timer;
    }

    private void dispatchProcessingActivityIntent() {
        Intent processingActivityIntent = new Intent(this, ProcessingActivity.class);
        processingActivityIntent.putExtra(SCALED_PHOTO, mScaledImageFile.getAbsolutePath());
        processingActivityIntent.putExtra(PROCESSING_RESULT, mProcessingResult);
        startActivity(processingActivityIntent);
        finish();
    }
}
