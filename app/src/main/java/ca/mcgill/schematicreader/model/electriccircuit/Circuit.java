package ca.mcgill.schematicreader.model.electriccircuit;

import java.util.ArrayList;

import ca.mcgill.schematicreader.model.ACResult;
import flanagan.complex.Complex;
import flanagan.complex.ComplexMatrix;
import flanagan.math.Matrix;

public class Circuit {
    public Matrix G;
    public Matrix C;
    public double[] b;

    private int nodeNumber;

    private ArrayList<CircuitElement> components;

    public int getNodeNumber() {
        return nodeNumber;
    }

    public void setNodeNumber(int nodeNumber) {
        this.nodeNumber = nodeNumber;
    }

    public ArrayList<CircuitElement> getComponents() {
        return components;
    }

    public Circuit(ArrayList<CircuitElement> list) {
        this.components = new ArrayList<>(list);

        int num_inductors = 0;
        int num_ind_voltage_sources = 0;
        int num_nodes = 0;

        for (CircuitElement element : components) {
            if (element.getElementType() == CircuitElement.ElementType.VOLTAGE_SOURCE) {
                num_ind_voltage_sources++;
            }
            if (element.getElementType() == CircuitElement.ElementType.INDUCTOR) num_inductors++;

            if (element.getNodeIn() > num_nodes) num_nodes = element.getNodeIn();
            if (element.getNodeOut() > num_nodes) num_nodes = element.getNodeOut();
        }

        if (num_nodes == Integer.MAX_VALUE) num_nodes = 0;

        int size = num_nodes + num_inductors + num_ind_voltage_sources;

        this.G = new Matrix(size, size, 0);

        this.C = new Matrix(size, size, 0);

        this.b = new double[size];

        if (size <= 1) {
            return;
        }

        int current_voltage_source_offset_offset = num_nodes + 1;
        int current_inductor_offset = num_nodes + num_ind_voltage_sources + 1;

        for (CircuitElement element : components) {

            // Resistor Stamp
            if (element.getElementType() == CircuitElement.ElementType.RESISTOR) {
                double g = 1 / element.getValue();
                if (element.getNodeIn() == 0 && element.getNodeOut() == 0) {
                } else if (element.getNodeIn() == 0) {
                    addToMatrix(G, element.getNodeOut(), element.getNodeOut(), g);
                } else if (element.getNodeOut() == 0) {
                    addToMatrix(G, element.getNodeIn(), element.getNodeIn(), g);
                } else {
                    addToMatrix(G, element.getNodeIn(), element.getNodeIn(), g);
                    addToMatrix(G, element.getNodeOut(), element.getNodeOut(), g);
                    addToMatrix(G, element.getNodeOut(), element.getNodeIn(), -g);
                    addToMatrix(G, element.getNodeIn(), element.getNodeOut(), -g);
                }
            }

            // Capacitor Stamp
            if (element.getElementType() == CircuitElement.ElementType.CAPACITOR) {
                double Cap = element.getValue();

                if (element.getNodeIn() == 0 && element.getNodeOut() == 0) {
                } else if (element.getNodeIn() == 0) {
                    addToMatrix(C, element.getNodeOut(), element.getNodeOut(), Cap);
                } else if (element.getNodeOut() == 0) {
                    addToMatrix(C, element.getNodeIn(), element.getNodeIn(), Cap);
                } else {
                    addToMatrix(C, element.getNodeIn(), element.getNodeIn(), Cap);
                    addToMatrix(C, element.getNodeOut(), element.getNodeOut(), Cap);
                    addToMatrix(C, element.getNodeOut(), element.getNodeIn(), -Cap);
                    addToMatrix(C, element.getNodeIn(), element.getNodeOut(), -Cap);
                }
            }

            // Inductor Stamp
            if (element.getElementType() == CircuitElement.ElementType.INDUCTOR) {
                double L = element.getValue();
                if (element.getNodeIn() == 0 && element.getNodeOut() == 0) {
                } else if (element.getNodeIn() == 0) {
                    addToMatrix(G, element.getNodeOut(), current_inductor_offset, -1);
                    addToMatrix(G, current_inductor_offset, element.getNodeOut(), -1);
                } else if (element.getNodeOut() == 0) {
                    addToMatrix(G, element.getNodeIn(), current_inductor_offset, 1);
                    addToMatrix(G, current_inductor_offset, element.getNodeIn(), 1);
                } else {
                    addToMatrix(G, element.getNodeIn(), current_inductor_offset, 1);
                    addToMatrix(G, element.getNodeOut(), current_inductor_offset, -1);
                    addToMatrix(G, current_inductor_offset, element.getNodeIn(), 1);
                    addToMatrix(G, current_inductor_offset, element.getNodeOut(), -1);
                }

                addToMatrix(C, current_inductor_offset, current_inductor_offset, -L);
                current_inductor_offset++;
            }

            // Current Source Stamp
            if (element.getElementType() == CircuitElement.ElementType.CURRENT_SOURCE) {
                double I = element.getValue();
                if (element.getNodeIn() == 0 && element.getNodeOut() == 0) {
                } else if (element.getNodeIn() == 0) {
                    addToVector(b, element.getNodeOut(), -I);
                } else if (element.getNodeOut() == 0) {
                    addToVector(b, element.getNodeIn(), I);
                } else {
                    addToVector(b, element.getNodeIn(), I);
                    addToVector(b, element.getNodeOut(), -I);
                }
            }

            // Voltage Source Stamp
            if (element.getElementType() == CircuitElement.ElementType.VOLTAGE_SOURCE) {
                double E = -element.getValue();

                if (element.getNodeIn() == 0 && element.getNodeOut() == 0) {
                } else if (element.getNodeIn() == 0) {
                    addToMatrix(G, element.getNodeOut(), current_voltage_source_offset_offset, -1);
                    addToMatrix(G, current_voltage_source_offset_offset, element.getNodeOut(), -1);
                } else if (element.getNodeOut() == 0) {
                    addToMatrix(G, element.getNodeIn(), current_voltage_source_offset_offset, 1);
                    addToMatrix(G, current_voltage_source_offset_offset, element.getNodeIn(), 1);
                } else {
                    addToMatrix(G, element.getNodeIn(), current_voltage_source_offset_offset, 1);
                    addToMatrix(G, element.getNodeIn(), current_voltage_source_offset_offset, -1);
                    addToMatrix(G, current_voltage_source_offset_offset, element.getNodeIn(), 1);
                    addToMatrix(G, current_voltage_source_offset_offset, element.getNodeIn(), -1);
                }

                addToVector(b, current_voltage_source_offset_offset, E);
                current_voltage_source_offset_offset++;
            }
        }

        nodeNumber = num_nodes;
    }

