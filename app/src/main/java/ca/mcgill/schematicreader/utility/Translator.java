package ca.mcgill.schematicreader.utility;

import java.util.HashMap;
import java.util.Map;

import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;

public class Translator {
    private static final Map<String, String> ELEMENT_TYPE_MAP_LABEL = new HashMap<String, String>() {{
        put("resistor", "Resistor");
        put("inductor", "Inductor");
        put("capacitor", "Capacitor");
        put("voltagesource", "Voltage Source");
        put("currentsource", "Current Source");
    }};

    private static final Map<CircuitElement.ElementType, String> ELEMENT_TYPE_MAP = new HashMap<CircuitElement.ElementType, String>() {{
        put(CircuitElement.ElementType.RESISTOR, "Resistor");
        put(CircuitElement.ElementType.INDUCTOR, "Inductor");
        put(CircuitElement.ElementType.CAPACITOR, "Capacitor");
        put(CircuitElement.ElementType.VOLTAGE_SOURCE, "Voltage Source");
        put(CircuitElement.ElementType.CURRENT_SOURCE, "Current Source");
    }};

    public static String translateElementType(String label) {
        return ELEMENT_TYPE_MAP_LABEL.get(label);
    }

    public static String translateElementType(CircuitElement.ElementType elementType) {
        return ELEMENT_TYPE_MAP.get(elementType);
    }
}
