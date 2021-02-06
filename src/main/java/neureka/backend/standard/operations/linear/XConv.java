package neureka.backend.standard.operations.linear;

import neureka.Neureka;
import neureka.Tsr;
import neureka.autograd.DefaultADAgent;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.algorithms.Algorithm;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.operations.Operation;
import neureka.backend.api.operations.OperationContext;
import neureka.backend.api.operations.OperationFactory;
import neureka.backend.standard.algorithms.Convolution;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.backend.standard.implementations.HostImplementation;
import neureka.calculus.Function;
import neureka.calculus.assembly.FunctionBuilder;
import neureka.devices.Device;
import neureka.devices.host.HostCPU;
import neureka.devices.opencl.OpenCLDevice;

import java.util.List;

public class XConv extends AbstractOperation
{

    public XConv()
    {
        super(
                new OperationFactory()
                        .setFunction(         "multiply"    )
                        .setOperator(         "x"    )
                        .setArity(            2          )
                        .setIsOperator(       true       )
                        .setIsIndexer(        false       )
                        .setIsDifferentiable( true        )
                        .setIsInline(         false       )
        );

        Algorithm.RecursiveJunctionAgent rja = (call, goDeeperWith)->
        {
            Tsr[] tsrs = call.getTensors();
            Device device = call.getDevice();
            int d = call.getDerivativeIndex();
            Operation type = call.getOperation();

            Tsr alternative = null;
            if (tsrs.length > 3) {
                if ( d < 0 ) {
                    Tsr[] reduction = new Tsr[]{tsrs[ 0 ], tsrs[ 1 ], tsrs[ 2 ]};
                    alternative = goDeeperWith.apply(
                            new ExecutionCall<>(device, reduction, d, type)
                    );
                    tsrs[ 0 ] = reduction[ 0 ];

                    reduction = Utility.offsetted(tsrs, 1);
                    alternative = goDeeperWith.apply(
                            new ExecutionCall<>(device, reduction, d, type)
                    );
                    tsrs[ 0 ] = reduction[ 0 ];
                }
                return alternative;
            } else {
                if ( call.getOperation().getOperator().equals("x") ) {
                    if (d >= 0) {
                        if (d == 0) tsrs[ 0 ] = tsrs[ 2 ];
                        else tsrs[ 0 ] = tsrs[ 1 ];
                        return tsrs[ 0 ];
                    } else {
                        call.mutateArguments( t -> new Tsr[]{t[ 0 ], t[ 1 ], t[ 2 ]} );
                    }
                } else if ( call.getOperation().getOperator().equals("x"+ ((char) 187)) ) {
                    call.mutateArguments( t -> new Tsr[]{t[ 2 ], t[ 1 ], t[ 0 ]} );
                }
                return alternative;
            }
        };

        DefaultOperatorCreator<TertiaryNDIConsumer> convolutionNDICreator =
                ( inputs, d ) -> {
                    double[] t1_val = inputs[ 1 ].value64();
                    double[] t2_val = inputs[ 2 ].value64();
                    if ( d < 0 ) {
                        return ( t0Idx, t1Idx, t2Idx ) -> t1_val[ t1Idx.i() ] * t2_val[t2Idx.i()];
                    } else {
                        return ( t0Idx, t1Idx, t2Idx ) -> {
                            if (d == 0) return t2_val[t2Idx.i()];
                            else return t1_val[ t1Idx.i() ];
                        };
                    }
                };
        DefaultOperatorCreator<TertiaryNDXConsumer> convolutionCreator =
                ( inputs, d ) -> {
                    double[] t1_val = inputs[ 1 ].value64();
                    double[] t2_val = inputs[ 2 ].value64();
                    if ( d < 0 ) {
                        return ( t0Idx, t1Idx, t2Idx ) -> t1_val[inputs[ 1 ].i_of_idx( t1Idx )] * t2_val[inputs[ 2 ].i_of_idx(t2Idx)];
                    } else {
                        return ( t0Idx, t1Idx, t2Idx ) -> {
                            if (d == 0) return t2_val[inputs[ 2 ].i_of_idx(t2Idx)];
                            else return t1_val[inputs[ 1 ].i_of_idx( t1Idx )];
                        };
                    }
                };

        Convolution convolution = new Convolution()
            .setBackwardADAnalyzer( call -> true )
            .setForwardADAnalyzer(
                call -> {
                    if ( call.getOperation().supports(Convolution.class) ) return false;
                    if ( call.getOperation().getOperator().equals(",") ) return false; //Reshape
                    Tsr<?> last = null;
                    for ( Tsr<?> t : call.getTensors() ) {
                        if ( last != null && !last.shape().equals(t.shape()) ) return false;
                        last = t; // Note: shapes are cached!
                    }
                    return true;
                }
            )
            .setADAgentSupplier(
                ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                {
                    Tsr ctxDerivative = (Tsr)call.getAt("derivative");
                    if ( forward ) throw new IllegalArgumentException("Convolution of does not support forward-AD!");

                    Function mul = Function.Detached.MUL;
                    Tsr[] inputs = call.getTensors();
                    int d = call.getDerivativeIndex();

                    Function invX = FunctionBuilder.build(
                            "I[ 0 ]" + getOperator() + ">>I[ 1 ]" + getOperator() + ">>I[ 2 ]",
                            false
                    );
                    Tsr deriv = f.derive( inputs, d );
                    assert deriv != null;
                    assert invX != null;
                    return new DefaultADAgent( deriv )
                    .setForward( (node, forwardDerivative ) -> mul.call(new Tsr[]{forwardDerivative, deriv}) )
                    .setBackward( (t, error) -> invX.call(new Tsr[]{error, deriv, new Tsr(t.getPayload().shape(), 0)}) );
                }
            )
            .setCallHook(
                    ( caller, call ) -> {
                        if ( !caller.isFlat() ) return null;
                        if ( call.getOperation().getOperator().equals("x") ) {

                            Tsr[] inputs = call.getTensors();
                            Tsr[] tsrs = new Tsr[]{null, inputs[ 0 ], inputs[ 1 ]};
                            tsrs[ 0 ] = (call.getDerivativeIndex() < 0)
                                    ? new Tsr(Tsr.Utility.Indexing.shpOfCon(tsrs[ 1 ].getNDConf().shape(), tsrs[ 2 ].getNDConf().shape()))
                                    : null;

                            for (Tsr t : tsrs) if (t != null) t.setIsVirtual( false );
                            call.getDevice().execute(call.withNew(tsrs));
                            return tsrs[ 0 ];
                        } else {
                            if (call.getDerivativeIndex() < 0) {
                                Tsr[] tsrs = caller.srcActivation(call.getTensors(), call.getJ(), -1, 0);
                                Tsr.makeFit(tsrs, caller.isDoingAD()); // This might not fit here... (fitting should probably be a setup thing...)
                                for ( Tsr t : tsrs ) t.setIsVirtual( false );
                                call.getDevice().execute( new ExecutionCall( call.getDevice(), tsrs, 0, call.getOperation() ) );
                                if ( call.getOperation().getId() == OperationContext.get().instance("x>>").getId()) return tsrs[ 2 ];
                                else return tsrs[ 0 ];
                            }
                        }
                        return null;
                    }
            )
            .setRJAgent( rja )
            .setDrainInstantiation(
                    call -> {
                        Tsr[] tsrs = call.getTensors();
                        Device device = call.getDevice();
                        if ( tsrs[ 0 ] == null ) // Creating a new tensor:
                        {
                            int[] shp = Tsr.Utility.Indexing.shpOfCon(tsrs[ 1 ].getNDConf().shape(), tsrs[ 2 ].getNDConf().shape());
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
                Convolution.class,
                convolution
                        .setImplementationFor(
                                HostCPU.class,
                                new HostImplementation(
                                        call ->
                                                call.getDevice().getExecutor()
                                                        .threaded (
                                                                call.getTensor( 0 ).size(),
                                                                (Neureka.instance().settings().indexing().isUsingArrayBasedIndexing())
                                                                ? ( start, end ) ->
                                                                        Convolution.convolve (
                                                                                call.getTensor( 0 ), call.getTensor(1), call.getTensor(2),
                                                                                call.getDerivativeIndex(), start, end,
                                                                                convolutionCreator.create(
                                                                                        call.getTensors(),
                                                                                        -1//call.getDerivativeIndex()
                                                                                )
                                                                        )
                                                                :  ( start, end ) ->
                                                                        Convolution.convolve (
                                                                                call.getTensor( 0 ), call.getTensor(1), call.getTensor(2),
                                                                                call.getDerivativeIndex(), start, end,
                                                                                convolutionNDICreator.create(
                                                                                        call.getTensors(),
                                                                                        -1//call.getDerivativeIndex()
                                                                                )
                                                                        )
                                                        ),
                                        3
                                )
                        )
                        .setImplementationFor(
                            OpenCLDevice.class,
                            new CLImplementation(
                                    call -> {
                                        int offset = ( call.getTensor( 0 ) != null ) ? 0 : 1;
                                        int gwz = ( call.getTensor( 0 ) != null ) ? call.getTensor( 0 ).size() : call.getTensor( 1 ).size();
                                        call.getDevice().getKernel(call)
                                                .pass( call.getTensor( offset ) )
                                                .pass( call.getTensor( offset + 1 ) )
                                                .pass( call.getTensor( offset + 2 ) )
                                                .pass( call.getTensor( 0 ).rank() )
                                                .pass( call.getDerivativeIndex() ) //call.getDerivativeIndex()
                                                .call( gwz );
                                    },
                                    3,
                                    convolution.getKernelSource(), // kernelSource
                                    "value = src1 * src2;\n",
                                    "value += handle * drain;\n",
                                    this // OperationType
                            )
                        )
        );
        new AbstractOperation(
                "inv_convolve_mul_left", ((char) 171) + "x",
                3,
                true,
                false,
                false,
                false
        ) {
            @Override
            public String stringify(String[] children) {
                StringBuilder reconstructed = new StringBuilder();
                for ( int i = 0; i < children.length; ++i ) {
                    reconstructed.append( children[ i ] );
                    if ( i < children.length - 1 ) {
                        reconstructed.append(" "+((char) 171) + "x ");
                    }
                }
                return "(" + reconstructed + ")";
            }

            @Override
            public String asDerivative( Function[] children, int d ) {
                throw new IllegalStateException("Operation does not support dynamic derivation!");
            }

            @Override
            public double calculate( double[] inputs, int j, int d, Function[] src ) {
                return src[ 0 ].call( inputs, j );
            }
        }
        .setAlgorithm(Convolution.class, convolution);

        new AbstractOperation(
                "inv_convolve_mul_right", "x" + ((char) 187),
                3,
                true,
                false,
                false,
                false
                ) {
            @Override
            public String stringify(String[] children) {
                StringBuilder reconstructed = new StringBuilder();
                for ( int i = 0; i < children.length; ++i ) {
                    reconstructed.append( children[ i ] );
                    if ( i < children.length - 1 ) {
                        reconstructed.append(" x" + ((char) 187)+" ");
                    }
                }
                return "(" + reconstructed + ")";
            }

            @Override
            public String asDerivative( Function[] children, int d ) {
                throw new IllegalStateException("Operation does not support dynamic derivation!");
            }

            @Override
            public double calculate( double[] inputs, int j, int d, Function[] src ) {
                return 0;
            }
        }
        .setAlgorithm( Convolution.class, convolution );




    }


    @Override
    public String stringify( String[] children ) {
        StringBuilder reconstructed = new StringBuilder();
        for ( int i = 0; i < children.length; ++i ) {
            reconstructed.append( children[ i ] );
            if ( i < children.length - 1 ) {
                reconstructed.append(" x ");
            }
        }
        return "(" + reconstructed + ")";
    }

    @Override
    public String asDerivative( Function[] children, int d ) {
        throw new IllegalStateException("Operation does not support dynamic derivation!");
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) {
            return src[ 0 ].call( inputs, j );
    }
}
