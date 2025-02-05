package neureka.backend.main.operations.other;

import neureka.Neureka;
import neureka.Tsr;
import neureka.backend.api.Algorithm;
import neureka.backend.api.AutoDiffMode;
import neureka.backend.api.Result;
import neureka.backend.api.fun.SuitabilityPredicate;
import neureka.backend.api.template.operations.AbstractOperation;
import neureka.backend.api.template.operations.OperationBuilder;
import neureka.calculus.Function;
import neureka.calculus.args.Arg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Cat extends AbstractOperation
{
    public Cat()
    {
        super(
            new OperationBuilder()
                .identifier(       "concat"    )
                .operator(         "concat"    )
                .arity(            -1          ) // Any number of arguments
                .isOperator(       false       )
                .isIndexer(        false       )
                .isDifferentiable( true        )
                .isInline(         false       )
        );
        setAlgorithm(
            Algorithm
            .withName("concat")
            .setIsSuitableFor( call -> {
                Integer dim = call.getValOf(Arg.Axis.class);
                Tsr<?> a = call.input(0);
                Tsr<?> b = call.input(1);
                if ( a.rank() != b.rank() ) return SuitabilityPredicate.UNSUITABLE;
                for ( int i = 0; i < a.rank(); i++ )
                    if ( i != dim && a.shape(i) != b.shape(i) )
                        return SuitabilityPredicate.UNSUITABLE;

                return SuitabilityPredicate.GOOD;
            })
            .setAutogradModeFor( call -> AutoDiffMode.BACKWARD_ONLY )
            .setExecution(
                ( caller, call ) ->
                {
                    // The dimension alongside we want to concat:
                    Integer dim = call.getValOf(Arg.Axis.class);

                    // First let's find out the shape of the concatenated result:
                    Tsr<?>[] inputs = call.inputs();
                    List<Integer> axes = Arrays.stream(inputs).map( t -> t.shape(dim) ).collect(Collectors.toList());
                    int newAxisSize = axes.stream().mapToInt( i -> i ).sum();
                    List<Integer> newShape = new ArrayList<>();
                    for ( int i = 0; i < call.input(0).rank(); i++ )
                        newShape.add( i == dim ? newAxisSize : call.input(0).shape(i) );

                    // We create the output tensor:
                    Tsr<?> c = Tsr.of( call.input(0).getItemType(), newShape, 0 );

                    // We make the axes list entries cumulative:
                    for ( int i = 0; i < axes.size(); i++ )
                        axes.set( i, ( i == 0 ? axes.get(i) : axes.get( i - 1 ) + axes.get(i) ) );

                    // Now we need to create the slices of c needed to populate c:
                    for ( int i = 0; i < inputs.length; i++ ) {
                        int start = i == 0 ? 0 : axes.get( i - 1 );
                        int end = ( axes.get( i ) - 1 );
                        Tsr<?> slice = c.slice().axis( dim ).from( start ).to( end ).detached();
                        Neureka.get().backend().getFunction().idy().execute( slice, call.input( i ) );
                    }
                    c.mut().setIsIntermediate(true);
                    return
                        Result.of(c)
                            .withADAction( target -> {
                                int i = target.inputIndex();
                                int start = i == 0 ? 0 : axes.get( i - 1 );
                                int end = axes.get( i ) - 1;
                                return target.error().slice().axis(dim).from(start).to(end).detached();
                            });
                }
            )
            .buildFunAlgorithm()
        );
    }

    @Override
    public double calculate( double[] inputs, int j, int d, Function[] src ) { return src[ 0 ].call( inputs, j ); }
}
