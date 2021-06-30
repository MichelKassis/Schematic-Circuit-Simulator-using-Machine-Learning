package ca.mcgill.schematicreader.utility;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

import ca.mcgill.schematicreader.model.Box;
import ca.mcgill.schematicreader.model.electriccircuit.Circuit;
import ca.mcgill.schematicreader.model.electriccircuit.CircuitElement;

public class NodePositioning {

    private static int VALUE_OFFSET_X = -10;
    private static int VALUE_OFFSET_Y = -40;

    private Map<Integer, Position> positionMap;

    @SuppressLint("UseSparseArrays")
    public NodePositioning(Circuit circuit) {
        positionMap = new HashMap<>();
        for (CircuitElement circuitElement : circuit.getComponents()) {
            int nodeIn = circuitElement.getNodeIn();

            if (!positionMap.containsKey(nodeIn)) {
                positionMap.put(nodeIn, sideToPosition(circuitElement.getSideIn(), circuitElement.getBox()));
            }
        }
        for (CircuitElement circuitElement : circuit.getComponents()) {
            int nodeOut = circuitElement.getNodeOut();

            if (!positionMap.containsKey(nodeOut)) {
                positionMap.put(nodeOut, sideToPosition(circuitElement.getSideOut(), circuitElement.getBox()));
            }
        }
    }

    public Map<Integer, Position> getPositionMap() {
        return positionMap;
    }

    private Position sideToPosition(CircuitElement.Side side, Box box) {
        Position position = new Position();
        switch (side) {
            case TOP:
                position.setX((box.getLeft() + box.getRight()) / 2);
                position.setY(box.getTop());
                break;
            case LEFT:
                position.setX(box.getLeft());
                position.setY((box.getTop() + box.getBottom()) / 2);
                break;
            case BOTTOM:
                position.setX((box.getLeft() + box.getRight()) / 2);
                position.setY(box.getBottom());
                break;
            case RIGHT:
                position.setX(box.getRight());
                position.setY((box.getTop() + box.getBottom()) / 2);
                break;
        }
        position.setValueX(position.getX() + VALUE_OFFSET_X);
        position.setValueY(position.getY() + VALUE_OFFSET_Y);
        return position;
    }

    public static class Position {
        private int x;
        private int y;

        private int valueX;
        private int valueY;

        public int getX() {
            return x;
        }

        void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        void setY(int y) {
            this.y = y;
        }

        public int getValueX() {
            return valueX;
        }

        void setValueX(int valueX) {
            this.valueX = valueX;
        }

        public int getValueY() {
            return valueY;
        }

        void setValueY(int valueY) {
            this.valueY = valueY;
        }
    }
}
