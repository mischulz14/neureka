package neureka.utility;

import lombok.Getter;
import lombok.experimental.Accessors;
import neureka.Neureka;
import neureka.Tsr;
import neureka.autograd.GraphNode;
import neureka.ndim.AbstractNDArray;
import neureka.ndim.config.NDConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

@Accessors( prefix = {"_"} )
public class TsrAsString
{
    public enum Should {
        BE_FORMATTED,
        HAVE_PADDING_OF,
        BE_COMPACT,
        HAVE_GRADIENT,
        BE_SHORTENED_BY,
        HAVE_VALUE,
        HAVE_SHAPE,
        HAVE_DERIVATIVES,
        HAVE_RECURSIVE_GRAPH,
    }

    @Getter private int _padding = 6;
    @Getter private int _shortage = 50;
    @Getter private boolean _hasGradient = true;
    @Getter private boolean _isCompact = true;
    @Getter private boolean _isFormatted = true;
    @Getter private boolean _hasValue = true;
    @Getter private boolean _hasShape = true;
    @Getter private boolean _hasRecursiveGraph = false;
    @Getter private boolean _hasDerivatives = false;

    private Tsr<?> _tensor;
    private StringBuilder _asStr;

    private Map<Should, Object> _config;

    public TsrAsString( Tsr<?> tensor, Map< Should, Object > settings ) {
        Map< Should, Object > copy = _defaults( "" );
        for ( Should s : Should.values() )
            if ( settings.containsKey( s ) ) copy.put( s, settings.get( s ) );
        _construct( tensor, copy );
    }

    public TsrAsString( Tsr<?> tensor, String modes ) {
        _construct(
                tensor,
                _defaults( modes )
        );
    }

    private void _construct( Tsr tensor, Map< Should, Object > settings )
    {
        _config = settings;
        _tensor = tensor;
        // TODO: Add some asserts!
        if ( settings.containsKey( Should.BE_COMPACT ) )
            _isCompact = (boolean) settings.get( Should.BE_COMPACT );

        if ( settings.containsKey( Should.BE_SHORTENED_BY ) )
            _shortage = (int) settings.get( Should.BE_SHORTENED_BY );

        if ( settings.containsKey( Should.HAVE_PADDING_OF ) )
            _padding = (int) settings.get( Should.HAVE_PADDING_OF );

        if ( settings.containsKey( Should.HAVE_GRADIENT ) )
            _hasGradient = (boolean) settings.get( Should.HAVE_GRADIENT );

        if ( settings.containsKey( Should.BE_FORMATTED ) )
            _isFormatted = (boolean) settings.get( Should.BE_FORMATTED );

        if ( settings.containsKey( Should.HAVE_VALUE ) )
            _hasValue = (boolean) settings.get( Should.HAVE_VALUE );

        if ( settings.containsKey( Should.HAVE_SHAPE ) )
            _hasShape = (boolean) settings.get( Should.HAVE_SHAPE );

        if ( settings.containsKey( Should.HAVE_RECURSIVE_GRAPH) )
            _hasRecursiveGraph = (boolean) settings.get( Should.HAVE_RECURSIVE_GRAPH);

        if ( settings.containsKey( Should.HAVE_DERIVATIVES) )
            _hasDerivatives = (boolean) settings.get( Should.HAVE_DERIVATIVES);
    }

    private Map< Should, Object > _defaults( String modes ) {
        Map< Should, Object > copy = new HashMap<>();
        copy.put( Should.BE_SHORTENED_BY,      (modes.contains( "s") ) ? 3 : 50                     );
        copy.put( Should.BE_COMPACT,           modes.contains( "c" )                                );
        copy.put( Should.BE_FORMATTED,         modes.contains( "f" )                                );
        copy.put( Should.HAVE_GRADIENT,        modes.contains( "g" )                                );
        copy.put( Should.HAVE_PADDING_OF,     (modes.contains( "p" )) ? 6 : -1                      );
        copy.put( Should.HAVE_VALUE,          !(modes.contains( "shp" ) || modes.contains("shape")) );
        copy.put( Should.HAVE_RECURSIVE_GRAPH, modes.contains( "r" )                                );
        copy.put( Should.HAVE_DERIVATIVES,     modes.contains( "d" )                                );
        copy.put( Should.HAVE_SHAPE,           !modes.contains( "v" )                               );
        return copy;
    }
    
