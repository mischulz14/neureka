package neureka.ndim.config.types.reshaped;

import neureka.ndim.config.types.D1C;

public class Reshaped1DConfiguration extends D1C {
    /**
     *  The shape of the NDArray.
     */
    protected final int _shape;
    /**
     *  The translation from a shape index (indices) to the index of the underlying data array.
     */
    private final int _translation;
    /**
     *  The mapping of the indices array.
     */
    private final int _indicesMap; // Maps index integer to array like translation. Used to avoid distortion when slicing!

    public static Reshaped1DConfiguration construct(
            int[] shape,
            int[] translation,
            int[] indicesMap
    ) {
        return _cached( new Reshaped1DConfiguration(shape[ 0 ], translation[ 0 ],  indicesMap[ 0 ]) );
    }

    protected Reshaped1DConfiguration(
            int shape,
            int translation,
            int indicesMap
    ) {
        _shape = shape;
        _translation = translation;
        _indicesMap = indicesMap;
        assert translation != 0;
        assert indicesMap != 0;
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public int[] shape() {
        return new int[]{_shape};
    }

    @Override
    public int shape( int i ) {
        return _shape;
    }

    @Override
    public int[] indicesMap() {
        return new int[]{_indicesMap};
    }

    @Override
    public int indicesMap(int i ) {
        return _indicesMap;
    }

    @Override
    public int[] translation() {
        return new int[]{_translation};
    }

    @Override
    public int translation( int i ) {
        return _translation;
    }

    @Override
    public int[] spread() {
        return new int[]{1};
    }

    @Override
    public int spread( int i ) {
        return 1;
    }

    @Override
    public int[] offset() {
        return new int[]{0};
    }

    @Override
    public int offset( int i ) {
        return 0;
    }

    @Override
    public int indexOfIndex(int index) {
        return (index / _indicesMap) * _translation;
    }

    @Override
    public int[] indicesOfIndex(int index) {
        return new int[]{index / _indicesMap};
    }

    @Override
    public int indexOfIndices(int[] indices) {
        return indices[ 0 ] * _translation;
    }

    @Override
    public int indexOfIndices(int d1 ) {
        return d1 * _translation;
    }

}
