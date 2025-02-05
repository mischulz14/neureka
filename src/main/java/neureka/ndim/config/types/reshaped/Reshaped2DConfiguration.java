package neureka.ndim.config.types.reshaped;

import neureka.ndim.config.types.D2C;

public class Reshaped2DConfiguration extends D2C
{
    /**
     *  The shape of the NDArray.
     */
    protected final int _shape1;
    protected final int _shape2;
    /**
     *  The translation from a shape index (indices) to the index of the underlying data array.
     */
    private final int _translation1;
    private final int _translation2;
    /**
     *  The mapping for the indices array.
     */
    private final int _indicesMap1;
    private final int _indicesMap2; // Maps index integer to array like translation. Used to avoid distortion when slicing!


    protected Reshaped2DConfiguration(
            int[] shape,
            int[] translation,
            int[] indicesMap
    ) {
        _shape1 = shape[ 0 ];
        _shape2 = shape[ 1 ];
        _translation1 = translation[ 0 ];
        _translation2 = translation[ 1 ];
        _indicesMap1 = indicesMap[ 0 ];
        _indicesMap2 = indicesMap[ 1 ];
    }

    public static Reshaped2DConfiguration construct(
            int[] shape,
            int[] translation,
            int[] indicesMap
    ) {
        return _cached( new Reshaped2DConfiguration(shape, translation, indicesMap) );
    }

    /** {@inheritDoc} */
    @Override public final int rank() { return 2; }

    /** {@inheritDoc} */
    @Override public final int[] shape() { return new int[]{_shape1, _shape2}; }

    /** {@inheritDoc} */
    @Override public final int shape( int i ) { return ( i==0 ? _shape1 : _shape2 ); }

    /** {@inheritDoc} */
    @Override public final int[] indicesMap() { return new int[]{_indicesMap1, _indicesMap2}; }

    /** {@inheritDoc} */
    @Override public final int indicesMap( int i ) { return ( i==0 ? _indicesMap1 : _indicesMap2 ); }

    /** {@inheritDoc} */
    @Override public final int[] translation() { return new int[]{_translation1, _translation2}; }

    /** {@inheritDoc} */
    @Override public final int translation( int i ) { return ( i==0 ? _translation1 : _translation2 ); }

    /** {@inheritDoc} */
    @Override public final int[] spread() { return new int[]{1, 1}; }

    /** {@inheritDoc} */
    @Override public final int spread( int i ) { return 1; }

    /** {@inheritDoc} */
    @Override public final int[] offset() { return new int[]{0, 0}; }

    /** {@inheritDoc} */
    @Override public final int offset( int i ) { return 0; }

    /** {@inheritDoc} */
    @Override public final int indexOfIndex( int index ) {
        return (index / _indicesMap1) * _translation1 +
                ((index %_indicesMap1) / _indicesMap2) * _translation2;
    }

    /** {@inheritDoc} */
    @Override public final int[] indicesOfIndex( int index ) {
        int[] indices = new int[ 2 ];
        indices[ 0 ] += index / _indicesMap1;
        index %= _indicesMap1;
        indices[ 1 ] += index / _indicesMap2;
        return indices;
    }

    /** {@inheritDoc} */
    @Override public final int indexOfIndices( int[] indices ) {
        int i = 0;
        i += indices[ 0 ]* _translation1;
        i += indices[ 1 ]* _translation2;
        return i;
    }

    /** {@inheritDoc} */
    @Override public final int indexOfIndices( int d1, int d2 ) {
        int i = 0;
        i += d1 * _translation1;
        i += d2 * _translation2;
        return i;
    }
}
