package neureka.backend.standard.operations.operator;

import neureka.Neureka;
import neureka.Tsr;
import neureka.autograd.ADAgent;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.operations.OperationBuilder;
import neureka.backend.standard.algorithms.Broadcast;
import neureka.backend.standard.algorithms.Fun;
import neureka.backend.standard.algorithms.Operator;
import neureka.backend.standard.algorithms.Scalarization;
import neureka.backend.standard.implementations.CLImplementation;
import neureka.backend.standard.implementations.CPUImplementation;
import neureka.backend.standard.memory.MemUtil;
import neureka.backend.standard.operations.JunctionUtil;
import neureka.calculus.internal.CalcUtil;
import neureka.calculus.Function;
import neureka.calculus.args.Arg;
import neureka.devices.Device;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.stream.Collectors;


public class Multiplication extends AbstractOperation
{
    public Multiplication()
    {
        super(
                new OperationBuilder()
                        .setFunction(         "multiply"    )
                        .setOperator(         "*"        )
                        .setArity(            -1         )
                        .setIsOperator(       true       )
                        .setIsIndexer(        false      )
                        .setIsDifferentiable( true       )
                        .setIsInline(         false      )
        );

        //_____________________
        // DEFAULT OPERATION :

        Operator operator = new Operator(JunctionUtil::forMultiplications)
                                   .setSupplyADAgentFor( getDefaultAlgorithm() )
                                   .buildFunAlgorithm();

        setAlgorithm(
            Operator.class,
            operator.setImplementationFor(
                CPU.class,
                CPUImplementation
                    .withArity(3)
                    .andImplementation(
                        Operator.implementationForCPU()
                                .with(Fun.F64F64ToF64.triple(
                                        ( a, b ) -> a * b,
                                        ( a, b ) -> b, // Deriving at input 0
                                        ( a, b ) -> a  // deriving input 1
                                ))
                                .with(Fun.F32F32ToF32.triple(
                                        ( a, b ) -> a * b,
                                        ( a, b ) -> b, // Deriving at input 0
                                        ( a, b ) -> a  // deriving input 1
                                ))
                                .get()
                    )
            )
            .setImplementationFor(
                OpenCLDevice.class,
                CLImplementation
                    .compiler()
                    .arity( 3 )
                    .kernelSource( operator.getKernelSource() )
                    .activationSource( "output = input1 * input2;\n" )
                    .differentiationSource( "if ( d==0 ) {output = input2;}else{output = input1;}\n" )
                    .kernelPostfix( this.getFunction() )
                    .execution(
                        call -> {
                            int offset = (call.getTsrOfType( Number.class, 0 ) != null) ? 0 : 1;
                            int gwz = (call.getTsrOfType( Number.class, 0 ) != null) ? call.getTsrOfType( Number.class, 0 ).size() : call.getTsrOfType( Number.class, 1 ).size();
                            call.getDevice()
                                .getKernel(call)
                                .passAllOf( call.getTsrOfType( Number.class, offset ) )
                                .passAllOf( call.getTsrOfType( Number.class, offset + 1 ) )
                                .passAllOf( call.getTsrOfType( Number.class, offset + 2 ) )
                                .pass( call.getTsrOfType( Number.class, 0 ).rank() )
                                .pass( call.getDerivativeIndex() )
                                .call( gwz );
                        }
                    )
                    .build()
            )
        );


        //________________
        // BROADCASTING :

        Broadcast broadcast = new Broadcast( JunctionUtil::forMultiplications )
                .setCanPerformBackwardADFor( call -> true )
                .setCanPerformForwardADFor( call -> false )
                .setSupplyADAgentFor(
                    ( Function f, ExecutionCall<? extends Device<?>> call, boolean forward ) ->
                    {
                        Tsr<?> ctxDerivative = (Tsr<?>) call.getValOf(Arg.Derivative.class);
                        Function mul = Neureka.get().backend().getFunction().mul();
                        if ( ctxDerivative != null ) {
                            return ADAgent.of( ctxDerivative )
                                            .setForward( (node, forwardDerivative ) -> mul.execute( forwardDerivative, ctxDerivative ) )
                                            .setBackward( (node, forwardDerivative ) -> mul.execute( forwardDerivative, ctxDerivative ) );
                        }
                        Tsr<?>[] inputs = call.getTensors();
                        int d = call.getDerivativeIndex();
                        if ( forward ) throw new IllegalArgumentException("Broadcast implementation does not support forward-AD!");
                        else
                        {
                            Tsr<?> derivative = MemUtil.keep( inputs, () -> f.executeDerive( inputs, d ) );
                            return ADAgent.of( derivative )
                                    .setForward( (node, forwardDerivative ) -> mul.execute( forwardDerivative, derivative ) )
                                    .setBackward( (node, backwardError ) -> mul.execute( backwardError, derivative ) );
                        }
                    }
                )
                .buildFunAlgorithm();

        setAlgorithm(
            Broadcast.class,
            broadcast
                .setImplementationFor(
                    CPU.class,
                    Broadcast.implementationForCPU()
                            .with(Fun.F64F64ToF64.triple(
                                ( a, b ) -> a * b,
                                ( a, b ) -> b, // Deriving at input 0
                                ( a, b ) -> a  // deriving input 1
                            ))
                            .with(Fun.F32F32ToF32.triple(
                                ( a, b ) -> a * b,
                                ( a, b ) -> b, // Deriving at input 0
                                ( a, b ) -> a  // deriving input 1
                            ))
                            .get()
                )
                .setImplementationFor(
                    OpenCLDevice.class,
                    CLImplementation.compiler()
                        .arity( 3 )
                        .kernelSource( broadcast.getKernelSource() )
                        .activationSource( "value = src1 * src2;\n" )
                        .differentiationSource( "value += ( d == 0 ? drain : handle );\n" )
                        .kernelPostfix( this.getFunction() )
                        .execution(
                            call -> {
                                int offset = (call.getTsrOfType( Number.class, 0 ) != null) ? 0 : 1;
                                int gwz = (call.getTsrOfType( Number.class, 0 ) != null) ? call.getTsrOfType( Number.class, 0 ).size() : call.getTsrOfType( Number.class, 1 ).size();
                                call.getDevice().getKernel(call)
                                        .passAllOf( call.getTsrOfType( Number.class, offset ) )
                                        .passAllOf( call.getTsrOfType( Number.class, offset + 1 ) )
                                        .passAllOf( call.getTsrOfType( Number.class, offset + 2 ) )
                                        .pass( call.getTsrOfType( Number.class, 0 ).rank() )
                                        .pass( call.getValOf( Arg.DerivIdx.class ) )
                                        .call( gwz );
                            }
                        )
                        .build()
            )
        );




        //___________________________
        // TENSOR SCALAR OPERATION :

        Scalarization scalarization = new Scalarization()
                .setCanPerformBackwardADFor( call -> true )
                .setCanPerformForwardADFor( call -> true )
                .setSupplyADAgentFor( getDefaultAlgorithm() )
                .setExecutionDispatcher( (caller, call) -> CalcUtil.executeFor( caller, call, JunctionUtil::forMultiplications ) )
                .buildFunAlgorithm();

        setAlgorithm(
            Scalarization.class,
            scalarization.setImplementationFor(
                CPU.class,
                CPUImplementation
                    .withArity(3)
                    .andImplementation(
                        call -> {
                            if ( call.getDerivativeIndex() == 0 )
                                call.getTensors()[0] = call.getTensors()[2].shallowCopy().getUnsafe().setIsIntermediate( true );
                            else if ( call.getDerivativeIndex() == 1 )
                                call.getTensors()[0] = call.getTensors()[1].shallowCopy().getUnsafe().setIsIntermediate( true );
                            else
                                Scalarization.implementationForCPU()
                                        .with(Fun.F64F64ToF64.triple(
                                            ( a, b ) -> a * b,
                                            ( a, b ) -> b, // Deriving at input 0
                                            ( a, b ) -> a  // deriving input 1
                                        ))
                                        .with(Fun.F32F32ToF32.triple(
                                                ( a, b ) -> a * b,
                                                ( a, b ) -> b, // Deriving at input 0
                                                ( a, b ) -> a  // deriving input 1
                                        ))
                                        .get()
                                        .run( call );
                        }
                    )
            )
            .setImplementationFor(
                OpenCLDevice.class,
                CLImplementation
                    .compiler()
                    .arity( 3 )
                    .kernelSource( scalarization.getKernelSource() )
                    .activationSource( "output = input1 * value;\n" )
                    .differentiationSource( "if ( d == 0 ) {output = value;}else{output = input1;}\n" )
                    .kernelPostfix( this.getFunction() )
                    .execution(
                        call -> {
                            if ( call.getDerivativeIndex() == 0 )
                                call.getTensors()[0] = call.getTensors()[2].shallowCopy().getUnsafe().setIsIntermediate( true );
                            else if ( call.getDerivativeIndex() == 1 )
                                call.getTensors()[0] = call.getTensors()[1].shallowCopy().getUnsafe().setIsIntermediate( true );
                            else {
                                int offset = (call.getTsrOfType(Number.class, 2).isVirtual() || call.getTsrOfType(Number.class, 2).size() == 1) ? 1 : 0;
                                int gwz = call.getTsrOfType(Number.class, 0).size();
                                call.getDevice()
                                    .getKernel(call)
                                    .passAllOf(call.getTsrOfType(Number.class, 0))
                                    .passAllOf(call.getTsrOfType(Number.class, 0 + offset))
                                    .pass((float) call.getTsrOfType(Number.class, 1 + offset).getDataAs( double[].class )[0])
                                    .pass(call.getTsrOfType(Number.class, 0).rank())
                                    .pass(call.getValOf(Arg.DerivIdx.class))
                                    .call(gwz);
                            }
                        }
                    )
                    .build()
            )
        );

    }


