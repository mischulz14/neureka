package neureka.backend.standard.operations.function;

import neureka.backend.api.ExecutionCall;
import neureka.backend.api.PrimitiveFun;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.operations.OperationBuilder;
import neureka.backend.standard.algorithms.Activation;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.backend.standard.implementations.CPUImplementation;
import neureka.calculus.Function;
import neureka.calculus.args.Arg;
import neureka.devices.Device;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import org.jetbrains.annotations.Contract;

public final class Tanh extends AbstractOperation
{

    private final DefaultOperatorCreator<TertiaryF64NDFun> _creator =
            ( inputs, d ) ->
            {
                double[] t1_val = inputs[ 1 ].getDataAs( double[].class );
                if ( d < 0 ) {
                    return ( t0Idx, t1Idx, t2Idx ) -> {
                        double input = t1_val[ t1Idx.i() ];
                        return input / Math.pow(1 + Math.pow(input, 2), 0.5);
                    };
                } else {
                    return ( t0Idx, t1Idx, t2Idx ) -> {
                        double input = t1_val[ t1Idx.i() ];
                        return 1 - Math.pow(input / Math.pow(1 + Math.pow(input, 2), 0.5), 2);
                    };
                }
            };

    public Tanh()
    {
        super (
                new OperationBuilder()
                        .setFunction(         "tanh"    )
                        .setOperator(         "tanh"    )
                        .setArity(            1         )
                        .setIsOperator(       false     )
                        .setIsIndexer(        false     )
                        .setIsDifferentiable( true      )
                        .setIsInline(         false     )
        );

        Activation operationAlgorithm = new Activation()
            .setSupplyADAgentFor(
                ( Function f, ExecutionCall<? extends Device<?>> call, boolean forward ) ->
                getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
            )
            .buildFunAlgorithm();

        setAlgorithm(
                Activation.class,
                operationAlgorithm.setImplementationFor(
                        CPU.class,
                        CPUImplementation
                            .withArity(3)
                            .andImplementation(
                                call  ->
                                    call.getDevice().getExecutor()
                                        .threaded(
                                            call.getTsrOfType( Number.class, 0 ).size(),
                                            Activation.newWorkloadFor(
                                                call,
                                                new Activation.Fun<>(
                                                   x -> x / Math.pow(1d + Math.pow(x, 2d), 0.5d),
                                                   x -> 1 - Math.pow(x / Math.pow(1 + Math.pow(x, 2), .5), 2)
                                                ),
                                                new Activation.Fun<>(
                                                   x -> (float) (x / Math.pow(1f + Math.pow(x, 2f), .5f)),
                                                   x -> (float) (1f - Math.pow(x / Math.pow(1 + Math.pow(x, 2f), .5f), 2f))
                                                )
                                            )
                                        )
                            )
                ).setImplementationFor(
                        OpenCLDevice.class,
                        CLImplementation.compiler()
                                .arity( 3 )
                                .kernelSource( operationAlgorithm.getKernelSource() )
                                .activationSource( "output = input/pow(1+pow(input, 2.0f), 0.5f);\n" )
                                .differentiationSource( "output = 1-pow(input/pow((1.0f+pow(input,2.0f)),0.5f), 2.0f);\n" )
                                .kernelPostfix( this.getFunction() )
                                .execution(
                                        call -> {
                                            int offset = (call.getTsrOfType( Number.class, 0 ) != null) ? 0 : 1;
                                            int gwz = (call.getTsrOfType( Number.class, 0 ) != null) ? call.getTsrOfType( Number.class, 0 ).size() : call.getTsrOfType( Number.class, 1 ).size();
                                            call.getDevice().getKernel(call)
                                                    .passAllOf( call.getTsrOfType( Number.class, offset ) )
                                                    .passAllOf( call.getTsrOfType( Number.class, offset + 1 ) )
                                                    .pass( call.getTsrOfType( Number.class, 0 ).rank() )
                                                    .pass( call.getValOf( Arg.DerivIdx.class ) )
                                                    .call( gwz );
                                        }
                                )
                                .build()
                )
        );




    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return "tanh" + expression;
        return "tanh" + "(" + expression + ")";
    }

    @Override
    public String asDerivative( Function[] children, int derivationIndex) {
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
        final double pow = Math.pow((1 + Math.pow(input, 2)), 0.5);
        if ( !derive ) {
            return input / pow;
        } else {
            return (1 - Math.pow((input / pow), 2));
        }
    }

}

