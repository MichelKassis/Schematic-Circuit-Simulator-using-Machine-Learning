package ca.mcgill.schematicreader.model.electriccircuit;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import ca.mcgill.schematicreader.model.Box;

public abstract class CircuitElement implements Parcelable {

    private static final Map<String, ElementType> ELEMENT_TYPE_MAP = new HashMap<String, ElementType>() {{
        put("resistor", ElementType.RESISTOR);
        put("inductor", ElementType.INDUCTOR);
        put("capacitor", ElementType.CAPACITOR);

        put("currentsource", ElementType.CURRENT_SOURCE);

        put("acvoltagesource", ElementType.VOLTAGE_SOURCE);

        put("dcvoltagesource", ElementType.VOLTAGE_SOURCE);



    }};

    private int nodeIn;
    private int nodeOut;
    private Side sideIn;
    private Side sideOut;

    CircuitElement(int nodeIn, int nodeOut, double value) {
        this.nodeIn = nodeIn;
        this.nodeOut = nodeOut;
        this.sideIn = sideIn;
        this.sideOut = sideOut;
        this.value = value;
    }

    private Box box;
    private double value;

    CircuitElement(CircuitElement circuitElement) {
        this.value = circuitElement.value;
        this.box = circuitElement.box;
        this.nodeIn = circuitElement.nodeIn;
        this.nodeOut = circuitElement.nodeOut;
        this.sideIn = circuitElement.sideIn;
        this.sideOut = circuitElement.sideOut;
    }

    public static CircuitElement build(String elementTypeString, int nodeIn, int nodeOut, Side sideIn, Side sideOut, double value, Box box) {
        ElementType elementType = ELEMENT_TYPE_MAP.get(elementTypeString);
        if (elementType == null) {
            return null;
        }

        CircuitElement result;

        switch (elementType) {
            case RESISTOR:
                result = new Resistor(nodeIn, nodeOut, value);
                break;
            case INDUCTOR:
                result = new Inductor(nodeIn, nodeOut, value);
                break;
            case CAPACITOR:
                result = new Capacitor(nodeIn, nodeOut, value);
                break;
            case VOLTAGE_SOURCE:
                result = new VoltageSource(nodeIn, nodeOut, value);
                break;
            case CURRENT_SOURCE:
                result = new CurrentSource(nodeIn, nodeOut, value);
                break;
            default:
                return null;
        }

        result.setNodeIn(nodeIn);
        result.setNodeOut(nodeOut);
        result.setSideIn(sideIn);
        result.setSideOut(sideOut);
        result.setBox(box);
        return result;
    }

    public abstract ElementType getElementType();

    public int getNodeIn() {
        return this.nodeIn;
    }

    public void setNodeIn(int nodeIn) {
        this.nodeIn = nodeIn;
    }

    public int getNodeOut() {
        return this.nodeOut;
    }

    public void setNodeOut(int nodeOut) {
        this.nodeOut = nodeOut;
    }

    public Side getSideIn() {
        return this.sideIn;
    }

    public void setSideIn(Side sideIn) {
        this.sideIn = sideIn;
    }

    public Side getSideOut() {
        return this.sideOut;
    }

    public void setSideOut(Side sideOut) {
        this.sideOut = sideOut;
    }

    public Box getBox() {
        return box;
    }

    private void setBox(Box box) {
        this.box = box;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getValueString() {
        return "" + getValue() + " " + getUnit();
    }

    public abstract String getUnit();

    @Override
    public int describeContents() {
        return 0;
    }

    CircuitElement(Parcel in) {
        sideIn = Side.values()[in.readInt()];
        sideOut = Side.values()[in.readInt()];
        nodeIn = in.readInt();
        nodeOut = in.readInt();
        value = in.readDouble();
        box = in.readParcelable(Box.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(sideIn.ordinal());
        dest.writeInt(sideOut.ordinal());
        dest.writeInt(nodeIn);
        dest.writeInt(nodeOut);
        dest.writeDouble(value);
        dest.writeParcelable(box, flags);
    }

    public enum ElementType {
        RESISTOR,
        INDUCTOR,
        VOLTAGE_SOURCE,
        CURRENT_SOURCE,
        CAPACITOR
    }

    public enum Side {
        TOP,
        LEFT,
        BOTTOM,
        RIGHT
    }
}
