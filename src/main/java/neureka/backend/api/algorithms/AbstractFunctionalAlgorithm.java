package neureka.backend.api.algorithms;


import neureka.Tsr;
import neureka.autograd.ADAgent;
import neureka.backend.api.Algorithm;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.Operation;
import neureka.backend.api.algorithms.api.*;
import neureka.calculus.Function;
import neureka.calculus.implementations.FunctionNode;
import neureka.devices.Device;

/**
 *  This is the base class for implementations of the {@link Algorithm} interface.
 *  The class implements a basic component system, as is implicitly expected by said interface.
 *  Additionally it contains useful methods used to process passed arguments of {@link ExecutionCall}
 *  as well as an implementation of the {@link Algorithm} interface which allows its methods to
 *  be implemented in a functional programming style, meaning that instances of concrete implementations
 *  extending this abstract class have setters for lambdas representing the {@link Algorithm} methods.
 *  It is being used by the standard backend of Neureka as abstract base class for various algorithms.
 *
 *  Conceptually an implementation of the {@link Algorithm} interface represents "a sub-kind of operation" for
 *  an instance of an implementation of the {@link Operation} interface. <br>
 *  The "+" operator for example has different {@link Algorithm} instances tailored to specific requirements
 *  originating from different {@link ExecutionCall} instances with unique arguments.
 *  {@link Tsr} instances within an execution call having the same shape would
 *  cause the {@link Operation} instance to chose an {@link Algorithm} instance which is responsible
 *  for performing elementwise operations, whereas otherwise the {@link neureka.backend.standard.algorithms.Broadcast}
 *  algorithm might be called to perform the operation.
 *
 * @param <C> The final type extending this class.
 */
public abstract class AbstractFunctionalAlgorithm< C extends Algorithm<C> > extends AbstractBaseAlgorithm<C>
{
    private SuitabilityChecker _isSuitableFor;
    private DeviceFinder _findDeviceFor;
    private ForwardADChecker _canPerformForwardADFor;
    private BackwardADChecker _canPerformBackwardADFor;
    private ADAgentSupplier _supplyADAgentFor;
    private InitialCallHook _handleInsteadOfDevice;
    private RecursiveExecutor _handleRecursivelyAccordingToArity = (call, goDeeperWith) -> null;
    private DrainInstantiation _instantiateNewTensorsForExecutionIn;

    public AbstractFunctionalAlgorithm(String name ) {
        super(name);
    }


    //---

    @Override
    public float isSuitableFor( ExecutionCall<? extends Device<?>> call ) {
        return _isSuitableFor.isSuitableFor(call);
    }

    ///---

    @Override
    public Device<?> findDeviceFor( ExecutionCall<? extends Device<?>> call ) {
        return ( _findDeviceFor == null ) ? null : _findDeviceFor.findDeviceFor(call);
    }

    //---

    @Override
    public boolean canPerformForwardADFor( ExecutionCall<? extends Device<?>> call ) {
        return _canPerformForwardADFor.canPerformForwardADFor(call);
    }

    //---

    @Override
    public boolean canPerformBackwardADFor( ExecutionCall<? extends Device<?>> call ) {
        return _canPerformBackwardADFor.canPerformBackwardADFor( call );
    }

    //---

    @Override
    public ADAgent supplyADAgentFor( Function f, ExecutionCall<? extends Device<?>> call, boolean forward ) {
        return _supplyADAgentFor.supplyADAgentFor( f, call, forward );
    }

    //---

    @Override
    public Tsr<?> handle(FunctionNode caller, ExecutionCall<? extends Device<?>> call ) {
        return _handleInsteadOfDevice.handle( caller, call );
    }

    //---

    @Override
    public Tsr<?> execute(ExecutionCall<? extends Device<?>> call, CallExecutor goDeeperWith ) {
        return _handleRecursivelyAccordingToArity.execute( call, goDeeperWith );
    }

    //---

    @Override
    public ExecutionCall<? extends Device<?>> handle(ExecutionCall<? extends Device<?>> call ) {
        return _instantiateNewTensorsForExecutionIn.handle( call );
    }

    //---

    public C build() { return (C) this; }

    /**
     *  The {@link SuitabilityChecker}
     *  checks if a given instance of an {@link ExecutionCall} is
     *  suitable to be executed in {@link neureka.backend.api.ImplementationFor}
     *  residing in this {@link Algorithm} as components.
     *
     * @return A lambda which checks if a given {@link ExecutionCall} instance is suitable to be executed by this {@link Algorithm}.
     */
    public SuitabilityChecker getIsSuitableFor() { return this._isSuitableFor; }

    /**
     *  The {@link neureka.backend.api.algorithms.api.DeviceFinder} finds
     *  a {@link Device} instance which fits the contents of a given {@link ExecutionCall} instance.
     *  The finder is supposed to find a {@link Device} which can be most easily shared
     *  by the {@link Tsr} instances within the {@link ExecutionCall} that is being received by the finder.
     *
     * @return A finder lambda for finding a suitable {@link Device} implementation instance for a given {@link ExecutionCall} passed to the finder.
     */
    public DeviceFinder getFindDeviceFor() { return this._findDeviceFor; }

    /**
     *  A {@link ForwardADChecker} lambda checks if this
     *  {@link Algorithm} can perform forward AD for a given {@link ExecutionCall}.
     *
     * @return An analyzer which return a truth value determining if this {@link Algorithm} can perform forward AD on a given {@link ExecutionCall}
     */
    public ForwardADChecker getCanPerformForwardADFor() { return this._canPerformForwardADFor; }

