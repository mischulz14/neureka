package neureka.backend.standard.operations.function;

import neureka.Neureka;
import neureka.backend.api.operations.OperationFactory;
import neureka.devices.Device;
import neureka.backend.standard.implementations.HostImplementation;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.calculus.Function;
import neureka.backend.standard.algorithms.Activation;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.ExecutionCall;
import neureka.devices.host.HostCPU;
import neureka.devices.opencl.OpenCLDevice;
import org.jetbrains.annotations.Contract;

import java.util.List;


public class Softplus extends AbstractOperation
{

    private final DefaultOperatorCreator<TertiaryNDIConsumer> _creator =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.log(1 + Math.pow(Math.E, t1_val[ t1Idx.i() ]));
                else return ( t0Idx, t1Idx, t2Idx ) -> 1 / (1 + Math.pow(Math.E, -t1_val[ t1Idx.i() ]));
            };

    private final DefaultOperatorCreator<TertiaryNDXConsumer> _creatorX =
            ( inputs, d ) -> {
                double[] t1_val = inputs[ 1 ].value64();
                if ( d < 0 ) return ( t0Idx, t1Idx, t2Idx ) -> Math.log(1 + Math.pow(Math.E, t1_val[inputs[ 1 ].i_of_idx( t1Idx )]));
                else return ( t0Idx, t1Idx, t2Idx ) -> 1 / (1 + Math.pow(Math.E, -t1_val[inputs[ 1 ].i_of_idx( t1Idx )]));
            };

    public Softplus()
    {
        super(
                new OperationFactory()
                        .setFunction(         "softplus"    )
                        .setOperator(         "softplus"    )
                        .setArity(            1             )
                        .setIsOperator(       false         )
                        .setIsIndexer(        false         )
                        .setIsDifferentiable( true          )
                        .setIsInline(         false         )
        );

        Activation operationAlgorithm = new Activation()
            .setADAgentSupplier(
                ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                    getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
            )
            .build();


        setAlgorithm(
                Activation.class,
                operationAlgorithm
                        .setImplementationFor(
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
                                "output = \n" +
                                        "   (\n" +
                                        "        (float) log(\n" +
                                        "            1+pow(\n" +
                                        "                (float)\n" +
                                        "                M_E,\n" +
                                        "                (float)\n" +
                                        "                input\n" +
                                        "            )\n" +
                                        "        )\n" +
                                        "    );",
                                "output =\n" +
                                        "    1 /\n" +
                                        "        (1 + (float) pow(\n" +
                                        "                (float)M_E,\n" +
                                        "                (float)input\n" +
                                        "            )\n" +
                                        "        );\n",
                                this // OperationType
                        )
                )
        );



    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return "softplus" + expression;
        return "softplus" + "(" + expression + ")";
    }

    @Override
    public String asDerivative( Function[] children, int d ) {
        throw new IllegalStateException("Operation does not support dynamic derivation!");
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
        if ( !derive ) return Math.log(1 + Math.pow(Math.E, input));
        else return Sigmoid.calculate(input, false);
    }



}