    @Contract(pure = true)
    @Override
    public String stringify( String[] children ) {
        StringBuilder reconstructed = new StringBuilder();
        for ( int i = 0; i < children.length; ++i ) {
            reconstructed.append( children[ i ] );
            if ( i < children.length - 1 ) {
                reconstructed.append(" * ");
            }
        }
        return "(" + reconstructed + ")";
    }

    @Override
    public String asDerivative( Function[] children, int derivationIndex) {
        return Arrays.stream( children )
                .filter( child -> child.dependsOn(derivationIndex) )
                .map( child -> {
                            String derivative = child.getDerivative(derivationIndex).toString();
                            return ( (derivative.equals("1.0") ) ? "" : " * " ) +
                                    Arrays.stream( children )
                                            .filter( inner -> inner != child )
                                            .map( Object::toString )
                                            .collect( Collectors.joining( " * " ) );
                        }
                )
                .map( Object::toString )
                .collect( Collectors.joining( " + " ) );
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) {
        if ( j < 0 ) return calculate( inputs, d, src );
        if ( d < 0 ) {
            double result = src[ 0 ].call( inputs, j );
            for ( int i = 1; i < src.length; i++ ) {
                final double current = src[ i ].call( inputs, j );
                result *= current;
            }
            return result;
        } else {
            double u, ud, v, vd;
            u = src[ 0 ].call( inputs, j );
            ud = src[ 0 ].derive( inputs, d, j );

            for ( int ji = 1; ji < src.length; ji++ ) {
                v = src[ ji ].call( inputs, j );
                vd = src[ ji ].derive( inputs, d, j );
                ud = u * vd + v * ud;
                u *= v;
            }
            return ud;
        }
    }

    @Contract(pure = true)
    public static double calculate( double[] inputs, int d, Function[] src ) {
        if ( d < 0 ) {
            double result = src[ 0 ].call( inputs );
            for ( int i = 1; i < src.length; i++ ) {
                final double current = src[ i ].call( inputs );
                result *= current;
            }
            return result;
        } else {
            double u, ud, v, vd;
            u = src[ 0 ].call( inputs );
            ud = src[ 0 ].derive( inputs, d );
            for ( int j = 1; j < src.length; j++ ) {
                v = src[ j ].call( inputs );
                vd = src[ j ].derive( inputs, d );

                ud = u * vd + v * ud;
                u *= v; // ...this step can be avoided (TODO optimize)
            }
            return ud;
        }
    }




}