    private TsrAsString _$( String s ) {
        _asStr.append( s );
        return this;
    }

    private TsrAsString _$( int s ) {
        _asStr.append( s );
        return this;
    }

    private IntFunction<String> _createValStringifier() {
        boolean compact = _isCompact;
        int pad = _padding;
        Object v = _tensor.getData();
        IntFunction<String> function;
        if ( v instanceof double[] )
            function = i -> ( compact )
                    ? AbstractNDArray.Utility.Stringify.formatFP( ( (double[]) v )[ i ])
                    : String.valueOf( ( (double[] ) v )[ i ] );
        else if ( v instanceof float[] )
            function = i -> ( compact )
                    ? AbstractNDArray.Utility.Stringify.formatFP( ( (float[]) v )[ i ] )
                    : String.valueOf( ( (float[]) v )[ i ] );
        else if ( v instanceof short[] )
            function = i -> ( compact )
                    ? AbstractNDArray.Utility.Stringify.formatFP( ( (short[]) v )[ i ] )
                    : String.valueOf( ( (short[]) v )[ i ] );
        else if ( v instanceof int[] )
            function = i -> ( compact )
                    ? AbstractNDArray.Utility.Stringify.formatFP( ( (int[]) v )[ i ] )
                    : String.valueOf( ( (int[]) v )[ i ] );
        else if ( v == null )
            function = i -> ( compact )
                    ? AbstractNDArray.Utility.Stringify.formatFP( _tensor.value64( i ) )
                    : String.valueOf( _tensor.value64( i ) );
        else
            function = i -> String.valueOf( ( (Object[]) v )[ i ] );

        if ( pad < 3 )
            return function;
        else return i -> {
            String s = function.apply( i );
            int margin = pad - s.length();
            int right = ( margin % 2 == 0 ) ? margin / 2 : ( margin-1 ) / 2;
            if ( margin > 0 ) s = Util.pad( margin - right, Util.pad( s, right ) );
            return s;
        };
    }

    public String toString( String deep )
    {
        if ( _tensor.isEmpty() ) return "empty";
        else if ( _tensor.isUndefined() ) return "undefined";
        _asStr = new StringBuilder();
        boolean legacy = Neureka.instance().settings().view().isUsingLegacyView();
        String base = ( deep == null ) ? "" : "\n" + deep;
        String delimiter = ( deep == null ) ? "" : "    ";
        String half = ( deep == null ) ? "" : "  ";
        String deeper = ( deep == null ) ? deep : deep + delimiter;
        if ( _hasShape ) _strShape();
        if ( !_hasValue ) return _asStr.toString();
        _$( ":" );
        if ( _isFormatted )
            _format( _tensor.getNDConf().shape(), new int[ _tensor.rank() ], -1 );
        else {
            if ( legacy ) _$( "(" );
            else _$( "[" );
            _stringifyAllValues();
            if ( legacy ) _$( ")" );
            else _$( "]" );
        }

        if ( _hasGradient && ( _tensor.rqsGradient() || _tensor.hasGradient() ) ) {
            _$( ":g" );
            Tsr<?> gradient = _tensor.find( Tsr.class );
            if ( gradient != null )
                _$( gradient.toString( "cv" ) );
            else
                _$( ( ( legacy ) ? ":(null)" : ":[null]" ) );
        }
        if ( _hasRecursiveGraph && _tensor.has( GraphNode.class ) && _tensor.find( GraphNode.class ).size() > 0 ) {
            GraphNode<?> node = _tensor.find( GraphNode.class );
            _$( "; " );
            node.forEachDerivative( ( t, agent ) -> {
                if ( agent.derivative() == null ) _$( "->d(null), " );
                else {
                    _$(
                                    base + "=>d|[ " +
                                    base + delimiter + agent.derivative().toString( _config, deeper ) + " " +
                                    base + half + "]|:t{ " +
                                    base + delimiter + (
                                    ( t.getPayload() != null ) ? t.getPayload().toString( _config, deeper ) : t.toString("")
                            ) + " " + base + half + "}, "
                    );
                }
            });
        }
        if ( _hasDerivatives && _tensor.has( GraphNode.class ) && _tensor.find( GraphNode.class ).size() > 0 ) {
            GraphNode<?> node = _tensor.find( GraphNode.class );
            if ( node.getMode() != 0 ) {
                _$( "; " );
                node.forEachDerivative( ( t, agent ) -> {
                    if ( agent.derivative() == null ) _$( "->d(" )._$(agent.toString())._$("), ");
                    else _$("->d")._$(agent.derivative().toString(_config, deeper))._$(", ");
                });
            }
        }
        return _asStr.toString();
    }

