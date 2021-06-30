package ca.mcgill.schematicreader;

import android.graphics.Bitmap;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;
import ca.mcgill.schematicreader.bitmap.BitmapScaler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class BitmapScalerTest {
    @Test
    public void testScaleBitmap_withSmallerBitmap_returnsOriginal() {
        final Bitmap expectedBitmap = createTestBitmap();

        final Bitmap actualBitmap = BitmapScaler.scaleBitmap(expectedBitmap, 600);

        assertSame(expectedBitmap, actualBitmap);
    }

    @Test
    public void testScaleBitmap_withLargerBitmap_scaledDown() {
        final Bitmap testBitmap = createTestBitmap();

        final Bitmap actualBitmap = BitmapScaler.scaleBitmap(testBitmap, 25);

        assertThat(actualBitmap.getWidth(), is(25));
    }

    @Test
    public void testScaleBitmap_withBitmap_ratioPreserved() {
        final Bitmap testBitmap = createTestBitmap();
        double expectedRatio = testBitmap.getWidth() / testBitmap.getHeight();

        final Bitmap actualBitmap = BitmapScaler.scaleBitmap(testBitmap, 25);

        double actualRatio = actualBitmap.getWidth() / actualBitmap.getHeight();
        assertThat(actualRatio, closeTo(expectedRatio, 0.1));
    }

    private Bitmap createTestBitmap() {
        return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
    }
}
