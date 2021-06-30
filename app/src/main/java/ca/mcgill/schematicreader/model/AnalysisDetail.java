package ca.mcgill.schematicreader.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AnalysisDetail implements Parcelable {
    private int node;
    private double startFrequency;
    private double endFrequency;
    private int steps;

    public AnalysisDetail() {

    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public double getStartFrequency() {
        return startFrequency;
    }

    public void setStartFrequency(double startFrequency) {
        this.startFrequency = startFrequency;
    }

    public double getEndFrequency() {
        return endFrequency;
    }

    public void setEndFrequency(double endFrequency) {
        this.endFrequency = endFrequency;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(node);
        dest.writeDouble(startFrequency);
        dest.writeDouble(endFrequency);
        dest.writeInt(steps);
    }

    private AnalysisDetail(Parcel in) {
        node = in.readInt();
        startFrequency = in.readDouble();
        endFrequency = in.readDouble();
        steps = in.readInt();
    }

    public static final Parcelable.Creator<AnalysisDetail> CREATOR
            = new Parcelable.Creator<AnalysisDetail>() {
        public AnalysisDetail createFromParcel(Parcel in) {
            return new AnalysisDetail(in);
        }

        public AnalysisDetail[] newArray(int size) {
            return new AnalysisDetail[size];
        }
    };
}
