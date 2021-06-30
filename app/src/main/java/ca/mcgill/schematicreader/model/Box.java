package ca.mcgill.schematicreader.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Box implements Parcelable {
    private int left;
    private int top;
    private int right;
    private int bottom;

    public Box() {
    }

    public Box(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
}

    public static double getMinDistance2(Box a, Box b) {
        Box outer = new Box(Math.min(a.top, b.top),
                Math.min(a.left, b.left),
                Math.max(a.bottom, b.bottom),
                Math.max(a.right, b.right)
        );

        double inner_width = Math.max(0, outer.width() - a.width() - b.width());
        double inner_height = Math.max(0, outer.height() - a.height() - b.height());

        return inner_width * inner_width + inner_height * inner_height;
    }

    public int width() {
        return right - left + 1;
    }

    public int height() {
        return bottom - top + 1;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(left);
        dest.writeInt(top);
        dest.writeInt(right);
        dest.writeInt(bottom);
    }

    private Box(Parcel in) {
        left = in.readInt();
        top = in.readInt();
        right = in.readInt();
        bottom = in.readInt();
    }

    public static final Parcelable.Creator<Box> CREATOR
            = new Parcelable.Creator<Box>() {
        public Box createFromParcel(Parcel in) {
            return new Box(in);
        }

        public Box[] newArray(int size) {
            return new Box[size];
        }
    };
}
