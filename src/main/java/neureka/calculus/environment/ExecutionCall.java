package neureka.calculus.environment;

import neureka.Tsr;
import neureka.acceleration.Device;
import neureka.calculus.environment.implementations.AbstractOperationTypeImplementation;

/**
 * This class is a simple container holding relevant
 * arguments needed to execute on a targeted Device which
 * is specified by the type parameter below.
 *
 * @param <DeviceType> The Device implementation targeted by an instance of this ExecutionCall!
 */
public class ExecutionCall< DeviceType extends Device >
{
    public interface Mutator {
        Tsr[] mutate( Tsr[] tensors );
    }

    private final DeviceType _device;
    private Tsr[] _tsrs;
    private final int _d;
    private int _j = -1;
    private final OperationType _type;
    private OperationTypeImplementation<AbstractOperationTypeImplementation> _implementation;

    public ExecutionCall(
            DeviceType device,
            Tsr[] tsrs,
            int d,
            OperationType type
    ) {
        _device = device;
        _tsrs = tsrs;
        _d = d;
        _type = type;
        _implementation = null;
    }
    public ExecutionCall(
            DeviceType device,
            Tsr[] tsrs,
            int d,
            int j,
            OperationType type
    ) {
        _device = device;
        _tsrs = tsrs;
        _d = d;
        _j = j;
        _type = type;
        _implementation = null;
    }
    public int getJ() {
        return _j;
    }
    public DeviceType getDevice() {return _device;}
    public Tsr[] getTensors() {return _tsrs;}
    public Tsr getTensor(int i) {return _tsrs[i];}
    public int getDerivativeIndex() {return _d;}
    public OperationType getType() {return _type;}
    public OperationTypeImplementation getImplementation() {
        if ( _implementation != null ) return _implementation;
        else _implementation = _type.implementationOf(this);
        return _implementation;
    }
    public boolean allowsForward(){
        return getImplementation().getADAnalyzer().allowsForward(this);
    }
    public void mutateArguments(Mutator mutation){
        _tsrs = mutation.mutate(_tsrs);
    }
    public ExecutionCall<DeviceType> withNew(Tsr[] tensors){
        return new ExecutionCall<>(_device, tensors, _d, _j, _type);
    }
}