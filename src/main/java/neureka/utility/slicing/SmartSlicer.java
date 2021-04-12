package neureka.utility.slicing;

import neureka.Tsr;
import neureka.framing.IndexAlias;
import neureka.utility.slicing.states.AxisOrGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *  This class is responsible for receiving any input and trying to interpret it so that a
 *  slice can be formed.
 */
public class SmartSlicer {

    /**
     *  An interface provided by sl4j which enables a modular logging backend!
     */
    private static Logger _LOG = LoggerFactory.getLogger(SmartSlicer.class); // Why is this not final ? : For unit testing!

    public static <ValType> Tsr<ValType> slice(
            Object[] ranges,
            Tsr<ValType> source,
            SliceBuilder.CreationCallback<ValType> callback
    ) {
        AxisOrGet<ValType> sliceBuilder = new SliceBuilder<>(source, callback);
        List<Object> rangeList = new ArrayList<>();
        List<Integer> stepsList = new ArrayList<>();
        for (Object range : ranges ) {
            if ( range instanceof Map) {
                rangeList.addAll(((Map<?, ?>) range).keySet());
                stepsList.addAll(((Map<?, Integer>) range).values());
            }
            else if ( range instanceof int[] ) {
                List<Integer> intList = new ArrayList<>(((int[]) range).length);
                for ( int ii : (int[]) range ) intList.add(ii);
                rangeList.add(intList);
                stepsList.add(1);
            }
            else if ( range instanceof String[] ) {
                List<String> strList = new ArrayList<>(((String[]) range).length);
                strList.addAll(Arrays.asList((String[]) range));
                rangeList.add(strList);
                stepsList.add(1);
            }
            else {
                rangeList.add( range );
                stepsList.add(1);
            }
        }

        ranges = rangeList.toArray();

        for ( int i = 0; i < ranges.length; i++ ) {
            int first = 0;
            int last = 0;
            if ( !( ranges[ i ] instanceof  List ) ) {
                if ( ranges[ i ] instanceof Integer ) {
                    first = (Integer) ranges[ i ];
                    last = (Integer) ranges[ i ];
                } else {
                    IndexAlias<?> indexAlias = source.find( IndexAlias.class );
                    if ( indexAlias != null ) {
                        int position = indexAlias.get( ranges[ i ], i );
                        first = position;
                        last = position;
                    } else {
                        String message = "Given "+IndexAlias.class.getSimpleName()+" key at axis " + ( i ) + " not found!";
                        _LOG.error( message );
                        throw new IllegalStateException( message );
                    }
                }
            } else {
                ranges[ i ] = ( (List<?>) ranges[ i ] ).toArray();
                ranges[ i ] = ( ( (Object[]) ranges[ i ] )[ 0 ] instanceof List )
                        ? ( (List<?>) ( (Object[]) ranges[ i ] )[ 0 ] ).toArray()
                        : ( (Object[]) ranges[ i ] );
                if (
                        !( ( (Object[]) ( ranges[ i ] ) )[ 0 ] instanceof Integer )
                                || !( ( (Object[]) ranges[ i ] )[ ( (Object[]) ( ranges[ i ] ) ).length - 1 ] instanceof Integer )
                ) {
                    IndexAlias<?> indexAlias = source.find( IndexAlias.class );
                    if ( !( ( (Object[]) (ranges[ i ]) )[ 0 ] instanceof Integer ) ) {
                        if ( indexAlias != null ) {
                            first = indexAlias.get( ( (Object[]) ranges[ i ])[ 0 ], i );
                        }
                    }
                    else first = (Integer) ( (Object[]) ranges[ i ] )[ 0 ];

                    if ( !( ( (Object[]) ranges[ i ] )[ ( (Object[]) ranges[ i ] ).length - 1 ] instanceof Integer )  ) {
                        if ( indexAlias != null ) {
                            last = indexAlias.get(
                                    ( (Object[]) ranges[ i ] )[ ( (Object[]) ranges[ i ] ).length - 1 ],
                                    i
                            );
                        }
                    }
                    else last = (Integer) ( (Object[]) ranges[ i ] )[ ( (Object[]) ranges[ i ] ).length - 1 ];

                } else {
                    first = (Integer)( (Object[]) ranges[ i ] )[ 0 ];
                    last = (Integer) ( (Object[]) ranges[ i ] )[ ( (Object[]) ranges[ i ] ).length - 1 ];
                }
            }

            sliceBuilder =
                    sliceBuilder
                        .axis( i )
                        .from( first )
                        .to( last )
                        .step( stepsList.get( i ) );

        }
        return sliceBuilder.get();
    }

}
