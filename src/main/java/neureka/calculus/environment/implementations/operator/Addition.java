package neureka.calculus.environment.implementations.operator;

import neureka.acceleration.host.execution.HostExecutor;
import neureka.acceleration.opencl.execution.CLExecutor;
import neureka.calculus.environment.OperationType;
import neureka.calculus.environment.executors.*;

public class Addition extends OperationType {

    private static final DefaultOperatorCreator<TertiaryNDXConsumer> _creator =
            (inputs, d) -> {
                double[] t1_val = inputs[1].value64();
                double[] t2_val = inputs[2].value64();
                if (d < 0) return (t0Idx, t1Idx, t2Idx) -> t1_val[inputs[1].i_of_idx(t1Idx)] + t2_val[inputs[2].i_of_idx(t2Idx)];
                else return (t0Idx, t1Idx, t2Idx) -> 1.0;
            };

    private static final Broadcast _broadcast = new Broadcast();

    public Addition()
    {
        super (
                "add",
                "+",
                -1,
                true,
                false,
                false,
                true,
                false
        );


        //_____________________
        // DEFAULT OPERATION :

        DefaultOperatorCreator<PrimaryNDXConsumer> operationCreator =
                (inputs, d) -> {
                    double[] t1_val = inputs[1].value64();
                    double[] t2_val = inputs[2].value64();
                    if (d < 0) return t1Idx -> t1_val[inputs[1].i_of_idx(t1Idx)] + t2_val[inputs[2].i_of_idx(t1Idx)];
                    else return t1Idx -> 1.0;
                };

        Operation operation = new Operation();

        setImplementation(Operation.class,
                operation
                        .setExecutor(
                        HostExecutor.class,
                                new HostExecutor(
                                        call ->
                                                call.getDevice().getExecutor()
                                                        .threaded (
                                                                call.getTensor(0).size(),
                                                                ( start, end ) ->
                                                                        Operation.operate (
                                                                                call.getTensor(0),
                                                                                call.getTensor(1),
                                                                                call.getTensor(2),
                                                                                call.getDerivativeIndex(),
                                                                                start, end,
                                                                                operationCreator.create(call.getTensors(), call.getDerivativeIndex())
                                                                        )
                                                        ),
                                        3
                                )
                        ).setExecutor(
                        CLExecutor.class,
                        new CLExecutor(
                                call -> {
                                    int offset = (call.getTensor(0) != null) ? 0 : 1;
                                    int gwz = (call.getTensor(0) != null) ? call.getTensor(0).size() : call.getTensor(1).size();
                                    call.getDevice().getKernel(call)
                                            .pass(call.getTensor(offset))
                                            .pass(call.getTensor(offset + 1))
                                            .pass(call.getTensor(offset + 2))
                                            .pass(call.getTensor(0).rank())
                                            .pass(call.getDerivativeIndex())
                                            .call(gwz);
                                },
                                3,
                                operation.getKernelSource(), // kernelSource
                                "output = input1 + input2;\n",
                                "output = 1;\n",
                                this // OperationType
                        )
                )
        );

        //________________
        // BROADCASTING :

        setImplementation(Broadcast.class,
                _broadcast
                .setExecutor(
                        HostExecutor.class,
                        new HostExecutor(
                                call ->
                                        call.getDevice().getExecutor()
                                                .threaded (
                                                        call.getTensor(0).size(),
                                                        ( start, end ) ->
                                                                Broadcast.broadcast (
                                                                        call.getTensor(0), call.getTensor(1), call.getTensor(2),
                                                                        call.getDerivativeIndex(), start, end,
                                                                        _creator.create(call.getTensors(), call.getDerivativeIndex())
                                                                )
                                                ),
                                3
                        )
                ).setExecutor(
                        CLExecutor.class,
                        new CLExecutor(
                                call -> {
                                    int offset = (call.getTensor(0) != null) ? 0 : 1;
                                    int gwz = (call.getTensor(0) != null) ? call.getTensor(0).size() : call.getTensor(1).size();
                                    call.getDevice().getKernel(call)
                                            .pass(call.getTensor(offset))
                                            .pass(call.getTensor(offset + 1))
                                            .pass(call.getTensor(offset + 2))
                                            .pass(call.getTensor(0).rank())
                                            .pass(call.getDerivativeIndex())
                                            .call(gwz);
                                },
                                3,
                                _broadcast.getKernelSource(), // kernelSource
                                "value = src1 + src2;\n",
                                "value += 1 * drain;\n",
                                this // OperationType
                        )
                )
        );

        //___________________________
        // TENSOR SCALAR OPERATION :

        Scalarization scalarization = new Scalarization();

        ScalarOperatorCreator<PrimaryNDXConsumer> scalarCreator =
                (inputs, value, d) -> {
                    double[] t1_val = inputs[1].value64();
                    if (d < 0) return t1Idx -> t1_val[inputs[1].i_of_idx(t1Idx)] + value;
                    else {
                        if (d == 0) return t1Idx -> 1;
                        else return t1Idx -> 1;
                    }
                };

        setImplementation(
                Scalarization.class,
                scalarization.setExecutor(
                        HostExecutor.class,
                                new HostExecutor(
                                        call -> {
                                            double value = call.getTensor(0).value64(2);
                                            call.getDevice().getExecutor()
                                                    .threaded (
                                                            call.getTensor(0).size(),
                                                            ( start, end ) ->
                                                                    Scalarization.scalarize (
                                                                            call.getTensor(0),
                                                                            start, end,
                                                                            scalarCreator.create(call.getTensors(), value, -1)
                                                                    )
                                                    );
                                            },
                                        3
                                )
                ).setExecutor(
                        CLExecutor.class,
                        new CLExecutor(
                                call -> {
                                    int offset = (call.getTensor(2).isVirtual() || call.getTensor(2).size() == 1)?1:0;
                                    int gwz = call.getTensor(0).size();
                                    call.getDevice().getKernel(call)
                                            .pass(call.getTensor(0))
                                            .pass(call.getTensor(0))
                                            .pass((float)call.getTensor(1+offset).value64(0))
                                            .pass(call.getTensor(0).rank())
                                            .pass(call.getDerivativeIndex())
                                            .call(gwz);
                                },
                                3,
                                scalarization.getKernelSource(), // kernelSource
                                "output = input1 + value;\n",
                                "output = 1;\n",
                                this // OperationType
                        )
                )
        );

        //__________________________
        // RELATED OPERATION TYPES :

        new OperationType(
                "", ((char) 171) + "+", 3, true, false, false, false, false
        ).setImplementation(Broadcast.class, _broadcast);

        new OperationType(
                "", "+" + ((char) 187), 3, true, false, false, false, false
        ).setImplementation(Broadcast.class, _broadcast);

        // Convolutoion:

        new OperationType(
                "add", "a", 2, true, false, true, false, false
        ).setImplementation(Convolution.class,
                new Convolution()
        );

        new OperationType(
                "", ((char) 171) + "a", 3, true, false, true, false, false
        );
        new OperationType(
                "", "a" + ((char) 187), 3, true, false, true, false, false
        );


    }

}
