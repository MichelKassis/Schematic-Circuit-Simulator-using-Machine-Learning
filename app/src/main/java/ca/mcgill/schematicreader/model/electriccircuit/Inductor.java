package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;
import android.os.Parcelable;

public class Inductor extends CircuitElement {
    private static final String UNIT = "Henry";

    Inductor(int nodeIn, int nodeOut, double value) {
        super(nodeIn, nodeOut, value);
    }

    public Inductor(CircuitElement circuitElement) {
        super(circuitElement);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.INDUCTOR;
    }

    @Override
    public String getUnit() {
        return UNIT;
    }

    public static final Parcelable.Creator<Inductor> CREATOR
            = new Parcelable.Creator<Inductor>() {
        public Inductor createFromParcel(Parcel in) {
            return new Inductor(in);
        }

        public Inductor[] newArray(int size) {
            return new Inductor[size];
        }
    };

    private Inductor(Parcel in) {
        super(in);
    }
}
