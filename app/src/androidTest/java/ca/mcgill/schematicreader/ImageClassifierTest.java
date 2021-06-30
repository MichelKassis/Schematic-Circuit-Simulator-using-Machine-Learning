package ca.mcgill.schematicreader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import ca.mcgill.schematicreader.neuralnetwork.ImageClassifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ImageClassifierTest {

    private final static String VOLTAGE_SOURCE_IMAGE = "ca/mcgill/schematicreader/voltagesource.jpg";
    private final static String CAPACITOR_IMAGE = "ca/mcgill/schematicreader/capacitor.png";
    private final static String RESISTOR_IMAGE = "ca/mcgill/schematicreader/resistor.png";
    private final static String INDUCTOR_IMAGE = "ca/mcgill/schematicreader/inductor.png";

    private Bitmap voltageSourceBitmap;
    private Bitmap capacitorBitmap;
    private Bitmap resistorBitmap;
    private Bitmap inductorBitmap;

    private ImageClassifier underTest;

    @Before
    public void setUp() throws IOException, NullPointerException {
        InputStream inputStream = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream(VOLTAGE_SOURCE_IMAGE);
        voltageSourceBitmap = BitmapFactory.decodeStream(inputStream);

        inputStream = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(CAPACITOR_IMAGE));
        capacitorBitmap = BitmapFactory.decodeStream(inputStream);

        inputStream = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(RESISTOR_IMAGE));
        resistorBitmap = BitmapFactory.decodeStream(inputStream);

        inputStream = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(INDUCTOR_IMAGE));
        inductorBitmap = BitmapFactory.decodeStream(inputStream);

        underTest = new ImageClassifier(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testClassifyImage_withVoltageSource() {
        String expected = "voltagesource";

        String actual = underTest.classifyFrame(voltageSourceBitmap);

        assertThat(actual, is(expected));
    }

    @Test
    public void testClassifyImage_withCapacitor() {
        String expected = "capacitor";

        String actual = underTest.classifyFrame(capacitorBitmap);

        assertThat(actual, is(expected));
    }

    @Test
    public void testClassifyImage_withResistor() {
        String expected = "resistor";

        String actual = underTest.classifyFrame(resistorBitmap);

        assertThat(actual, is(expected));
    }

    @Test
    public void testClassifyImage_withInductor() {
        String expected = "inductor";

        String actual = underTest.classifyFrame(inductorBitmap);

        assertThat(actual, is(expected));
    }

    @Test
    public void testGetLabelList_returnsLabels() {
        List<String> actualLabelList = underTest.getLabelList();

        assertThat(actualLabelList.get(0), is("capacitor"));
        assertThat(actualLabelList.get(1), is("inductor"));
        assertThat(actualLabelList.get(2), is("resistor"));
        assertThat(actualLabelList.get(3), is("voltagesource"));
    }
}
