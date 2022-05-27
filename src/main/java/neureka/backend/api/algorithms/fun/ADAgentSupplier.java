package neureka.backend.api.algorithms.fun;

import neureka.autograd.ADAgent;
import neureka.backend.api.ExecutionCall;
import neureka.calculus.Function;
import neureka.devices.Device;

/**
 *  Implementations of this functional interface ought to return a new instance
 *  of the {@link ADAgent} class responsible for performing automatic differentiation
 *  both for forward and backward mode differentiation. <br>
 *  Therefore an {@link ADAgent} exposes 2 different procedures. <br>
 *  One is the forward mode differentiation, and the other one <br>
 *  is the backward mode differentiation which is more commonly known as back-propagation... <br>
 *  Besides that it may also contain context information used <br>
 *  to perform said procedures.
 */
public interface ADAgentSupplier {

    /**
     *  This method ought to return a new instance
     *  if the {@link ADAgent} class responsible for performing automatic differentiation
     *  both for forward and backward mode differentiation. <br>
     *  Therefore an {@link ADAgent} exposes 2 different procedures. <br>
     *  One is the forward mode differentiation, and the other one <br>
     *  is the backward mode differentiation which is more commonly known as back-propagation... <br>
     *  Besides that it may also contain context information used <br>
     *  to perform said procedures.
     *
     * @param function The function from where the request for auto differentiation originates.
     * @param call The execution call of the current execution which requires auto differentiation support.
     * @return The resulting {@link ADAgent}.
     */
    ADAgent supplyADAgentFor(
            Function function,
            ExecutionCall<? extends Device<?>> call
    );

}
