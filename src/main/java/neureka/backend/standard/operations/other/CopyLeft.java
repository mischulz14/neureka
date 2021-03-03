package neureka.backend.standard.operations.other;

import neureka.Neureka;
import neureka.Tsr;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.operations.OperationContext;
import neureka.backend.api.operations.OperationFactory;
import neureka.backend.standard.algorithms.Activation;
import neureka.backend.standard.algorithms.Scalarization;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.backend.standard.implementations.HostImplementation;
import neureka.calculus.Function;
import neureka.devices.Device;
import neureka.devices.host.HostCPU;
import neureka.devices.opencl.OpenCLDevice;

public class CopyLeft extends AbstractOperation {

    public CopyLeft() {
        super(
                new OperationFactory()
                        .setFunction(         "left_inline"    )
                        .setOperator(         "<"        )
                        .setArity(            -1         )
                        .setIsOperator(       true       )
                        .setIsIndexer(        false      )
                        .setIsDifferentiable( false       )
                        .setIsInline(         true      )
        );

        Scalarization scalarization = new Scalarization()
                .setSuitabilityChecker(
                        call ->
                        {
                            if ( call.getTensor( 1 ).isVirtual() || call.getTensor( 1 ).size() == 1 ) {
                                return 1.0f;
                            } else return 0.0f;
                        }
                )
                .setBackwardADAnalyzer( call -> false )
                .setForwardADAnalyzer( call -> false )
                .setADAgentSupplier(
                        ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                                getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
                )
                .setCallHook( (caller, call ) -> null )
                .setRJAgent( ( call, goDeeperWith ) -> null )
                .setDrainInstantiation(
                        call ->
                        {
                            Tsr[] tsrs = call.getTensors();
                            int offset = ( tsrs[ 0 ] == null ) ? 1 : 0;
                            call.getTensor(offset).incrementVersionBecauseOf(call);
                            call.getTensor(offset).setIsVirtual( false );
                            return
                                    ExecutionCall.builder()
                                        .device( call.getDevice() )
                                        .tensors( new Tsr[]{tsrs[offset], tsrs[1+offset]} )
                                        .derivativeIndex( -1 )
                                        .operation( this )
                                        .build();
                        }
                )
                .build();

        ScalarOperatorCreator<PrimaryNDIConsumer> scalarCreator =
                (inputs, value, d) -> {
                    double[] t1_val = inputs[ 1 ].value64();
                    if ( d < 0 ) return t1Idx -> t1_val[ t1Idx.i() ] = value;
                    else return null;
                };

        ScalarOperatorCreator<PrimaryNDXConsumer> scalarXCreator =
                (inputs, value, d) -> {
                    double[] t1_val = inputs[ 1 ].value64();
                    if ( d < 0 ) return t1Idx -> t1_val[inputs[ 1 ].indexOfIndices( t1Idx )] = value;
                    else return null;
                };

        setAlgorithm(
                Scalarization.class,
                scalarization.setImplementationFor(
                        HostCPU.class,
                        new HostImplementation(
                                call ->
                                {
                                    double value = call.getTensor( 1 ).value64( 0 );
                                    call.getDevice().getExecutor()
                                            .threaded (
                                                    call.getTensor( 0 ).size(),
                                                    (Neureka.instance().settings().indexing().isUsingArrayBasedIndexing())
                                                    ? ( start, end ) ->
                                                            Scalarization.scalarize (
                                                                    call.getTensor( 0 ),
                                                                    start, end,
                                                                    scalarXCreator.create(call.getTensors(), value, -1)
                                                            )
                                                    : ( start, end ) ->
                                                            Scalarization.scalarize (
                                                                    call.getTensor( 0 ),
                                                                    start, end,
                                                                    scalarCreator.create(call.getTensors(), value, -1)
                                                            )
                                            );
                                },
                                2
                        )
                ).setImplementationFor(
                        OpenCLDevice.class,
                        new CLImplementation(
                                call -> {
                                    Tsr t = call.getTensor( 0 );
                                    int gwz = t.size();
                                    call.getDevice().getKernel(call)
                                            .pass( t )
                                            .pass( t )
                                            .pass( call.getTensor( 1 ).value32( 0 ) )
                                            .pass( t.rank() )
                                            .pass( call.getDerivativeIndex() )
                                            .call( gwz );
                                },
                                2,
                                scalarization.getKernelSource(), // kernelSource
                                "output = value;\n",
                                "output = value;\n",
                                this // OperationType
                        )
                )
        );

        Activation activation = new Activation()
            .setBackwardADAnalyzer( call -> false )
            .setForwardADAnalyzer( call -> false )
            .setADAgentSupplier(
                ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                        getDefaultAlgorithm().supplyADAgentFor( f, call, forward )
            )
            .setCallHook( (caller, call ) -> null )
            .setRJAgent( ( call, goDeeperWith ) -> null )
            .setDrainInstantiation(
                    call ->
                    {
                        Tsr[] tsrs = call.getTensors();
                        int offset = ( tsrs[ 0 ] == null ) ? 1 : 0;
                        call.getTensor(offset).incrementVersionBecauseOf(call);
                        return ExecutionCall.builder()
                                    .device(call.getDevice())
                                    .tensors(new Tsr[]{tsrs[offset], tsrs[1+offset]})
                                    .derivativeIndex(-1)
                                    .operation(OperationContext.get().instance("idy"))
                                    .build();
                    }
            )
            .build();

        setAlgorithm(
                Activation.class,
                activation
                    .setImplementationFor(
                        HostCPU.class,
                        new HostImplementation(
                                call ->
                                {
                                    call.getTensor( 0 ).setIsVirtual( false );
                                    OperationContext.get().instance("idy")
                                            .getAlgorithm( Activation.class )
                                            .getImplementationFor( HostCPU.class )
                                            .run(call);
                                },
                                2
                        )
                    )
                    .setImplementationFor(
                        OpenCLDevice.class,
                        new CLImplementation(
                                call -> {
                                    call.getTensor( 0 ).setIsVirtual( false );
                                    OperationContext.get().instance("idy")
                                            .getAlgorithm(Activation.class)
                                            .getImplementationFor( OpenCLDevice.class )
                                            .run(call);
                                },
                                2
                        )
                )
        );
    }


    @Override
    public String stringify( String[] children ) {
        StringBuilder reconstructed = new StringBuilder();
        for ( int i = 0; i < children.length; ++i ) {
            reconstructed.append( children[ i ] );
            if ( i < children.length - 1 ) reconstructed.append(" <- ");
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
