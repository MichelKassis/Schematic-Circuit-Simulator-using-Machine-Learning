package ca.mcgill.schematicreader.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ProcessingResult implements Parcelable {
    private int boxes;
    private int connections;
    private Box[] boxArray;
    private int groundNode;
    private int[] connectionMatrixWithId;
    private int[] connectionMatrixWithSides;

    public ProcessingResult() {

    }

    public void setBoxes(int boxes) {
        this.boxes = boxes;
    }

    public int getBoxes() {
        return boxes;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public int getConnections() {
        return connections;
    }

    public Box[] getBoxArray() {
        return boxArray;
    }

    public void setBoxArray(Box[] boxArray) {
        this.boxArray = boxArray;
    }

    public int getGroundNode() {
        return groundNode;
    }

    public void setGroundNode(int groundNode) {
        this.groundNode = groundNode;
    }


    public int[] getConnectionMatrixWithId() {
        return connectionMatrixWithId;
    }

    public void setConnectionMatrixWithId(int[] connectionMatrixWithId) {
        this.connectionMatrixWithId = connectionMatrixWithId;
    }

    private ProcessingResult(Parcel in) {
        boxes = in.readInt();
        connections = in.readInt();
        boxArray = in.createTypedArray(Box.CREATOR);
        groundNode = in.readInt();
        connectionMatrixWithId = in.createIntArray();
        connectionMatrixWithSides = in.createIntArray();
    }

    public int[] getConnectionMatrixWithSides() {
        return connectionMatrixWithSides;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public void setConnectionMatrixWithSides(int[] connectionMatrixWithSides) {
        this.connectionMatrixWithSides = connectionMatrixWithSides;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(boxes);
        dest.writeInt(connections);
        dest.writeTypedArray(boxArray, flags);
        dest.writeInt(groundNode);
        dest.writeIntArray(connectionMatrixWithId);
        dest.writeIntArray(connectionMatrixWithSides);
    }

    public static final Parcelable.Creator<ProcessingResult> CREATOR
            = new Parcelable.Creator<ProcessingResult>() {
        public ProcessingResult createFromParcel(Parcel in) {
            return new ProcessingResult(in);
        }

        public ProcessingResult[] newArray(int size) {
            return new ProcessingResult[size];
        }
    };
}
