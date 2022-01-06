/*
MIT License

Copyright (c) 2019 Gleethos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   _   _ _____   _____             __ _                       _   _
  | \ | |  __ \ / ____|           / _(_)                     | | (_)
  |  \| | |  | | |     ___  _ __ | |_ _  __ _ _   _ _ __ __ _| |_ _  ___  _ __
  | . ` | |  | | |    / _ \| '_ \|  _| |/ _` | | | | '__/ _` | __| |/ _ \| '_ \
  | |\  | |__| | |___| (_) | | | | | | | (_| | |_| | | | (_| | |_| | (_) | | | |
  |_| \_|_____/ \_____\___/|_| |_|_| |_|\__, |\__,_|_|  \__,_|\__|_|\___/|_| |_|
                                         __/ |
                                        |___/
*/

package neureka.ndim.config;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.stream.IntStream;

public interface NDConfiguration
{
    enum Layout {

        ROW_MAJOR, COLUMN_MAJOR;

        @Contract(pure = true)
        public int[] newTranslationFor( int[] shape ) {
            int[] tln = new int[ shape.length ];
            int prod = 1;
            if ( this == COLUMN_MAJOR ) {
                for ( int i = 0; i < tln.length; i++ ) {
                    tln[ i ] = prod;
                    prod *= shape[ i ];
                }
            } else if ( this == ROW_MAJOR ) {
                for (int i = tln.length - 1; i >= 0; i--) {
                    tln[i] = prod;
                    prod *= shape[i];
                }
            }
            else
                throw new IllegalStateException("Unknown data layout!");

            return tln;
        }

        @Contract(pure = true)
        public int[] rearrange(int[] tln, int[] shape, int[] newForm) {
            int[] shpTln = this.newTranslationFor( shape );
            int[] newTln = new int[ newForm.length ];
            for ( int i = 0; i < newForm.length; i++ ) {
                if ( newForm[ i ] < 0 ) newTln[ i ] = shpTln[ i ];
                else if ( newForm[ i ] >= 0 ) newTln[ i ] = tln[ newForm[ i ] ];
            }
            return newTln;
        }
    }

    /**
     *  The layout of a tensor can either be row major or column major.
     *  Row major means that row elements are right next to one another
     *  in the underlying data array of a tensor.
     *  Column major is the exact opposite...
     *
     * @return The layout of the underlying data array of a tensor.
     */
    default Layout getLayout() {
        return Layout.ROW_MAJOR;
    }

    /**
     *  A slice will have a unique kind of access pattern defined by the ND-Configuration.
     *
     * @return The truth value determining if this ND-Configuration models a slice index pattern.
     */
    default boolean isSlice() {
        boolean noOffsets = Arrays.stream(offset()).allMatch( o -> o == 0 );
        boolean simpleSpread = Arrays.stream(spread()).allMatch( s -> s == 1 );
        return noOffsets && simpleSpread;
    }

    /**
     *  This method returns the number of axis of
     *  an nd-array / {@link neureka.Tsr} which is equal to the
     *  length of the shape of an nd-array / {@link neureka.Tsr}.
     *
     * @return The number of axis of an nd-array.
     */
    int rank();

    /**
     *  This method returns an array of axis sizes.
     *
     * @return An array of axis sizes.
     */
    int[] shape();

    /**
     *  This method receives an axis index and return the
     *  size of the axis.
     *  It enables readable access to the shape
     *  of this configuration.
     *
     * @param i The index of the axis whose size ought to be returned.
     * @return The axis size targeted by the provided index.
     */
    int shape( int i );

    int[] indicesMap();

    int indicesMap( int i );

    int[] translation();

    int translation( int i );

    int[] spread();

    int spread( int i );

    int[] offset();

    int offset( int i );

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Use this to calculate the true index for an element in the data array (data array index)
     *  based on a provided "virtual index", or "value array index".
     *  This virtual index may be different from the true index depending on the type of nd-array,
     *  like for example if the nd-array is
     *  a slice of another larger nd-array, or if it is in fact a reshaped version of another nd-array.
     *  This virtual index ought to be turned into an index array which defines the position for every axis.
     *  Then this indices array will be converted into the final and true index targeting an underlying item.
     *  The information needed for performing this translation is expressed by individual implementations of
     *  this {@link NDConfiguration} interface, which contain everything
     *  needed to treat a given block of data as a nd-array!
     *
     * @param index The virtual index of the tensor having this configuration.
     * @return The true index which targets the actual data within the underlying data array of an nd-array / tensor.
     */
    int indexOfIndex( int index );

