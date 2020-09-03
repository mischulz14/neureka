package neureka.calculus.environment;

import neureka.Tsr;
import neureka.acceleration.Device;
import neureka.autograd.ADAgent;
import neureka.calculus.Function;

import java.util.Map;
import java.util.TreeMap;

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
    private OperationTypeImplementation<OperationTypeImplementation> _implementation;

    private Map<String, Object> _context;

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
        _context = null;
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
    
    public ADAgent getADAgentFrom(Function function, Tsr derivative, ExecutionCall<Device> call, boolean forward ) {
        if ( this._context != null ) {
            if ( call._context ==null ) call._context = new TreeMap<>();
            call._context.putAll(this._context);
        }
        if( derivative != null ) assert (call._context != null && call._context.containsKey("derivative"));
        else assert call._context == null;
        return getImplementation().getADAgentCreator().getADAgentOf(function, call, forward);
    }
    
    public void mutateArguments(Mutator mutation){
        _tsrs = mutation.mutate(_tsrs);
    }
    
    public ExecutionCall<DeviceType> withNew(Tsr[] tensors) {
        return new ExecutionCall<DeviceType>(_device, tensors, _d, _j, _type);
    }
    
    public <T> T getAt(Class<T> type){
        if ( _context == null ) return null;
        return (T) _context.get(getClass().getName());
    }

    public Object getAt(String varName){
        if ( _context == null ) return null;
        return _context.get(varName);
    }

    public ExecutionCall<DeviceType> putAt(String s, Tsr o){
        if ( _context == null ) _context = new TreeMap<>();
        _context.put(s,o);
        return this;
    }

    public Map<String, Object> getContext(){
        return _context;
    }

    public void takeContext( Map<String, Object>  context ){
        if(_context==null && context!=null )_context = new TreeMap<>();
        if(context!=null) _context.putAll(_context);
    }

}