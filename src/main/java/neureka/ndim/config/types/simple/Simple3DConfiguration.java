package neureka.ndim.config.types.simple;

import neureka.ndim.config.types.D3C;

public class Simple3DConfiguration extends D3C //:= IMMUTABLE
{
    /**
     *  The shape of the NDArray.
     */
    protected final int _shape1;
    protected final int _shape2;
    protected final int _shape3;
    /**
     *  The translation from a shape index (indices) to the index of the underlying data array.
     */
    private final int _translation1;
    private final int _translation2;
    private final int _translation3;

    protected Simple3DConfiguration(
            int[] shape,
            int[] translation
    ) {
        _shape1 = shape[ 0 ];
        _shape2 = shape[ 1 ];
        _shape3 = shape[ 2 ];
        _translation1 = translation[ 0 ];
        _translation2 = translation[ 1 ];
        _translation3 = translation[ 2 ];
    }

    public static Simple3DConfiguration construct(
            int[] shape,
            int[] translation
    ) {
        return _cached( new Simple3DConfiguration(shape, translation) );
    }

    /** {@inheritDoc} */
    @Override public final int rank() { return 3; }

    /** {@inheritDoc} */
    @Override public final int[] shape() { return new int[]{_shape1, _shape2, _shape3}; }

    /** {@inheritDoc} */
    @Override public final int shape( int i ) { return (i==0 ?_shape1 : (i==1 ? _shape2 : _shape3)); }

    /** {@inheritDoc} */
    @Override public final int[] indicesMap() { return new int[]{_translation1, _translation2, _translation3}; }

    /** {@inheritDoc} */
    @Override public final int indicesMap(int i ) { return (i==0?_translation1:(i==1?_translation2:_translation3)); }

    /** {@inheritDoc} */
    @Override public final int[] translation() { return new int[]{_translation1, _translation2, _translation3}; }

    /** {@inheritDoc} */
    @Override public final int translation( int i ) { return (i==0?_translation1:(i==1?_translation2:_translation3)); }

    /** {@inheritDoc} */
    @Override public final int[] spread() { return new int[]{1, 1, 1}; }

    /** {@inheritDoc} */
    @Override public final int spread( int i ) { return 1; }

    /** {@inheritDoc} */
    @Override public final int[] offset() { return new int[]{0, 0, 0}; }

    /** {@inheritDoc} */
    @Override public final int offset( int i ) { return 0; }

    /** {@inheritDoc} */
    @Override public final int indexOfIndex( int index ) {
        int indices1, indices2, indices3;
        indices1 = index / _translation1;
        index %= _translation1;
        indices2 = index / _translation2;
        index %= _translation2;
        indices3 = index / _translation3;
        return indices1 * _translation1 +
                indices2 * _translation2 +
                indices3 * _translation3;
    }

    /** {@inheritDoc} */
    @Override public final int[] indicesOfIndex( int index ) {
        int indices1, indices2, indices3;
        indices1 = index / _translation1;
        index %= _translation1;
        indices2 = index / _translation2;
        index %= _translation2;
        indices3 = index / _translation3;
        return new int[]{ indices1, indices2, indices3 };
    }

    /** {@inheritDoc} */
    @Override public final int indexOfIndices(int[] indices) {
        return indices[ 0 ] * _translation1 +
                indices[ 1 ] * _translation2 +
                indices[ 2 ] * _translation3;
    }

    /** {@inheritDoc} */
    @Override public final int indexOfIndices( int d1, int d2, int d3 ) {
        return d1 * _translation1 +
                d2 * _translation2 +
                d3 * _translation3;
    }
}