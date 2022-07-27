package neureka.backend.main.implementations;

import neureka.backend.FunImplementationFor;
import neureka.backend.api.ExecutionCall;
import neureka.devices.opencl.KernelCode;
import neureka.devices.opencl.KernelSource;
import neureka.devices.opencl.OpenCLDevice;

public class AdHocClImplementation extends CLImplementation {

    private final KernelSource _kernelProvider;

    protected AdHocClImplementation(FunImplementationFor<OpenCLDevice> execution, int arity, KernelSource kernelProvider) {
        super(execution, arity);
        _kernelProvider = kernelProvider;
    }

    @Override
    public KernelCode getKernelFor(ExecutionCall<OpenCLDevice> call) {
        return _kernelProvider.getKernelFor( call );
    }
}
