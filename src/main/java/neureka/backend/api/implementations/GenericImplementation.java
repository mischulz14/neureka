package neureka.backend.api.implementations;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import neureka.Tsr;
import neureka.autograd.ADAgent;
import neureka.autograd.DefaultADAgent;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.operations.OperationType;
import neureka.calculus.Function;
import neureka.calculus.frontend.AbstractFunction;
import neureka.calculus.frontend.assembly.FunctionBuilder;
import neureka.devices.Device;
import neureka.devices.host.execution.HostExecutor;
import neureka.dtype.NumericType;

import java.util.Arrays;

public class GenericImplementation extends AbstractBaseOperationTypeImplementation<OperationTypeImplementation>{

    public GenericImplementation( String name, int arity,  OperationType type ) {
        super(name);
        setExecutor(
                HostExecutor.class,
                new HostExecutor(
                        call -> {
                            Function f = FunctionBuilder.build( type, call.getTensors().length-1, false);
                            boolean allNumeric = call.validate()
                                    .all( t -> t.getDataType().typeClassImplements(NumericType.class) )
                                    .isValid();

                            if ( allNumeric )
                            {
                                double[] inputs = new double[call.getTensors().length-1];
                                call
                                        .getDevice()
                                        .getExecutor()
                                        .threaded (
                                                call.getTensor( 0 ).size(),
                                                ( start, end ) -> {
                                                    for ( int i = start; i < end; i++ ) {
                                                        for ( int ii = 0; ii < inputs.length; ii++ ) {
                                                            inputs[ii] = call.getTensor(1+ii).value64( i );
                                                        }
                                                        call.getTensor( 0 ).value64()[ i ] = f.call( inputs );
                                                    }
                                                }
                                        );
                            } else {
                                Object[] inputs = new Object[ call.getTensors().length-1 ];
                                String expression = f.toString();
                                Binding binding = new Binding();
                                binding.setVariable("I", inputs);
                                GroovyShell shell = new GroovyShell(binding);
                                call
                                        .getDevice()
                                        .getExecutor()
                                        .threaded (
                                                call.getTensor( 0 ).size(),
                                                ( start, end ) -> {
                                                    for ( int i = start; i < end; i++ ) {
                                                        for ( int ii = 0; ii < inputs.length; ii++ ) {
                                                            inputs[ii] = call.getTensor(1+ii).getElement(i);
                                                        }
                                                        call.getTensor( 0 ).setAt(i, shell.evaluate( expression ) );
                                                    }
                                                }
                                        );

                            }
                        },
                        arity
                )
        );
    }

    @Override
    public float isImplementationSuitableFor(ExecutionCall call) {
        int[] shape = null;
        for ( Tsr<?> t : call.getTensors() ) {
            if ( shape == null ) if ( t != null ) shape = t.getNDConf().shape();
            else if ( t != null && !Arrays.equals( shape, t.getNDConf().shape() ) ) return 0.0f;
        }
        return 1.0f;
    }

    /**
     * @param call The execution call which has been routed to this implementation...
     * @return null because the default implementation is not outsourced.
     */
    @Override
    public Device findDeviceFor(ExecutionCall call ) {
        return null;
    }

    @Override
    public boolean canImplementationPerformForwardADFor(ExecutionCall call) {
        return true;
    }

    @Override
    public boolean canImplementationPerformBackwardADFor(ExecutionCall call) {
        return true;
    }

    @Override
    public ADAgent supplyADAgentFor(Function f, ExecutionCall<Device> call, boolean forward)
    {
        Tsr<?> ctxDerivative = (Tsr<?>) call.getAt("derivative");
        Function mul = Function.Detached.MUL;
        if ( ctxDerivative != null ) {
            return new DefaultADAgent( ctxDerivative )
                    .withForward( ( node, forwardDerivative ) -> mul.call( new Tsr[]{forwardDerivative, ctxDerivative} ) )
                    .withBackward( ( node, backwardError ) -> mul.call( new Tsr[]{backwardError, ctxDerivative} ) );
        }
        Tsr<?> localDerivative = f.derive(call.getTensors(), call.getDerivativeIndex());
        return new DefaultADAgent( localDerivative )
                .withForward( ( node, forwardDerivative ) -> mul.call(new Tsr[]{forwardDerivative, localDerivative}) )
                .withBackward( ( node, backwardError ) -> mul.call(new Tsr[]{backwardError, localDerivative}) );
    }

    @Override
    public Tsr handleInsteadOfDevice(AbstractFunction caller, ExecutionCall call) {
        return null;
    }

    @Override
    public Tsr handleRecursivelyAccordingToArity(ExecutionCall call, java.util.function.Function<ExecutionCall, Tsr> goDeeperWith)
    {
        return null;
    }

    @Override
    public ExecutionCall instantiateNewTensorsForExecutionIn(ExecutionCall call)
    {
        Tsr[] tensors = call.getTensors();
        Device device = call.getDevice();
        if ( tensors[ 0 ] == null ) // Creating a new tensor:
        {
            int[] shp = tensors[ 1 ].getNDConf().shape();
            Tsr output = new Tsr( shp, tensors[ 1 ].getDataType() );
            output.setIsVirtual( false );
            try {
                device.store(output);
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            tensors[ 0 ] = output;
        }
        return call;
    }

}
