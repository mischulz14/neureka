package neureka.backend.standard.operations.function;

import neureka.Neureka;
import neureka.Tsr;
import neureka.backend.standard.algorithms.Activation;
import neureka.backend.api.operations.AbstractOperation;
import neureka.devices.Device;
import neureka.backend.standard.implementations.HostImplementation;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.calculus.Function;
import neureka.backend.api.ExecutionCall;
import neureka.devices.host.HostCPU;
import neureka.devices.opencl.OpenCLDevice;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class Sinus extends AbstractOperation
{

    private DefaultOperatorCreator<TertiaryNDIConsumer> _creator =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.sin(t1_val[ t1Idx.i() ]);
                else return ( t0Idx, t1Idx, t2Idx ) -> Math.cos(t1_val[ t1Idx.i() ]);
            };

    private DefaultOperatorCreator<TertiaryNDXConsumer> _creatorX =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.sin(t1_val[inputs[ 1 ].i_of_idx( t1Idx )]);
                else return ( t0Idx, t1Idx, t2Idx ) -> Math.cos(t1_val[inputs[ 1 ].i_of_idx( t1Idx )]);
            };

    public Sinus()
    {
        super("sin", "sin" , 1, false, false, true, false);

        Activation operationAlgorithm = new Activation()
        .setBackwardADAnalyzer( call -> true )
        .setForwardADAnalyzer(
             call -> {
                 Tsr last = null;
                 for ( Tsr t : call.getTensors() ) {
                     if ( last != null && !last.shape().equals(t.shape()) ) return false;
                     last = t; // Note: shapes are cached!
                 }
                 return true;
             }
        ).setADAgentSupplier(
            ( Function f, ExecutionCall<Device> call, boolean forward ) ->
            getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
        ).setCallHook(  (caller, call ) -> null )
         .setRJAgent( ( call, goDeeperWith ) -> null )
         .setDrainInstantiation(
             call -> {
                 Tsr[] tsrs = call.getTensors();
                 Device device = call.getDevice();
                 if ( tsrs[ 0 ] == null ) // Creating a new tensor:
                 {
                     int[] shp = tsrs[ 1 ].getNDConf().shape();
                     Tsr output = new Tsr( shp, 0.0 );
                     output.setIsVirtual( false );
                     try {
                         device.store(output);
                     } catch ( Exception e ) {
                         e.printStackTrace();
                     }
                     tsrs[ 0 ] = output;
                 }
                 return call;
             }
        )
        .build();

        setAlgorithm(
                Activation.class,
                operationAlgorithm.setImplementationFor(
                        HostCPU.class,
                        new HostImplementation(
                                call  ->
                                        call.getDevice().getExecutor()
                                                .threaded (
                                                        call.getTensor( 0 ).size(),
                                                        (Neureka.instance().settings().indexing().isUsingArrayBasedIndexing())
                                                        ? ( start, end ) ->
                                                                Activation.activate (
                                                                        call.getTensor( 0 ),
                                                                        start, end,
                                                                        _creatorX.create(call.getTensors(), call.getDerivativeIndex())
                                                                )
                                                        : ( start, end ) ->
                                                                Activation.activate (
                                                                        call.getTensor( 0 ), call.getTensor( 1 ),
                                                                        start, end,
                                                                        _creator.create(call.getTensors(), call.getDerivativeIndex())
                                                                )
                                                ),
                                3
                        )
                ).setImplementationFor(
                        OpenCLDevice.class,
                        new CLImplementation(
                                call -> {
                                    int offset = (call.getTensor( 0 ) != null) ? 0 : 1;
                                    int gwz = (call.getTensor( 0 ) != null) ? call.getTensor( 0 ).size() : call.getTensor( 1 ).size();
                                    call.getDevice().getKernel(call)
                                            .pass( call.getTensor( offset ) )
                                            .pass( call.getTensor( offset + 1 ) )
                                            .pass( call.getTensor( 0 ).rank() )
                                            .pass( call.getDerivativeIndex() )
                                            .call( gwz );
                                },
                                3,
                                operationAlgorithm.getKernelSource(), // kernelSource
                                "output = sin( input );\n",
                                "output = cos( input );\n",
                                this // OperationType
                        )
                )
        );




    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return "sin" + expression;
        return "sin" + "(" + expression + ")";
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) {
        return calculate(
                src[ 0 ].call( inputs, j ),
                d >= 0
        ) * ( ( d < 0 ) ? 1 : src[ 0 ].derive( inputs, d, j ) );
    }

    @Contract(pure = true)
    public static double calculate(double input, boolean derive ) {
        if ( !derive ) return Math.sin( input );
        else return Math.cos( input );
    }




}
