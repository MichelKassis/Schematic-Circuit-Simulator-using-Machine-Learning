package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;

public class CurrentSource extends CircuitElement {
    public static final Creator<CurrentSource> CREATOR
            = new Creator<CurrentSource>() {
        public CurrentSource createFromParcel(Parcel in) {
            return new CurrentSource(in);
        }

        public CurrentSource[] newArray(int size) {
            return new CurrentSource[size];
        }
    };
    private static final String UNIT = "Amperes";

    CurrentSource(int nodeIn, int nodeOut, double value) {
        super(nodeIn, nodeOut, value);
    }

    public CurrentSource(CircuitElement circuitElement) {
        super(circuitElement);
    }

    private CurrentSource(Parcel in) {
        super(in);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.CURRENT_SOURCE;
    }

    @Override
    public String getUnit() {
        return UNIT;
    }
}
