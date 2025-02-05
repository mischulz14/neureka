package neureka.autograd;

import neureka.Neureka;
import neureka.Tsr;
import neureka.backend.main.memory.MemUtil;
import neureka.calculus.assembly.FunctionParser;

public final class PendingError<ValType>
{
    private int _toBeReceived;
    private final Tsr<ValType> _accumulatedError;

    public PendingError( Tsr<ValType> error, int toBeReceived ) {
        _toBeReceived = toBeReceived;
        _accumulatedError = error;
    }

    public void accumulate( Tsr<?> error ) {
        Tsr[] inputs = { _accumulatedError, error };
        MemUtil.keep( inputs, () -> {
                    new FunctionParser(
                                Neureka.get().backend()
                            )
                            .parse("I[ 0 ] <- (I[ 0 ] + I[ 1 ])", false)
                            .call(inputs);
                    return null;
                });
        _toBeReceived--;
    }

    public boolean isFullyAccumulated() {
        return _toBeReceived == 0;
    }

    public String toString() {
        return this.getClass().getSimpleName()+"[toBeReceived=" + _toBeReceived + ",accumulatedError=" + _accumulatedError + "]";
    }

    public int getToBeReceived() { return _toBeReceived; }

    public Tsr<ValType> getAccumulatedError() { return _accumulatedError; }

}
