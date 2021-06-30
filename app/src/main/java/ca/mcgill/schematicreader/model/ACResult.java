package ca.mcgill.schematicreader.model;

import flanagan.complex.Complex;

/* Stores the result of an AC simulation at a specific frequency */
public class ACResult {
    public double frequency;
    public Complex[] vector;

    public ACResult(double frequency, Complex[] vector) {
        this.frequency = frequency;
        this.vector = vector;
    }
}
