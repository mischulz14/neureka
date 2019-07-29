package neureka.core.function.imp;

import neureka.core.T;
import neureka.core.function.TFunction;

public class Variable implements TFunction {
    @Override
    public boolean isFlat() {
        return false;
    }
    @Override
    public int id() {
        return -1;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public TFunction newBuild(final String equation) {
        return (TFunction) this;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public double activate(final double[] input, int j) {
        return input[j];
    }
    @Override
    public double activate(final double[] input) {
        double sum = 0;
        for (int Ii = 0; Ii < input.length; Ii++) {
            sum += activate(input, Ii);
        }
        return sum;
    }
    @Override
    public double derive(final double[] input, final int index) {
        return 1.0;
    }
    @Override
    public double derive(double[] input, int index, int j) {
        if (j != index) {
            return 0;
        }
        return derive(input, index);
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public T activate(T[] input, int j) {
        return input[j];
    }
    @Override
    public T activate(T[] input) {
        return new T(input, "sum(I[j])");
    }
    @Override
    public T derive(T[] input, int index, int j) {
        if (j != index) {
            return T.factory.newTensor(0, input[0].shape());
        }
        return derive(input, index);
    }
    @Override
    public T derive(T[] input, int index) {
        return T.factory.newTensor(1, input[0].shape());
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public String toString() {
        return "I[j]";
    }
}
