package ca.mcgill.schematicreader.model.electriccircuit;

import ca.mcgill.schematicreader.model.Box;

public class Label {
    public Box box;
    public String string;
    public LabelType type;

    public Label(LabelType type, String string, Box box) {
        this.string = string;
        this.box = box;
        this.type = type;
    }

    public enum LabelType {POLARITY, VALUE, EQUATION, NAME}

    ;
}
