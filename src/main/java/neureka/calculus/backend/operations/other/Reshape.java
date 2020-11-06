package neureka.calculus.backend.operations.other;

import neureka.Tsr;
import neureka.devices.Device;
import neureka.autograd.DefaultADAgent;
import neureka.calculus.Function;
import neureka.calculus.backend.operations.AbstractOperationType;
import neureka.calculus.backend.ExecutionCall;
import neureka.calculus.backend.implementations.functional.GenericImplementation;
import neureka.calculus.frontend.assembly.FunctionBuilder;
import neureka.framing.Relation;
import neureka.ndim.AbstractNDArray;
import neureka.ndim.config.NDConfiguration;

import java.util.ArrayList;
import java.util.List;

public class Reshape extends AbstractOperationType
{

    public Reshape()
    {

        super(
                "reshape", ",", -1,
                true,
                false,
                true,
                false
        );

        setStringifier(
            children ->
            {
                java.util.function.Function<String, Boolean> isConstantNumeric =
                s ->
                {
                    try {
                        Double.parseDouble(s);
                        return true;
                    } catch (Exception e) { return false; }
                };
                StringBuilder reconstructed = new StringBuilder();
                reconstructed.insert(0, "[");
                for ( int i = 0; i < children.size(); ++i ) {
                    if ( i == children.size() - 1 ) {
                        reconstructed.append("]:(").append(
                                ( isConstantNumeric.apply( children.get( i ) ) )
                                        ? children.get( i ).split("\\.")[ 0 ]
                                        : children.get( i )
                        ).append(")");
                    } else {
                        reconstructed.append(
                                ( isConstantNumeric.apply( children.get( i ) ) )
                                        ? children.get( i ).split("\\.")[ 0 ]
                                        : children.get( i )
                        );
                    }
                    if ( i < children.size() - 2 ) {
                        reconstructed.append(",");
                    }
                }
                return "(" + reconstructed + ")";
            }
        );

        GenericImplementation implementation = new GenericImplementation( "reshape" )
                .setSuitabilityChecker( call -> 1.0f )
                .setBackwardADAnalyzer( call -> true )
                .setForwardADAnalyzer(call -> false )
                .setADAgentSupplier(
                    ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                    {
                        //Tsr ctxDerivative = (Tsr)call.getAt("derivative");
                        if ( forward ) {
                            throw new IllegalArgumentException("Reshape operation does not support forward-AD!");
                        }
                        return new DefaultADAgent( null )
                                .withForward( ( t, derivative ) -> FunctionBuilder.build( f.toString(), false ).derive( new Tsr[]{ derivative },0 ) )
                                .withBackward( ( t, error ) -> FunctionBuilder.build( f.toString(), false ).derive( new Tsr[]{ error },0 ) );
                    }
                ).setCallHock(
                    ( caller, call ) ->
                    {
                        Tsr<?>[] inputs = caller.srcActivation( call.getTensors(), call.getJ(), -1, 0 );
                        int[] newForm = new int[ inputs.length - 1 ];
                        for ( int i = 0; i < inputs.length - 1; i++ ) {
                            newForm[ i ] = (int) Tsr.IO.getFrom( inputs[ i ], 0 );
                        }
                        if ( call.getDerivativeIndex() >= 0 ) {//reverse reshape:
                            newForm = invert( newForm );
                        }
                        Tsr<?> t = inputs[ inputs.length - 1 ];
                        return reshaped( t, newForm, true );
                    }
                )
                .setRJAgent( ( call, goDeeperWith ) -> null )
                .setDrainInstantiation( call -> call);

        setImplementation(
                GenericImplementation.class,
                implementation
        );

    }


    public static Tsr reshaped( Tsr tensor, int[] newForm, boolean newTsr )
    {
        Tsr parent = tensor;
        tensor = (newTsr) ? (Tsr) tensor.getAt(new ArrayList<>()) : tensor;
        NDConfiguration newNDC = tensor.getNDConf().newReshaped( newForm );
        AbstractNDArray.Utility.Indexing.shpCheck( newNDC.shape(), tensor );
        tensor.setNDConf( newNDC );
        if ( newTsr ) {
            Relation r = (Relation) parent.find(Relation.class);
            r.addReshapeRelationFor( tensor, newForm );
        }
        return tensor;
    }

    public static int[] invert( int[] reshape ) {
        int reverseLength = 0;
        for ( int e : reshape ) {
            if ( e >= 0 ) reverseLength++;
        }
        int[] reversed = new int[reverseLength];
        int reshape_i = 0;
        int reverse_i = 0;
        while ( reverse_i < reverseLength ) {
            if ( reshape[ reshape_i ] >= 0 ) {
                reversed[ reshape[ reshape_i ] ] = reshape_i;
                reverse_i++;
            }
            reshape_i++;
        }
        return reversed;
    }

    @Override
    public double calculate( double[] inputs, int j, int d, List<Function> src ) {
            return src.get( 0 ).call( inputs, j );
    }
}