    private void _stringifyAllValues()
    {
        int max = _shortage;
        IntFunction<String> getter = _createValStringifier();
        int size = _tensor.size();
        int trim = ( size - max );
        size = ( trim > 0 ) ? max : size;
        for ( int i = 0; i < size; i++ ) {
            String vStr = getter.apply( ( _tensor.isVirtual() ) ? 0 : _tensor.i_of_i( i ) );
            _$( vStr );
            if ( i < size - 1 ) _$( ", " );
            else if ( trim > 0 ) _$( ", ... + " )._$( trim )._$( " more" );
        }
    }

    //::: formatted...

    private void _stringifyValueAt(
            int max,
            int[] idx,
            int size
    ) {
        int[] shape = _tensor.getNDConf().shape();
        IntFunction<String> getter = _createValStringifier();
        int trim = ( size - max );
        trim = Math.max( trim, 0 );
        int trimStart = (size / 2 - trim / 2);
        int trimEnd = (size / 2 + trim / 2);
        assert trimEnd - trimStart == trim;
        for ( int i = 0; i < size; i++ ) {
            if ( i < trimStart || i >= trimEnd ) {
                _$( getter.apply( ( _tensor.isVirtual() ) ? 0 : _tensor.i_of_idx( idx ) ) );
                if ( i < size - 1 ) _$( ", " );
            }
            else if ( i == trimStart )
                _$( "... " )._$( trim )._$( " more ..., " );

            NDConfiguration.Utility.increment( idx, shape );
        }
    }

    // A recursive stringifier for formatted tensors...
    private void _format( int[] shape, int[] idx, int dim )
    {
        boolean legacy = Neureka.instance().settings().indexing().isUsingLegacyIndexing();
        int max = (_shortage * 32 / 50 );
        dim = ( dim < 0 ) ? ( (!legacy) ? 0 : _tensor.rank()-1 ) : dim;
        int depth = ( !legacy ) ? dim : _tensor.rank()-dim-1 ;

        if ( legacy && dim == 0 || !legacy && dim == idx.length - 1 ) {
            _$( Util.indent( depth ) );
            _$( "[ " );
            _stringifyValueAt( max, idx, shape[ dim ] );
            _$( " ]" );
        } else {
            _$( Util.indent( depth ) + "[" );
            _$( "\n" );

            int size = shape[ dim ];
            int trim = ( size - max );
            trim = Math.max(trim, 0);
            int trimStart = (size / 2 - trim / 2);
            int trimEnd = (size / 2 + trim / 2);
            assert trimEnd - trimStart == trim;
            int i = 0;
            do {
                if ( i < trimStart || i >= trimEnd )
                    _format( shape, idx, dim + ( (!legacy) ? 1 : -1 ) );
                else if ( i == trimStart )
                    _$( Util.indent( depth + 1 ) + "... " + trim + " more ...\n" );
                else
                    idx[ dim ] = trimEnd + 1;
                i++;
            }
            while( idx[ dim ] != 0 );

            _$( Util.indent( depth )  + "]");
        }
        int i = dim + ( (legacy) ? 1 : -1 );
        if ( i >= 0 && i < idx.length && idx[ i ] != 0 ) _$( "," );
        _$( "\n" );
    }

    private void _strShape() {
        boolean legacy = Neureka.instance().settings().view().isUsingLegacyView();
        _$( (legacy) ? "[" : "(" );
        int[] shape = _tensor.getNDConf().shape();
        for ( int i = 0; i < shape.length; i++ ) {
            _$( shape[ i ] );
            if ( i < shape.length - 1 ) _$( "x" );
        }
        if ( legacy ) _$( "]" );
        else _$( ")" );
    }

    static class Util
    {
        public static String indent( int n ){
            return String.join("", Collections.nCopies( n, "   " ));
        }

        public static String pad( int left, String s ) {
            return String.join("", Collections.nCopies( left, " " )) + s;
        }

        public static String pad( String s, int right ) {
            return s + String.join("", Collections.nCopies( right, " " ));
        }

    }

}
