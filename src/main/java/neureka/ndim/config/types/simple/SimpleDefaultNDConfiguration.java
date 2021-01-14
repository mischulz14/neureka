package neureka.ndim.config.types.simple;

import neureka.Neureka;
import neureka.ndim.config.NDConfiguration;
import neureka.ndim.config.AbstractNDC;

import java.util.Arrays;

public final class SimpleDefaultNDConfiguration extends AbstractNDC //:= IMMUTABLE
{

    /**
     *  The shape of the NDArray.
     */
    protected final int[] _shape;
    /**
     *  The translation from a shape index (idx) to the index of the underlying data array.
     */
    private final int[] _translation_and_idxmap;


    protected SimpleDefaultNDConfiguration(
            int[] shape, int[] translation
    ) {
        _shape = _cacheArray( shape );
        _translation_and_idxmap = _cacheArray( translation );
    }

    public static NDConfiguration construct(
            int[] shape,
            int[] translation
    ) {
        return _cached(new SimpleDefaultNDConfiguration(shape, translation));
    }

    @Override
    public int rank() {
        return _shape.length;
    }

    @Override
    public int[] shape() {
        return _shape;
    }

    @Override
    public int shape( int i ) {
        return _shape[ i ];
    }

    @Override
    public int[] idxmap() {
        return _translation_and_idxmap;
    }

    @Override
    public int idxmap( int i ) {
        return _translation_and_idxmap[ i ];
    }

    @Override
    public int[] translation() {
        return _translation_and_idxmap;
    }

    @Override
    public int translation( int i ) {
        return _translation_and_idxmap[ i ];
    }

    @Override
    public int[] spread() {
        int[] newSpred = new int[ _shape.length ];
        Arrays.fill(newSpred, 1);
        return newSpred;
    }

    @Override
    public int spread( int i ) {
        return 1;
    }

    @Override
    public int[] offset() {
        return new int[ _shape.length ];
    }

    @Override
    public int offset( int i ) {
        return 0;
    }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public int i_of_i( int i ) {
        return i_of_idx(idx_of_i( i ));
    }

    @Override
    public int[] idx_of_i( int i ) {
        int[] idx = new int[ _shape.length ];
        for ( int ii=0; ii<rank(); ii++ ) {
            idx[ ii ] += i / _translation_and_idxmap[ ii ];
            i %= _translation_and_idxmap[ ii ];
        }
        return idx;
    }

    @Override
    public int i_of_idx( int[] idx ) {
        int i = 0;
        for ( int ii=0; ii<_shape.length; ii++ ) i += idx[ ii ] * _translation_and_idxmap[ ii ];
        return i;
    }


}
