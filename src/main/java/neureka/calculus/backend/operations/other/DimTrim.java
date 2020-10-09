package neureka.calculus.backend.operations.other;

import neureka.Tsr;
import neureka.acceleration.Device;
import neureka.autograd.ADAgent;
import neureka.calculus.Function;
import neureka.calculus.backend.operations.AbstractOperationType;
import neureka.calculus.backend.ExecutionCall;
import neureka.calculus.backend.implementations.functional.GenericImplementation;
import neureka.calculus.frontend.assembly.FunctionBuilder;
import neureka.ndim.config.AbstractNDC;

import java.util.ArrayList;
import java.util.List;

public class DimTrim extends AbstractOperationType
{

    public DimTrim()
    {

        super(
                "dimtrim",
                "dimtrim",
                -1,
                false,
                false,
                true,
                false
        );

        setStringifier(
                children -> {
                    String expression = String.join( ", ", children );
                    if (expression.charAt(0) == '(' && expression.charAt(expression.length() - 1) == ')') {
                        return "dimtrim" + expression;
                    }
                    return "dimtrim" + "(" + expression + ")";
                }
        );

        GenericImplementation implementation = new GenericImplementation("reshape")
                .setSuitabilityChecker( call -> 1.0f )
                .setBackwardADAnalyzer( call -> true )
                .setForwardADAnalyzer( call -> false )
                .setADAgentSupplier(
                        ( Function f, ExecutionCall<Device> call, boolean forward ) ->
                        {
                            int prefix = ((int[]) call.getAt("ends"))[0];
                            int postfix = ((int[]) call.getAt("ends"))[1];
                            if(forward){
                                throw new IllegalArgumentException("Dim-Trim operation does not support forward-AD!");
                            }
                            return new ADAgent()
                                    .withContext(call.getContext())
                                    .withForward((t, derivative) -> FunctionBuilder.build(f.toString(), false).derive(new Tsr[]{derivative},0))
                                    .withBackward(
                                            (t, error) -> pad(error, new int[]{prefix, postfix}, true)
                                    );
                        }
                )
                .setCallHock(
                        ( caller, call ) ->
                        {
                            Tsr<?>[] inputs = caller.srcActivation(call.getTensors(), call.getJ(), -1, 0);
                            assert inputs.length == 1;
                            Tsr<?> t = inputs[0];
                            if ( call.getDerivativeIndex() == 0 ) {
                                int prefix = ((int[]) call.getAt("ends"))[0];
                                int postfix = ((int[]) call.getAt("ends"))[0];
                                return pad(t, new int[]{prefix, postfix}, true);
                            } else {
                                int[] ends = new int[2];
                                call.putAt("ends", ends);
                                return trim(t, ends, true);
                            }
                        }
                )
                .setRJAgent( ( call, goDeeperWith ) -> null )
                .setDrainInstantiation( call -> call );

        setImplementation(
                GenericImplementation.class,
                implementation
        );

    }

    public static Tsr pad(Tsr tensor, int[] ends, boolean newTsr) {
        tensor = (newTsr) ? (Tsr)tensor.getAt(new ArrayList<>()) : tensor;
        List<Integer> newShape = new ArrayList<>();
        List<Integer> newTranslation = new ArrayList<>();
        List<Integer> newIdxmap = new ArrayList<>();
        List<Integer> newSpread = new ArrayList<>();
        List<Integer> newOffset = new ArrayList<>();
        int[] shape = tensor.getNDConf().shape();
        int prefix = ends[0];
        int postfix = ends[1];
        for ( int i = 0; i < prefix; i++ ) {
            newShape.add(1);
            newTranslation.add(1);
            newIdxmap.add(1);
            newSpread.add(0);
            newOffset.add(0);
        }
        for ( int i = 0; i < shape.length; i++ ) {
            newShape.add(shape[i]);
            newTranslation.add(tensor.getNDConf().translation(i));
            newIdxmap.add(tensor.getNDConf().idxmap(i));
            newSpread.add(tensor.getNDConf().spread(i));
            newOffset.add(tensor.getNDConf().offset(i));
        }
        for ( int i = 0; i < postfix; i++ ) {
            newShape.add(1);
            newTranslation.add(1);
            newIdxmap.add(1);
            newSpread.add(0);
            newOffset.add(0);
        }
        tensor.setNDConf(
                AbstractNDC.construct(
                        newShape.stream().mapToInt(i->i).toArray(),
                        newTranslation.stream().mapToInt(i->i).toArray(),
                        newIdxmap.stream().mapToInt(i->i).toArray(),
                        newSpread.stream().mapToInt(i->i).toArray(),
                        newOffset.stream().mapToInt(i->i).toArray()
                )
        );
        return tensor;
    }

    public static Tsr<?> trim(Tsr<?> tensor, int[] ends, boolean newTsr)
    {
        tensor = (newTsr) ? (Tsr<?>)tensor.getAt(new ArrayList<>()) : tensor;
        List<Integer> newShape = new ArrayList<>();
        List<Integer> newTranslation = new ArrayList<>();
        List<Integer> newIdxmap = new ArrayList<>();
        List<Integer> newSpread = new ArrayList<>();
        List<Integer> newOffset = new ArrayList<>();
        int[] shape = tensor.getNDConf().shape();
        int prefix = 0;
        for (int s : shape) if (s == 1) prefix++; else break;
        int postfix = 0;
        for ( int i=shape.length-1; i>=0; i-- ) if ( shape[i] == 1 ) postfix++; else break;
        for ( int i = prefix; i < shape.length-postfix; i++ ) {
            newShape.add(shape[i]);
            newTranslation.add(tensor.getNDConf().translation(i));
            newIdxmap.add(tensor.getNDConf().idxmap(i));
            newSpread.add(tensor.getNDConf().spread(i));
            newOffset.add(tensor.getNDConf().offset(i));
        }
        tensor.setNDConf(
                AbstractNDC.construct(
                        newShape.stream().mapToInt(i->i).toArray(),
                        newTranslation.stream().mapToInt(i->i).toArray(),
                        newIdxmap.stream().mapToInt(i->i).toArray(),
                        newSpread.stream().mapToInt(i->i).toArray(),
                        newOffset.stream().mapToInt(i->i).toArray()
                )
        );
        ends[0] = prefix;
        ends[1] = postfix;
        return tensor;
    }


    @Override
    public double calculate(double[] inputs, int j, int d, List<Function> src) {
        return src.get(0).call( inputs, j );
    }
}
