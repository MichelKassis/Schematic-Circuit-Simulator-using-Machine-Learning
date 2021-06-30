package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;
import android.os.Parcelable;

public class Capacitor extends CircuitElement {
    private static final String UNIT = "Farad";

    Capacitor(int nodeIn, int nodeOut, double value) {
        super(nodeIn, nodeOut, value);
    }

    public Capacitor(CircuitElement circuitElement) {
        super(circuitElement);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.CAPACITOR;
    }

    @Override
    public String getUnit() {
        return UNIT;
    }

    public static final Parcelable.Creator<Capacitor> CREATOR
            = new Parcelable.Creator<Capacitor>() {
        public Capacitor createFromParcel(Parcel in) {
            return new Capacitor(in);
        }

        public Capacitor[] newArray(int size) {
            return new Capacitor[size];
        }
    };

    private Capacitor(Parcel in) {
        super(in);
    }
}
