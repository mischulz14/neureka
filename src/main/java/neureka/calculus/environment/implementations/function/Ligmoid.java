package neureka.calculus.environment.implementations.function;

import neureka.acceleration.host.HostCPU;
import neureka.acceleration.host.execution.HostExecution;
import neureka.acceleration.opencl.OpenCLDevice;
import neureka.acceleration.opencl.execution.CLExecution;
import neureka.calculus.environment.OperationType;
import neureka.calculus.environment.executors.*;


public class Ligmoid extends OperationType {

    private DefaultOperatorCreator<TertiaryNDXConsumer> _creator =
            (inputs, d)->{
                double[] t1_val = inputs[1].value64();
                if (d < 0) return (t0Idx, t1Idx, t2Idx) -> Math.log(1 + Math.pow(Math.E, t1_val[inputs[1].i_of_idx(t1Idx)]));
                else return (t0Idx, t1Idx, t2Idx) -> 1 / (1 + Math.pow(Math.E, -t1_val[inputs[1].i_of_idx(t1Idx)]));
            };

    public Ligmoid(){
        super("ligmoid", "lig" , 1, false, false, false, true, true);


        Activation typeImplementation =
                new Activation(
                        "output = \n" +
                                "(\n" +
                                "        (float) log(\n" +
                                "            1+pow(\n" +
                                "                (float)\n" +
                                "                M_E,\n" +
                                "                (float)\n" +
                                "                input\n" +
                                "            )\n" +
                                "        )\n" +
                                "    );",
                        "output =\n" +
                                "    1 /\n" +
                                "        (1 + (float) pow(\n" +
                                "                (float)M_E,\n" +
                                "                (float)input\n" +
                                "            )\n" +
                                "        );\n",
                        _creator
                );



        setImplementation(
                Activation.class,
                typeImplementation.setExecution (
                        HostCPU.class,
                        new HostExecution(
                                call  ->
                                        call.getDevice().getExecutor()
                                                .threaded (
                                                        call.getTensor(0).size(),
                                                        ( start, end ) ->
                                                                Activation.activate (
                                                                        call.getTensor(0),
                                                                        start, end,
                                                                        _creator.create(call.getTensors(), -1)
                                                                )
                                                ),
                                3
                        )
                ).setExecution(
                        OpenCLDevice.class,
                        new CLExecution(
                                call -> {
                                    int offset = (call.getTensor(0) != null) ? 0 : 1;
                                    int gwz = (call.getTensor(0) != null) ? call.getTensor(0).size() : call.getTensor(1).size();
                                    call.getDevice().getKernel(call)
                                            .pass(call.getTensor(offset))
                                            .pass(call.getTensor(offset + 1))
                                            .pass(call.getTensor(0).rank())
                                            .pass(call.getDerivativeIndex())
                                            .call(gwz);
                                },
                                3,
                                typeImplementation.getKernelSource(), // kernelSource
                                "output = \n" +
                                        "   (\n" +
                                        "        (float) log(\n" +
                                        "            1+pow(\n" +
                                        "                (float)\n" +
                                        "                M_E,\n" +
                                        "                (float)\n" +
                                        "                input\n" +
                                        "            )\n" +
                                        "        )\n" +
                                        "    );",
                                "output =\n" +
                                        "    1 /\n" +
                                        "        (1 + (float) pow(\n" +
                                        "                (float)M_E,\n" +
                                        "                (float)input\n" +
                                        "            )\n" +
                                        "        );\n",
                                this // OperationType
                        )
                )
        );



    }


}
