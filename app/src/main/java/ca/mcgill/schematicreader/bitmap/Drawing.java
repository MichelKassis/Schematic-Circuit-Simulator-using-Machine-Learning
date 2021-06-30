package ca.mcgill.schematicreader.bitmap;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Iterator;
import java.util.Map;

import ca.mcgill.schematicreader.model.Box;
import ca.mcgill.schematicreader.utility.NodePositioning;

public class Drawing {
    private static final int CIRCLE_RADIUS = 10;
    private static final int PAINT_ROUND_EDGE = 2;

    public void drawBoxes(Box[] boxes, Canvas canvas, Paint paint) {
        for (Box box : boxes) {
            drawBox(box, canvas, paint);
        }
    }

    public void drawBox(Box box, Canvas canvas, Paint paint) {
        RectF rectF = new RectF(box.getLeft(), box.getTop(), box.getRight(), box.getBottom());
        canvas.drawRoundRect(rectF, PAINT_ROUND_EDGE, PAINT_ROUND_EDGE, paint);
    }

    public void drawNodes(
            NodePositioning nodePositioning,
            Canvas canvas,
            Paint paint,
            Paint textPaint) {
        Iterator iterator = nodePositioning.getPositionMap().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();

            canvas.drawCircle(
                    ((NodePositioning.Position) pair.getValue()).getX(),
                    ((NodePositioning.Position) pair.getValue()).getY(),
                    CIRCLE_RADIUS,
                    paint);

            canvas.drawText(
                    ((int) pair.getKey() == 0) ? "G" : pair.getKey().toString(),
                    ((NodePositioning.Position) pair.getValue()).getValueX(),
                    ((NodePositioning.Position) pair.getValue()).getValueY(),
                    textPaint);

            iterator.remove();
        }
    }

    @SuppressLint("DefaultLocale")
    public void drawValues(
            NodePositioning nodePositioning,
            Canvas canvas,
            Paint paint,
            Paint textPaint,
            double[] values) {
        Iterator iterator = nodePositioning.getPositionMap().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            if ((int) pair.getKey() == 0) continue;
            canvas.drawCircle(
                    ((NodePositioning.Position) pair.getValue()).getX(),
                    ((NodePositioning.Position) pair.getValue()).getY(),
                    CIRCLE_RADIUS,
                    paint);

            canvas.drawText(
                    String.format("%.2f", values[(int) pair.getKey() - 1]) + " V",
                    ((NodePositioning.Position) pair.getValue()).getValueX(),
                    ((NodePositioning.Position) pair.getValue()).getValueY(),
                    textPaint);

            iterator.remove();
        }
    }
}
