package ca.mcgill.schematicreader;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import ca.mcgill.schematicreader.utility.ImageStorage;
import ca.mcgill.schematicreader.utility.StorageCleaner;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 */
public class HomeActivity extends AppCompatActivity {
    public static final String CROPPED_PHOTO = "ca.mcgill.schematicreader.CROPPED_PHOTO";

    static final int TOAST_LENGTH = Toast.LENGTH_SHORT;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PICK_PHOTO_FROM_GALLERY = 2;

    static final int CAMERA_INDEX = 0;
    static final int GALLERY_INDEX = 1;

    static final int CLEAN_STORAGE_INDEX = 0;
    static final int CLEAN_CACHE_INDEX = 1;

    File mStorageDirectory;

    Uri mLatestPhotoUri;

    AlertDialog mSelectImageDialog;
    AlertDialog mOptionsDialog;

    StorageCleaner mStorageCleaner;
    ImageStorage mImageStorage;

    private TextView mSubtitleText;
    private boolean mDebugMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        final Button selectImageButton = findViewById(R.id.select_image_button);
        selectImageButton.setOnClickListener(this::onSelectImageButton);

        final Button optionsButton = findViewById(R.id.options_button);
        optionsButton.setOnClickListener(this::onSelectOptionsButton);

        mSubtitleText = findViewById(R.id.fullscreen_content_sub);
        mSubtitleText.setOnClickListener(this::toggleDebugMode);

        mSelectImageDialog = createSelectImageDialog();
        mOptionsDialog = createOptionsDialog();

        mStorageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        mStorageCleaner = new StorageCleaner(mStorageDirectory, getCacheDir());

        mImageStorage = new ImageStorage(mStorageDirectory);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void toggleDebugMode(View view) {
        mDebugMode = !mDebugMode;
        mSubtitleText.setTextColor(mDebugMode ? Color.RED : Color.WHITE);
    }

    private void onSelectImageButton(View view) {
        mSelectImageDialog.show();
    }

    private void onSelectOptionsButton(View view) {
        mOptionsDialog.show();
    }

    private AlertDialog createSelectImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.select_image_dialog_title);
        builder.setItems(R.array.image_select_options, (dialog, which) -> {
            if (which == CAMERA_INDEX) {
                dispatchTakePictureIntent();
            } else if (which == GALLERY_INDEX) {
                dispatchPickPhotoIntent();
            }
        });

        return builder.create();
    }

    private AlertDialog createOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.options_dialog_title);
        builder.setItems(R.array.options_options, ((dialog, which) -> {
            if (which == CLEAN_STORAGE_INDEX) {
                try {
                    mStorageCleaner.cleanStorage();
                } catch (IOException e) {
                    showToast("An IOException has occurred.");
                }
            } else if (which == CLEAN_CACHE_INDEX) {
                try {
                    mStorageCleaner.cleanCache();
                } catch (IOException e) {
                    showToast("An IOException has occurred.");
                }
            }
        }));

        return builder.create();
    }

    private void dispatchPickPhotoIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_PICK_PHOTO_FROM_GALLERY);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = mImageStorage.createImageFile();
            } catch (IOException ex) {
                showToast("An IO error has occurred.");
            }

            if (photoFile != null) {
                mLatestPhotoUri =
                        FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLatestPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, TOAST_LENGTH).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    CropImage.activity(mLatestPhotoUri).start(this);
                }
                break;
            case REQUEST_PICK_PHOTO_FROM_GALLERY:
                if (resultCode == RESULT_OK) {
                    mLatestPhotoUri = intent.getData();
                    CropImage.activity(mLatestPhotoUri).start(this);
                }
                break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(intent);
                if (resultCode == RESULT_OK) {
                    Intent nextActivity;
                    if (mDebugMode) {
                        nextActivity = new Intent(this, ConfirmPhotoActivity.class);
                    } else {
                        nextActivity = new Intent(this, ProcessingActivity.class);
                    }
                    Uri resultUri = result.getUri();
                    nextActivity.putExtra(CROPPED_PHOTO, resultUri.toString());
                    startActivity(nextActivity);
                }
            default:
                break;
        }
    }
}
