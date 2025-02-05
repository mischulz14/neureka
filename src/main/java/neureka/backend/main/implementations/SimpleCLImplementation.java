package neureka.backend.main.implementations;

import neureka.backend.api.ImplementationFor;
import neureka.backend.api.ExecutionCall;
import neureka.devices.opencl.KernelCode;
import neureka.devices.opencl.OpenCLDevice;

public class SimpleCLImplementation extends CLImplementation
{
    private final KernelCode _kernel;

    protected SimpleCLImplementation(
            ImplementationFor<OpenCLDevice> execution,
            int arity,
            String kernelName,
            String kernelSource
    ) {
        super(execution, arity);
        _kernel = new KernelCode( kernelName, kernelSource );
    }

    @Override
    public KernelCode getKernelFor( ExecutionCall<OpenCLDevice> call ) {
        return _kernel;
    }

}
