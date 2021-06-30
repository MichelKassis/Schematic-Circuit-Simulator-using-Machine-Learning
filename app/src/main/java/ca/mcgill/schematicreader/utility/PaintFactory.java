package ca.mcgill.schematicreader.utility;

import android.graphics.Paint;

public class PaintFactory {
    private static final int PAINT_STROKE_WIDTH = 5;
    private static final int TEXT_SIZE = 50;

    public static Paint createPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(PAINT_STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        return paint;
    }

    public static Paint createTextPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(TEXT_SIZE);
        return paint;
    }
}
