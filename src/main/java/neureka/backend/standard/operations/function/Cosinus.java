package neureka.backend.standard.operations.function;

import neureka.backend.standard.algorithms.Fun;
import neureka.backend.api.operations.AbstractOperation;
import neureka.backend.api.operations.OperationBuilder;
import neureka.backend.standard.algorithms.Activation;
import neureka.backend.standard.implementations.CPUImplementation;
import neureka.calculus.Function;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import org.jetbrains.annotations.Contract;

public final class Cosinus extends AbstractOperation
{
    public Cosinus()
    {
        super (
            new OperationBuilder()
                .setFunction(         "cos"  )
                .setOperator(         "cos"  )
                .setArity(            1      )
                .setIsOperator(       false  )
                .setIsIndexer(        false  )
                .setIsDifferentiable( true   )
                .setIsInline(         false  )
        );

        Activation operationAlgorithm = new Activation()
            .setSupplyADAgentFor( getDefaultAlgorithm() )
            .buildFunAlgorithm();

        setAlgorithm(
            Activation.class,
            operationAlgorithm.setImplementationFor(
                CPU.class,
                Activation.implementationForCPU()
                    .with(Fun.F64ToF64.pair(
                            x -> Math.cos(x),
                            x -> -Math.sin(x)
                    ) )
                    .with(Fun.F32ToF32.pair(
                            x -> (float) Math.cos(x),
                            x -> (float) -Math.sin(x)
                    ))
                    .get()
            )
            .setImplementationFor(
                OpenCLDevice.class,
                    Activation.implementationForGPU( this.getFunction() )
                            .with( "output = cos( input );\n" )
                            .and( "output = -sin( input );\n" )
            )
        );
    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return "cos" + expression;
        return "cos" + "(" + expression + ")";
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
    public static double calculate( double input, boolean derive ) {
        if ( !derive ) return Math.cos( input );
        else return -Math.sin( input );
    }


}
