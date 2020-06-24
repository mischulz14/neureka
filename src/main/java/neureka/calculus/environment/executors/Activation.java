package neureka.calculus.environment.executors;

import neureka.Tsr;
import neureka.calculus.environment.Type;
import org.jetbrains.annotations.Contract;

public class Activation extends AbstractTypeExecutor<Type.OperatorCreator>
{
    public Activation(String strActivation, String strDeriviation, Type.OperatorCreator creator){
        super(strActivation, strDeriviation, creator);
    }

    @Override
    public boolean canHandle(ExecutionCall call) {
        return true;
    }



    @Contract(pure = true)
    public static void activate(
            Tsr t0_drn,
            int i, int end,
            Type.DefaultOperator operation
    ) {
        int[] t0Shp = t0_drn.getNDConf().shape();//Tsr t0_origin, Tsr t1_handle, Tsr t2_drain ... when d>=0
        int rank = t0Shp.length;
        int[] t0Idx = t0_drn.idx_of_i(i);
        int[] t1Idx = new int[rank];
        double[] t0_value = t0_drn.value64();
        while (i < end) {//increment on drain accordingly:
            System.arraycopy(t0Idx, 0, t1Idx, 0, rank);
            //setInto _value in drn:
            t0_value[t0_drn.i_of_idx(t0Idx)] = operation.execute(t0Idx, t1Idx, null);
            //increment on drain:
            Tsr.Utility.Indexing.increment(t0Idx, t0Shp);
            i++;
        }
    }

}
