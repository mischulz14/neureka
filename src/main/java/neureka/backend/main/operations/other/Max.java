package neureka.backend.main.operations.other;

import neureka.Tsr;
import neureka.backend.api.AutoDiffMode;
import neureka.backend.api.DeviceAlgorithm;
import neureka.backend.api.Result;
import neureka.backend.api.template.algorithms.AbstractDeviceAlgorithm;
import neureka.backend.api.template.operations.AbstractOperation;
import neureka.backend.api.template.operations.OperationBuilder;
import neureka.backend.main.operations.linear.internal.opencl.CLReduce;
import neureka.backend.main.operations.other.internal.CPUReduce;
import neureka.calculus.Function;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import neureka.ndim.config.types.simple.Simple2DConfiguration;

public class Max extends AbstractOperation
{
    public Max()
    {
        super(
            new OperationBuilder()
                .setIdentifier(       "max"       )
                .setOperator(         "max"       )
                .setArity(            1           )
                .setIsOperator(       false       )
                .setIsIndexer(        false       )
                .setIsDifferentiable( true        )
                .setIsInline(         false       )
        );

        setAlgorithm(
            DeviceAlgorithm
            .withName("max_algorithm")
            .setIsSuitableFor(
                call -> call.validate()
                            .allNotNull( t -> Number.class.isAssignableFrom(t.getItemType()) )
                            .getEstimator()
                                .goodIfAnyNonNull( t -> t.getNDConf() instanceof Simple2DConfiguration)
                                .badIfAnyNonNull( t -> !( t.getNDConf() instanceof Simple2DConfiguration) )
                                .getEstimation()
            )
            .setAutogradModeFor( call -> AutoDiffMode.BACKWARD_ONLY )
            .setExecution( (caller, call) -> {
                Tsr<?>[] inputs = AbstractDeviceAlgorithm.flatten(caller, call).inputs();
                Tsr<Integer> index = ((DeviceAlgorithm)call.getAlgorithm()).getImplementationFor(call.getDevice()).run(call);
                int i = index.item(0);
                Tsr<?> in = inputs[0] == null ? inputs[1] : inputs[0];
                return Result.of(
                            Tsr.of(in.itemType(), new int[]{1}, in.item(i)).to(call.getDevice()).getUnsafe().setIsIntermediate(true)
                        )
                        .withADAction( target -> {
                            return null;//TODO
                        });
            })
            .setCallPreparation( call ->
             {
                 if ( call.input( 0 ) == null )
                     call = call.withInputAt( 0, call.input( 1 ) );

                 return call;
             })
            .buildFunAlgorithm()
            .setImplementationFor( CPU.class, new CPUReduce(CPUReduce.Type.MAX) )
            .setImplementationFor( OpenCLDevice.class, new CLReduce(CLReduce.Type.MAX) )
        );
    }

    @Override
    public String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.charAt( 0 ) == '(' && expression.charAt( expression.length() - 1 ) == ')' ) {
            return "max" + expression;
        }
        return "max" + "(" + expression + ")";
    }

    @Override
    public String asDerivative( Function[] children, int derivationIndex) {
        throw new IllegalStateException("Operation does not support dynamic derivation!");
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) { return src[ 0 ].call( inputs, j ); }
}