    /**
     *  The following method calculates the axis indices for an element in the nd-array array
     *  based on a provided "virtual index".
     *  The resulting index defines the position of the element for every axis.
     *
     * @param index The virtual index of the tensor having this configuration.
     * @return The position of the (virtually) targeted element represented as an array of axis indices.
     */
    int[] indicesOfIndex( int index );

    /**
     * The following method calculates the true index for an element in the data array
     * based on a provided index array.
     *
     * @param indices The indices for every axis of a given nd-array.
     * @return The true index targeting the underlying data array of a given nd-array.
     */
    int indexOfIndices( int[] indices );

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  This method returns an array of flattened arrays which
     *  define this nd-configuration in a compact manner.
     *  The array consists of the following arrays joined
     *  in the following order:
     *  [ shape | translation | idxMap | idx | idxScale | idxBase ]
     *
     * @return An array of flattened arrays which define this nd-configuration in a compact manner.
     */
    default int[] asInlineArray()
    {
        //CONFIG TRANSFER: <[ shape | translation | indicesMap | indices | strides ]>
        int rank = rank();
        int[] inline = new int[ rank * 5 ];
        System.arraycopy( shape(),       0, inline, rank * 0, rank ); // -=> SHAPE COPY
        System.arraycopy( translation(), 0, inline, rank * 1, rank ); // -=> TRANSLATION
        System.arraycopy( indicesMap(),  0, inline, rank * 2, rank ); // -=> INDICES MAP (translates scalarization to dimension index)
        System.arraycopy( offset(),      0, inline, rank * 3, rank ); // -=> SPREAD / STRIDES
        System.arraycopy( spread(),      0, inline, rank * 4, rank ); // -=> OFFSET
        return inline;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    long keyCode();

    boolean equals(NDConfiguration ndc);

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  This method enables reshaping for {@link NDConfiguration} implementation instances.
     *  Because {@link NDConfiguration}s are in essence things which define
     *  the access relationship from shape indices to the actual underlying data,
     *  the creation of reshaped {@link NDConfiguration} is up to a specific implementation.
     *
     * @param newForm An array of indices which define how the axis ought to be rearranged.
     * @return A new {@link NDConfiguration} which carries the needed information for the reshaped view.
     */
    NDConfiguration newReshaped( int[] newForm );

    /**
     *  The boolean returned by this method simply reports
     *  if this configuration is the most basic form of configuration
     *  possible for the given shape represented by this instance.
     *  This type of configuration is the typical for freshly created
     *  tensors which are neither slices or reshaped variants of an
     *  original tensor...
     *
     * @return The truth value determining if this configuration is not modeling more complex indices like reshaped views or slices...
     */
    default boolean isSimple() {
        int[] simpleTranslation = this.getLayout().newTranslationFor( this.shape() );
        return Arrays.equals(this.translation(), simpleTranslation)
                &&
               IntStream.range(0, this.rank()).allMatch( i -> this.spread(i) == 1 )
                &&
               Arrays.equals(this.indicesMap(), simpleTranslation )
                &&
               IntStream.range(0, this.rank()).allMatch( i -> this.offset(i) == 0 );
    }

    default IndexToIndexFunction getIndexToIndexAccessPattern() {
        NDConfiguration nda = this;
        return (nda::indexOfIndex);
    }

    interface IndexToIndexFunction { int map(int i); }

    /**
     *  This utility class provides static methods which are helpful
     *  for nd-configuration related operations like reshaping,
     *  incrementing or decrementing index arrays...
     */
    class Utility
    {
        @Contract(pure = true)
        public static int[] rearrange(int[] array, int[] ptr) {
            int[] newShp = new int[ptr.length];
            for ( int i = 0; i < ptr.length; i++ ) {
                if (ptr[ i ] < 0) newShp[ i ] = Math.abs(ptr[ i ]);
                else if (ptr[ i ] >= 0) newShp[ i ] = array[ptr[ i ]];
            }
            return newShp;
        }


        @Contract(pure = true)
        public static void increment( int[] indices, int[] shape ) {
            int i = shape.length-1;
            while ( i >= 0 && i < shape.length ) i = _incrementAt( i, indices, shape );
        }

        @Contract(pure = true)
        private static int _incrementAt( int i, int[] indices, int[] shape )
        {
            if ( indices[ i ] < shape[ i ] ) {
                indices[ i ]++;
                if ( indices[ i ] == shape[ i ] ) {
                    indices[ i ] = 0;
                    i--;
                }
                else i = -1;
            }
            else i--;
            return i;
        }

        @Contract(pure = true)
        public static int szeOfShp( int[] shape ) {
            int size = 1;
            for ( int i : shape ) size *= i;
            return size;
        }


    }

}
