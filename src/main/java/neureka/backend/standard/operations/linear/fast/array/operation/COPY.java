/*<#LICENSE#>*/
package neureka.backend.standard.operations.linear.fast.array.operation;

import java.lang.reflect.Array;

/**
 * The ?copy routines perform a vector-vector operation defined as y = x, where x and y are vectors.
 *
 */
public final class COPY {

    public static int THRESHOLD = 128;

    public static double[] copyOf(final double[] original) {
        final int tmpLength = original.length;
        final double[] retVal = new double[tmpLength];
        System.arraycopy(original, 0, retVal, 0, tmpLength);
        return retVal;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(final T[] original) {
        final int tmpLength = original.length;
        final T[] retVal = (T[]) Array.newInstance(original.getClass().getComponentType(), tmpLength);
        System.arraycopy(original, 0, retVal, 0, tmpLength);
        return retVal;
    }

}
