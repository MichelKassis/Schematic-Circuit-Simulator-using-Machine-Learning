package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;
import android.os.Parcelable;

public class VoltageSource extends CircuitElement {
    private static final String UNIT = "Volt";

    VoltageSource(int nodeIn, int nodeOut, double value) {
        super(nodeIn, nodeOut, value);
    }

    public VoltageSource(CircuitElement circuitElement) {
        super(circuitElement);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.VOLTAGE_SOURCE;
    }

    @Override
    public String getUnit() {
        return UNIT;
    }

    public static final Parcelable.Creator<VoltageSource> CREATOR
            = new Parcelable.Creator<VoltageSource>() {
        public VoltageSource createFromParcel(Parcel in) {
            return new VoltageSource(in);
        }

        public VoltageSource[] newArray(int size) {
            return new VoltageSource[size];
        }
    };

    private VoltageSource(Parcel in) {
        super(in);
    }
}
