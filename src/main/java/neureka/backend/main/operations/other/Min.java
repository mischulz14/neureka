package neureka.backend.main.operations.other;

import neureka.Tsr;
import neureka.backend.api.AutoDiffMode;
import neureka.backend.api.DeviceAlgorithm;
import neureka.backend.api.Result;
import neureka.backend.api.template.algorithms.AbstractDeviceAlgorithm;
import neureka.backend.api.template.operations.AbstractOperation;
import neureka.backend.api.template.operations.OperationBuilder;
import neureka.backend.main.operations.ElemWiseUtil;
import neureka.backend.main.operations.linear.internal.opencl.CLReduce;
import neureka.backend.main.operations.other.internal.CPUReduce;
import neureka.calculus.Function;
import neureka.devices.Device;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;

public class Min extends AbstractOperation
{
    public Min()
    {
        super(
                new OperationBuilder()
                        .identifier(       "min"       )
                        .operator(         "min"       )
                        .arity(            1           )
                        .isOperator(       false       )
                        .isIndexer(        false       )
                        .isDifferentiable( true        )
                        .isInline(         false       )
        );

        setAlgorithm(
            DeviceAlgorithm
            .withName("min_algorithm")
            .setIsSuitableFor(
                    call -> call.validate()
                            .allNotNull( t -> Number.class.isAssignableFrom(t.getItemType()) )
                            .basicSuitability()
            )
            .setAutogradModeFor( call -> AutoDiffMode.BACKWARD_ONLY )
            .setExecution( (caller, call) -> {
                Tsr<?>[] inputs = AbstractDeviceAlgorithm.flatten(caller, call).inputs();
                call = call.withInputs(inputs);
                Tsr<Integer> index = ((DeviceAlgorithm)call.getAlgorithm()).getImplementationFor(call.getDevice()).run(call);
                int i = index.item(0);
                Tsr<?> in = inputs[0] == null ? inputs[1] : inputs[0];
                Class<Object> typeClass = (Class<Object>) in.itemType();
                int[] shape = in.getNDConf().shape();
                Device<Object> device = (Device<Object>) call.getDevice();
                return Result.of(
                            Tsr.of(in.itemType(), new int[]{1}, in.item(i)).to(call.getDevice()).mut().setIsIntermediate(true)
                        )
                        .withADAction( target -> {
                            Tsr<Object> error = (Tsr<Object>) target.error();
                            assert error.size() == 1;
                            Tsr<Object> newError = ElemWiseUtil.newTsrLike(typeClass, shape, true, device, 0);
                            newError.mut().setIsVirtual(false);
                            newError.mut().setItemAt(i, error.item(0));
                            return newError;
                        });
            })
            .setCallPreparation( call ->
            {
                if ( call.input( 0 ) == null )
                    call = call.withInputAt( 0, call.input( 1 ) );

                return call;
            })
            .buildFunAlgorithm()
            .setImplementationFor( CPU.class, new CPUReduce(CPUReduce.Type.MIN) )
            .setImplementationFor( OpenCLDevice.class, new CLReduce(CLReduce.Type.MIN) )
        );
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) { return src[ 0 ].call( inputs, j ); }
}