    private static void addToVector(double[] b, int i, double k) {
        if (i < 1) return;
        b[i - 1] += k;
    }

    private static void addToMatrix(Matrix M, int i, int j, double k) {
        if (i < 1 || j < 1) return;
        M.setElement(i - 1, j - 1, M.getElement(i - 1, j - 1) + k);
    }

    public String toNetlist() {
        StringBuilder netlist = new StringBuilder();
        String name="";
        int voltage_counter=0;
        int current_counter=0;
        int resistor_counter=0;
        int capacitor_counter=0;
        int inductor_counter=0;

        for (int i=0; i<this.components.size();i++){
            if (components.get(i).getElementType()== CircuitElement.ElementType.VOLTAGE_SOURCE){
                voltage_counter++;
                name="V"+ voltage_counter;

            }
            if (components.get(i).getElementType()== CircuitElement.ElementType.CURRENT_SOURCE){
                current_counter++;
                name="C"+ current_counter;

            }
            if (components.get(i).getElementType()== CircuitElement.ElementType.CAPACITOR){
                capacitor_counter++;
                name="C"+ capacitor_counter;

            }
            if (components.get(i).getElementType()== CircuitElement.ElementType.RESISTOR){
                resistor_counter++;
                name="R"+ resistor_counter;

            }
            if (components.get(i).getElementType()== CircuitElement.ElementType.INDUCTOR){
                inductor_counter++;
                name="I"+ inductor_counter;

            }
            netlist
                    .append(name)
                    .append(" ")
                    .append("n")
                    .append(this.components.get(i).getNodeIn())
                    .append(" ")
                    .append("n")
                    .append(this.components.get(i).getNodeOut())
                    .append(" ")
                    .append(this.components.get(i).getValue())
                    .append("\n");
        }
        return netlist.toString();
    }

    public double[] do_DC_simulation() {
        return this.G.solveLinearSet(this.b);
    }


    public ACResult[] do_AC_simulation(double frequency_start, double frequency_range, int num_frequency_steps, boolean doLog) {
        ACResult[] acResults = new ACResult[num_frequency_steps + 1];
        int size = this.G.getNcol();

        for (int step = 0; step <= num_frequency_steps; step++) {
            double f = frequency_start;

            if (doLog) {
                if (step != 0) { // Special case for log so that we evaluate the start frequency as well.
                    f += Math.pow(10, (step - 1) * Math.log10(frequency_range) / (num_frequency_steps - 1));
                }
            } else {
                f += step * frequency_range / num_frequency_steps;
            }

            double omega = 2 * Math.PI * f;

            ComplexMatrix A = new ComplexMatrix(size, size);
            Complex[] B = new Complex[size];

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    A.setElement(i, j, this.G.getElement(i, j), this.C.getElement(i, j) * omega);
                }

                B[i] = new Complex(this.b[i], 0.0);
            }
            try {
                acResults[step] = new ACResult(f, A.solveLinearSet(B));
            } catch (java.lang.ArithmeticException e) {
                acResults[step] = new ACResult(f, new Complex[A.getNcol()]);
                for (int k = 0; k < A.getNcol(); k++) {
                    acResults[step].vector[k] = new Complex(0, 0);
                }
            }
        }

        return acResults;
    }
}