    /**
     *  A {@link BackwardADChecker} lambda checks if this
     *  {@link Algorithm} can perform backward AD for a given {@link ExecutionCall}.
     *
     * @return An analyzer which return a truth value determining if this {@link Algorithm} can perform backward AD on a given {@link ExecutionCall}
     */
    public BackwardADChecker getCanPerformBackwardADFor() { return this._canPerformBackwardADFor; }

    /**
     *  This {@link neureka.backend.api.algorithms.api.ADAgentSupplier} will supply
     *  {@link ADAgent} instances which can perform backward and forward auto differentiation.
     *
     * @return An {@link neureka.backend.api.algorithms.api.ADAgentSupplier} for creting suitable {@link ADAgent} instances.
     */
    public ADAgentSupplier getSupplyADAgentFor() { return this._supplyADAgentFor; }

    /**
     *  The {@link neureka.backend.api.algorithms.api.InitialCallHook} lambda
     *  is simply a bypass procedure which if provided will simply occupy
     *  the rest of the execution without any other steps being taken.
     *  For example the {@link RecursiveExecutor}
     *  would not be used in that case.
     *  This bypassing is useful for unorthodox types of operations
     *  like the {@link neureka.backend.standard.operations.other.Reshape} opertion.
     *
     * @return A lambda which bypasses the default execution procedure.
     */
    public InitialCallHook getHandleInsteadOfDevice() { return this._handleInsteadOfDevice; }

    /**
     *  This returns an instance of the {@link RecursiveExecutor}
     *  which enables the pairwise execution of a chain of an abitrary number of
     *  {@link ExecutionCall} arguments.
     *  This is useful for simple operators like '+' or '*' which
     *  might have any number of arguments...
     *
     * @return A lambda which will be executed recursively to executes multiple arguments pairwise.
     */
    public RecursiveExecutor getHandleRecursivelyAccordingToArity() { return this._handleRecursivelyAccordingToArity; }

    /**
     *  An {@link Algorithm} will typically produce a result when executing an {@link ExecutionCall}.
     *  This result must be created somehow.
     *  The {@link neureka.backend.api.algorithms.api.DrainInstantiation} lambda instance
     *  returned by this method will do just that...
     *
     * @return A result instantiation lambda called before execution...
     */
    public DrainInstantiation getInstantiateNewTensorsForExecutionIn() { return this._instantiateNewTensorsForExecutionIn; }

    /**
     *  The {@link SuitabilityChecker}
     *  checks if a given instance of an {@link ExecutionCall} is
     *  suitable to be executed in {@link neureka.backend.api.ImplementationFor} instances
     *  residing in this {@link Algorithm} as components.
     *
     * @return This very instance to enable method chaining.
     */
    public AbstractFunctionalAlgorithm<C> setIsSuitableFor( SuitabilityChecker isSuitableFor ) {
        this._isSuitableFor = isSuitableFor;
        return this;
    }

    /**
     *  The {@link neureka.backend.api.algorithms.api.DeviceFinder} finds
     *  a {@link Device} instance which fits the contents of a given {@link ExecutionCall} instance.
     *  The finder is supposed to find a {@link Device} which can be most easily shared
     *  by the {@link Tsr} instances within the {@link ExecutionCall} that is being received by the finder.
     *
     * @param findDeviceFor The finder lambda which ought to find a suitable device for a given {@link ExecutionCall}.
     * @return This very instance to enable method chaining.
     */
    public AbstractFunctionalAlgorithm<C> setFindDeviceFor( DeviceFinder findDeviceFor ) {
        this._findDeviceFor = findDeviceFor;
        return this;
    }

    /**
     *  A {@link ForwardADChecker} lambda checks if this
     *  {@link Algorithm} can perform forward AD for a given {@link ExecutionCall}.
     *
     * @param canPerformForwardADFor
     * @return This very instance to enable method chaining.
     */
    public AbstractFunctionalAlgorithm<C> setCanPerformForwardADFor( ForwardADChecker canPerformForwardADFor ) {
        this._canPerformForwardADFor = canPerformForwardADFor;
        return this;
    }

    /**
     *  A {@link BackwardADChecker} lambda checks if this
     *  {@link Algorithm} can perform backward AD for a given {@link ExecutionCall}.
     *
     * @param canPerformBackwardADFor
     * @return This very instance to enable method chaining.
     */
    public AbstractFunctionalAlgorithm<C> setCanPerformBackwardADFor( BackwardADChecker canPerformBackwardADFor ) {
        this._canPerformBackwardADFor = canPerformBackwardADFor;
        return this;
    }

    /**
     *  This method receives a {@link neureka.backend.api.algorithms.api.ADAgentSupplier} which will supply
     *  {@link ADAgent} instances which can perform backward and forward auto differentiation.
     *
     * @param supplyADAgentFor
     * @return This very instance to enable method chaining.
     */
    public AbstractFunctionalAlgorithm<C> setSupplyADAgentFor( ADAgentSupplier supplyADAgentFor ) {
        this._supplyADAgentFor = supplyADAgentFor;
        return this;
    }

    public AbstractFunctionalAlgorithm<C> setHandleInsteadOfDevice( InitialCallHook handleInsteadOfDevice ) {
        this._handleInsteadOfDevice = handleInsteadOfDevice;
        return this;
    }

    public AbstractFunctionalAlgorithm<C> setHandleRecursivelyAccordingToArity( RecursiveExecutor handleRecursivelyAccordingToArity ) {
        this._handleRecursivelyAccordingToArity = handleRecursivelyAccordingToArity;
        return this;
    }

    public AbstractFunctionalAlgorithm<C> setInstantiateNewTensorsForExecutionIn( DrainInstantiation instantiateNewTensorsForExecutionIn ) {
        this._instantiateNewTensorsForExecutionIn = instantiateNewTensorsForExecutionIn;
        return this;
    }
}


