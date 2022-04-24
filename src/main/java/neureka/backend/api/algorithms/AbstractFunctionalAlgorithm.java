package neureka.backend.api.algorithms;


import neureka.Tsr;
import neureka.autograd.ADAgent;
import neureka.backend.api.Algorithm;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.Operation;
import neureka.backend.api.algorithms.fun.*;
import neureka.backend.standard.memory.MemValidator;
import neureka.calculus.Function;
import neureka.calculus.implementations.FunctionNode;
import neureka.calculus.internal.RecursiveExecutor;
import neureka.devices.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *  This is the base class for implementations of the {@link Algorithm} interface.
 *  The class implements a basic component system, as is implicitly expected by said interface.
 *  Additionally, it contains useful methods used to process passed arguments of {@link ExecutionCall}
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
public abstract class AbstractFunctionalAlgorithm<C extends Algorithm<C>>
extends AbstractBaseAlgorithm<C> implements ExecutionPreparation
{
    private static final Logger _LOG = LoggerFactory.getLogger( AbstractFunctionalAlgorithm.class );
    /*
        Consider the following lambdas as effectively immutable because this
        class will warn us if any field variable is set for a second time.
        This makes the backend somewhat hackable, but also manageable with respect to complexity.
     */
    private SuitabilityPredicate _isSuitableFor;
    private ADSupportPredicate   _autogradModeFor;
    private Execution            _execution;
    private ADAgentSupplier      _supplyADAgentFor;
    private ExecutionPreparation _instantiateNewTensorsForExecutionIn;
    /*
        This flag will ensure that we can warn the user that the state has been illegally modified.
     */
    private boolean _isFullyBuilt = false;


    public AbstractFunctionalAlgorithm( String name ) { super(name); }

    /**
     *  The {@link SuitabilityPredicate} checks if a given instance of an {@link ExecutionCall} is
     *  suitable to be executed in {@link neureka.backend.api.ImplementationFor}
     *  residing in this {@link Algorithm} as components.
     *  It can be implemented as s simple lambda.
     */
    @Override
    public final float isSuitableFor( ExecutionCall<? extends Device<?>> call ) {
        _checkReadiness();
        return _isSuitableFor.isSuitableFor(call);
    }

    /**
     *  This method returns a new instance
     *  of the {@link ADAgent} class responsible for performing automatic differentiation
     *  both for forward and backward mode differentiation. <br>
     *  Therefore an {@link ADAgent} exposes 2 different procedures. <br>
     *  One is the forward mode differentiation, and the other one <br>
     *  is the backward mode differentiation which is more commonly known as back-propagation... <br>
     *  Besides that it may also contain context information used <br>
     *  to perform said procedures.
     */
    @Override
    public final ADAgent supplyADAgentFor(Function function, ExecutionCall<? extends Device<?>> call, boolean forward ) {
        _checkReadiness();
        return _supplyADAgentFor.supplyADAgentFor(function, call, forward );
    }

    /**
     *  Preparing refers to instantiating output tensors for the provided {@link ExecutionCall}.
     *  
     * @param call The execution call which needs to be prepared for execution.
     * @return The prepared {@link ExecutionCall} instance.
     */
    @Override
    public final ExecutionCall<? extends Device<?>> prepare( ExecutionCall<? extends Device<?>> call ) {
        _checkReadiness();
        if ( call != null ) {
            Tsr<?>[] inputs = call.inputs().clone();
            ExecutionCall<? extends Device<?>> prepared = _instantiateNewTensorsForExecutionIn.prepare(call);
            Arrays.stream(prepared.inputs())
                    .filter(
                            out -> Arrays.stream(inputs)
                                    .noneMatch(in -> in == out)
                    )
                    .forEach(t -> t.getUnsafe().setIsIntermediate(true));

            return prepared;
        }
        else return null;
    }

    /**
     * @return A new concrete implementation of the {@link AbstractFunctionalAlgorithm} which
     *         is fully built and ready to be used as an {@link Operation} component.
     */
    public final C buildFunAlgorithm() {
        if (
            _isSuitableFor == null ||
            _autogradModeFor == null ||
            (_supplyADAgentFor == null && _execution == null) ||
            _execution == null ||
            _instantiateNewTensorsForExecutionIn == null
        ) {
            throw new IllegalStateException(
                    "Instance '"+getClass().getSimpleName()+"' incomplete!"
            );
        }

        _isFullyBuilt = true;
        return (C) this;
    }

    /**
     *  This method ensures that this algorithm was fully supplied with all the
     *  required lambdas...
     */
    private void _checkReadiness() {
        if ( !_isFullyBuilt ) {
            throw new IllegalStateException(
                "Trying use an instance of '"+this.getClass().getSimpleName()+"' with name '" + getName() + "' " +
                "which was not fully built!"
            );
        }
    }

    /**
     *  Neureka is supposed to be extremely modular and in a sense its backend should be "hackable" to a degree.
     *  However, this comes with a lot of risk, because it requires us to expose mutable state, which is not good.
     *  This class is semi-immutable, by simply warning us about any mutations after building was completed!
     *
     * @param newState The state which will be set.
     * @param <T> The type of the thing which is supposed to be set.
     * @param current The state which is currently set.
     * @return The checked thing.
     */
    private <T> T _checked( T newState, T current, Class<T> type ) {
        if ( _isFullyBuilt )
            _LOG.warn(
                "Implementation '" + type.getSimpleName() + "' in algorithm '"+this+"' was modified! " +
                "Please consider only modifying the standard backend state of Neureka for experimental reasons."
            );
        else if ( current != null && newState == null )
            throw new IllegalArgumentException(
                "Trying set an already specified implementation of lambda '"+current.getClass().getSimpleName()+"' to null!"
            );

        return newState;
    }

    /**
     *  The {@link SuitabilityPredicate} received by this method 
     *  checks if a given instance of an {@link ExecutionCall} is
     *  suitable to be executed in {@link neureka.backend.api.ImplementationFor} instances
     *  residing in this {@link Algorithm} as components.
     *  The lambda will be called by the {@link #isSuitableFor(ExecutionCall)} method
     *  by any given {@link Operation} instances this algorithm belongs to.
     *
     * @param isSuitableFor The suitability predicate which determines if the algorithm is suitable or not.
     * @return This very instance to enable method chaining.
     */
    public final AbstractFunctionalAlgorithm<C> setIsSuitableFor( SuitabilityPredicate isSuitableFor ) {
        _isSuitableFor = _checked(isSuitableFor, _isSuitableFor, SuitabilityPredicate.class);
        return this;
    }

    /**
     *  This method receives a {@link neureka.backend.api.algorithms.fun.ADAgentSupplier} which will supply
     *  {@link ADAgent} instances which can perform backward and forward auto differentiation.
     *  The lambda will be called by the {@link #supplyADAgentFor(Function, ExecutionCall, boolean)} method
     *  by any given {@link Operation} instances this algorithm belongs to.
     *
     * @param supplyADAgentFor A supplier for an {@link ADAgent} containing implementation details for autograd.
     * @return This very instance to enable method chaining.
     */
    public final AbstractFunctionalAlgorithm<C> setSupplyADAgentFor( ADAgentSupplier supplyADAgentFor ) {
        _supplyADAgentFor = _checked(supplyADAgentFor, _supplyADAgentFor, ADAgentSupplier.class);
        return this;
    }

    /**
     *  An {@link Algorithm} will typically produce a result when executing an {@link ExecutionCall}.
     *  This result must be created somehow.
     *  A {@link ExecutionPreparation} implementation instance will do just that...
     *  Often times the first entry in the array of tensors stored inside the call
     *  will be null to serve as a position for the output to be placed at.
     *  The creation of this output tensor is of course highly dependent on the type
     *  of operation and algorithm that is currently being used.
     *  Element-wise operations for example will require the creation of an output tensor
     *  with the shape of the provided input tensors, whereas the execution of a
     *  linear operation like for example a broadcast operation will require a very different approach...
     *  The lambda passed to this will be called by the {@link #prepare(ExecutionCall)} method
     *  by any given {@link Operation} instances this algorithm belongs to.
     *
     * @param instantiateNewTensorsForExecutionIn A lambda which prepares the provided execution call (usually output instantiation).
     * @return This very instance to enable method chaining.
     */
    public final AbstractFunctionalAlgorithm<C> setCallPreparation( ExecutionPreparation instantiateNewTensorsForExecutionIn ) {
        _instantiateNewTensorsForExecutionIn = _checked(instantiateNewTensorsForExecutionIn, _instantiateNewTensorsForExecutionIn, ExecutionPreparation.class);
        return this;
    }

    /**
     *  A {@link ADSupportPredicate} lambda checks what kind of auto differentiation mode an
     *  {@link Algorithm} supports for a given {@link ExecutionCall}.
     *  The lambda will be called by the {@link #autoDiffModeFrom(ExecutionCall)} method
     *  by any given {@link Operation} instances this algorithm belongs to.
     *
     * @param autogradModeFor A predicate lambda which determines the auto diff mode of this algorithm a given execution call.
     * @return This very instance to enable method chaining.
     */
    public final AbstractFunctionalAlgorithm<C> setAutogradModeFor( ADSupportPredicate autogradModeFor ) {
        _autogradModeFor = _checked(autogradModeFor, _autogradModeFor, ADSupportPredicate.class);
        return this;
    }

    @Override
    public AutoDiff autoDiffModeFrom(ExecutionCall<? extends Device<?>> call ) {
        _checkReadiness();
        return _autogradModeFor.autoDiffModeFrom( call );
    }

    public final AbstractFunctionalAlgorithm<C> setExecution( Execution execution ) {
        _execution = _checked(execution, _execution, Execution.class);
        return this;
    }

    @Override
    public Result execute( Function caller, ExecutionCall<? extends Device<?>> call ) {
        _checkReadiness();
        if ( call == null ) {
            if ( _supplyADAgentFor != null )
                return _execution.execute( caller, call ).withADAgent(_supplyADAgentFor);
            else
                return _execution.execute( caller, call );
        }
        MemValidator checker = MemValidator.forInputs( call.inputs(), ()-> {
            if ( _supplyADAgentFor != null )
                return _execution.execute( caller, call ).withADAgent(_supplyADAgentFor);
            else
                return _execution.execute( caller, call );
        });
        if ( checker.isWronglyIntermediate() ) {
            throw new IllegalStateException(
                    "Output of algorithm '" + this.getName() + "' " +
                            "is marked as intermediate result, despite the fact " +
                            "that it is a member of the input array. " +
                            "Tensors instantiated by library users instead of operations in the backend are not supposed to be flagged " +
                            "as 'intermediate', because they are not eligible for deletion!"
            );
        }
        if ( checker.isWronglyNonIntermediate() ) {
            throw new IllegalStateException(
                    "Output of algorithm '" + this.getName() + "' " +
                            "is neither marked as intermediate result nor a member of the input array. " +
                            "Tensors instantiated by operations in the backend are expected to be flagged " +
                            "as 'intermediate' in order to be eligible for deletion!"
            );
        }
        return checker.getResult();
    }
}


