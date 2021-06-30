package ca.mcgill.schematicreader.OCR;
import com.googlecode.tesseract.android.TessBaseAPI;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;


import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import ca.mcgill.schematicreader.utility.ImageStorage;
import flanagan.io.FileOutput;


public class OCR {

    private static final String LANG_FILE = "tessdata/eng.traineddata";

    private TessBaseAPI mTess;
    private static final String TESSERACT_SUBDIRECTORY = "tesseract";
    private static final String TESSDATA_SUBDIRECTORY = "tessdata";
    private File tesseractDirectory;
    private File tessDataDirectory;
    private boolean mkdirFailed = false;
    private File imageDirectoryBase;

    public OCR(File imageDirectory, Context context) {
        this.imageDirectoryBase = imageDirectory;
        mTess = new TessBaseAPI();
        this.tesseractDirectory = getOrCreateDir(imageDirectoryBase.getParent() +
                File.separator + TESSERACT_SUBDIRECTORY);
        this.tessDataDirectory = getOrCreateDir(tesseractDirectory.getAbsolutePath() +
                File.separator + TESSDATA_SUBDIRECTORY);

        String datapath = tesseractDirectory.getAbsolutePath();
        String language = "eng";

        try {
            copyAsset(context.getAssets().open(LANG_FILE), datapath);
        }
        catch(IOException e){

        }
        mTess.init(datapath, language);
    }

    private void copyAsset(InputStream in, String path) {
        OutputStream out = null;
        String dirout= path + "/tessdata/";
        File outFile = new File(dirout, "eng.traineddata");
            try {
                (new File(dirout)).mkdirs();
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("tag", "Error creating files", e);
            }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public String getText(Bitmap bitmap) {

        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();

        return result;
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

    private File getOrCreateDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdir()) {
                mkdirFailed = true;
            }
        }
        return file;
    }
}