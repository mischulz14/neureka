package neureka.backend.standard.operations.function;

import neureka.Neureka;
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

public class Absolute extends AbstractOperation
{

    private DefaultOperatorCreator<TertiaryNDIConsumer> _activationCreator =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.abs(t1_val[ t1Idx.i() ]);
                else return ( t0Idx, t1Idx, t2Idx ) -> ( t1_val[ t1Idx.i() ] < 0 ) ? -1 : 1;
            };
    private DefaultOperatorCreator<TertiaryNDXConsumer> _activationXCreator =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.abs(t1_val[inputs[ 1 ].i_of_idx( t1Idx )]);
                else return ( t0Idx, t1Idx, t2Idx ) -> ( t1_val[inputs[ 1 ].i_of_idx( t1Idx )] < 0 ) ? -1 : 1;
            };

    public Absolute()
    {
        super("abs", "abs" , 1, false, false, true, false);

        Activation operationAlgorithm = new Activation()
            .setADAgentSupplier(
                ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
            )
            .build();

        setAlgorithm(
                Activation.class,
                operationAlgorithm.setImplementationFor(
                        HostCPU.class,
                        new HostImplementation(
                                call  -> call.getDevice().getExecutor()
                                            .threaded(
                                                call.getTensor( 0 ).size(),
                                                   (Neureka.instance().settings().indexing().isUsingArrayBasedIndexing())
                                                      ? ( start, end ) ->
                                                                Activation.activate(
                                                                        call.getTensor( 0 ),
                                                                        start, end,
                                                                        _activationXCreator.create(call.getTensors(), call.getDerivativeIndex())
                                                                )
                                                      : ( start, end ) ->
                                                                Activation.activate(
                                                                        call.getTensor( 0 ), call.getTensor( 1 ),
                                                                        start, end,
                                                                        _activationCreator.create(call.getTensors(), call.getDerivativeIndex())
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
                                    call.getDevice().getKernel( call )
                                            .pass( call.getTensor( offset ) )
                                            .pass( call.getTensor(offset + 1 ) )
                                            .pass( call.getTensor( 0 ).rank() )
                                            .pass( call.getDerivativeIndex() )
                                            .call( gwz );
                                },
                                3,
                                operationAlgorithm.getKernelSource(), // kernelSource
                                "output = fabs( input );\n", // activationSource
                                "output = ( input < 0 ) ? -1 : 1;\n", //differentiationSource
                                this // OperationType
                        )
                )
        );
    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return "abs" + expression;
        return "abs" + "(" + expression + ")";
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) {
        return calculate(
                src[ 0 ].call( inputs, j ),
                d >= 0
        ) * ( ( d < 0 ) ? 1 : src[ 0 ].derive( inputs, d, j ) );
    }

    @Contract( pure = true )
    public static double calculate( double input, boolean derive ) {
        if ( !derive ) return Math.abs( input );
        else return ( input < 0 ) ? -1 : 1;
    }



}
