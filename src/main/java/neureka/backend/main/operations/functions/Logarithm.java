package neureka.backend.main.operations.functions;

import neureka.backend.main.implementations.fun.api.ScalarFun;
import neureka.calculus.Function;

public final class Logarithm extends AbstractActivationOperation
{
    public Logarithm() {
        super(ScalarFun.LOGARITHM);
    }
    @Override
    public String asDerivative( Function[] children, int derivationIndex) {
        if ( children.length != 1 ) throw new IllegalStateException("Natual logarithm does not support more than 1 argument.");
        return children[0].getDerivative(derivationIndex)+" / "+children[0].toString();
    }
}
