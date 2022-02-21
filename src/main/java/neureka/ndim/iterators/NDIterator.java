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

   _   _ _____ _____ _                 _
  | \ | |  __ \_   _| |               | |
  |  \| | |  | || | | |_ ___ _ __ __ _| |_ ___  _ __
  | . ` | |  | || | | __/ _ \ '__/ _` | __/ _ \| '__|
  | |\  | |__| || |_| ||  __/ | | (_| | || (_) | |
  |_| \_|_____/_____|\__\___|_|  \__,_|\__\___/|_|


*/


package neureka.ndim.iterators;

import neureka.Tsr;
import neureka.ndim.config.NDConfiguration;
import neureka.ndim.config.types.reshaped.Reshaped2DConfiguration;
import neureka.ndim.config.types.reshaped.Reshaped3DConfiguration;
import neureka.ndim.config.types.simple.Simple1DConfiguration;
import neureka.ndim.config.types.simple.Simple2DConfiguration;
import neureka.ndim.config.types.simple.Simple3DConfiguration;
import neureka.ndim.config.types.sliced.Sliced1DConfiguration;
import neureka.ndim.config.types.sliced.Sliced2DConfiguration;
import neureka.ndim.config.types.sliced.Sliced3DConfiguration;
import neureka.ndim.config.types.virtual.VirtualNDConfiguration;
import neureka.ndim.iterators.types.reshaped.Reshaped2DCIterator;
import neureka.ndim.iterators.types.reshaped.Reshaped3DCIterator;
import neureka.ndim.iterators.types.sliced.SlicedNDIterator;
import neureka.ndim.iterators.types.virtual.VirtualNDIterator;
import neureka.ndim.iterators.types.sliced.Sliced1DCIterator;
import neureka.ndim.iterators.types.sliced.Sliced2DCIterator;
import neureka.ndim.iterators.types.sliced.Sliced3DCIterator;
import neureka.ndim.iterators.types.simple.Simple1DCIterator;
import neureka.ndim.iterators.types.simple.Simple2DCIterator;
import neureka.ndim.iterators.types.simple.Simple3DCIterator;

import java.util.StringJoiner;
import java.util.stream.IntStream;

public interface NDIterator
{
    enum NonVirtual { TRUE, FALSE }

    static NDIterator of ( Tsr<?> t ) {
        return of( t, NonVirtual.FALSE );
    }

    static NDIterator of( Tsr<?> t, NonVirtual shouldNotBeVirtual ) {
        return of( t.getNDConf(), shouldNotBeVirtual );
    }

    static NDIterator of( NDConfiguration ndc, NonVirtual shouldNotBeVirtual ) {

        if ( ndc instanceof Simple1DConfiguration   ) return new Simple1DCIterator(     (Simple1DConfiguration) ndc );
        if ( ndc instanceof Sliced1DConfiguration   ) return new Sliced1DCIterator(     (Sliced1DConfiguration) ndc );

        if ( ndc instanceof Simple2DConfiguration   ) return new Simple2DCIterator(     (Simple2DConfiguration) ndc );
        if ( ndc instanceof Reshaped2DConfiguration ) return new Reshaped2DCIterator( (Reshaped2DConfiguration) ndc );
        if ( ndc instanceof Sliced2DConfiguration   ) return new Sliced2DCIterator(     (Sliced2DConfiguration) ndc );

        if ( ndc instanceof Simple3DConfiguration   ) return new Simple3DCIterator(      (Simple3DConfiguration) ndc );
        if ( ndc instanceof Reshaped3DConfiguration ) return new Reshaped3DCIterator(  (Reshaped3DConfiguration) ndc );
        if ( ndc instanceof Sliced3DConfiguration   ) return new Sliced3DCIterator(      (Sliced3DConfiguration) ndc );

        if ( ndc instanceof VirtualNDConfiguration && shouldNotBeVirtual == NonVirtual.FALSE )
            return new VirtualNDIterator( (VirtualNDConfiguration) ndc );
        else
            return new SlicedNDIterator( ndc );
    }


    int shape( int i );

    int[] shape();

    void increment();

    default int getIndexAndIncrement() {
        int i = i();
        this.increment();
        return i;
    }

    void decrement();

    int i();

    int get( int axis );

    int[] get();

    void set( int axis, int position );

    void set( int[] indices );

    int rank();


    default String asString()
    {
        StringBuilder b = new StringBuilder();

        StringJoiner sj = new StringJoiner( "," );
        StringJoiner finalSj1 = sj;
        IntStream.of( this.shape() ).forEach( x -> finalSj1.add( String.valueOf(x) ) );

        b.append( "S[" + sj.toString() + "];" );
        sj = new StringJoiner( "," );
        StringJoiner finalSj = sj;
        IntStream.of( this.get() ).forEach( x -> finalSj.add( String.valueOf( x ) ) );
        b.append( "I[" + sj.toString() + "];" );
        return b.toString();
    }


}
