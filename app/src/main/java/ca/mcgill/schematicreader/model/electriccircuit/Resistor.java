package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;
import android.os.Parcelable;

public class Resistor extends CircuitElement {
    private static final String UNIT = "Ohm";

    Resistor(int nodeIn, int nodeOut, double value) {
        super(nodeIn, nodeOut, value);
    }

    public Resistor(CircuitElement circuitElement) {
        super(circuitElement);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.RESISTOR;
    }

    @Override
    public String getUnit() {
        return UNIT;
    }

    public static final Parcelable.Creator<Resistor> CREATOR
            = new Parcelable.Creator<Resistor>() {
        public Resistor createFromParcel(Parcel in) {
            return new Resistor(in);
        }

        public Resistor[] newArray(int size) {
            return new Resistor[size];
        }
    };

    private Resistor(Parcel in) {
        super(in);
    }
}
