import neureka.core.Tsr;
import neureka.core.function.IFunction;
import neureka.core.function.factory.assembly.FunctionBuilder;
import org.junit.Test;
import util.NTester_Function;
import util.NTester_Tensor;

public class TestOnCPU {

    @Test
    public void testReadmeExamples(){

        NTester_Tensor tester = new NTester_Tensor("Tensor tester (only cpu)");

        Tsr x = new Tsr(new int[]{1}, 3).setRqsGradient(true);
        Tsr b = new Tsr(new int[]{1}, -4);
        Tsr w = new Tsr(new int[]{1}, 2);
        /**
         *      ((3-4)*2)^2 = 4
         *  dx:   8*3 - 32  = -8
         * */
        Tsr y = new Tsr(new Tsr[]{x, b, w}, "((i0+i1)*i2)^2");
        tester.testTensor(y, new String[]{"[1]:(4.0); ->d[1]:(-8.0), "});
        y.backward(new Tsr(2));
        tester.testTensor(x, new String[]{"-16.0"});

        y = new Tsr("(","(",x,"+",b,")","*",w,")^2");
        tester.testTensor(y, new String[]{"[1]:(4.0); ->d[1]:(-8.0), "});
        y.backward(new Tsr(1));
        tester.testTensor(x, new String[]{"-8.0"});

        y = new Tsr("((",x,"+",b,")*",w,")^2");
        tester.testTensor(y, new String[]{"[1]:(4.0); ->d[1]:(-8.0), "});
        y.backward(new Tsr(-1));
        tester.testTensor(x, new String[]{"8.0"});
        //===========================================
        x = new Tsr(
                new int[]{2, 3, 1},
                new double[]{
                        3, 2,
                        -1, -2,
                        2, 4
                }
        );
        y = new Tsr(
                new int[]{1, 3, 2},
                new double[]{
                        4, -1, 3,
                        2, 3, -1
                });
        Tsr z = new Tsr(new Tsr[]{x, y}, "I0xi1");
        tester.testTensor(z, new String[]{"[2x1x2]:(19.0, 22.0, 1.0, -6.0)"});

        z = new Tsr(new Object[]{x, "x", y});
        tester.testTensor(z, new String[]{"[2x1x2]:(19.0, 22.0, 1.0, -6.0)"});
        //=======================
        x = new Tsr(
                new int[]{3, 3},
                new double[]{
                        1, 2, 5,
                        -1, 4, -2,
                        -2, 3, 4,
                }
        );
        y = new Tsr(
                new int[]{2, 2},
                new double[]{
                        -1, 3,
                        2, 3,
                }).setRqsGradient(true);

        tester.testTensor(y, new String[]{":g:(null)"});
        z = new Tsr(new Tsr[]{x, y}, "I0xi1");
        tester.testTensor(z, new String[]{"[2x2]:(15.0, 15.0, 18.0, 8.0)"});

        z = new Tsr(new Object[]{x, "x", y});
        tester.testTensor(z, new String[]{"[2x2]:(15.0, 15.0, 18.0, 8.0)"});

        z.backward(new Tsr(new int[]{2, 2}, 1));
        tester.testTensor(y, new String[]{"[2x2]:(-1.0, 3.0, 2.0, 3.0):g:(6.0, 9.0, 4.0, 9.0)"});
        //====
        x = new Tsr(new int[]{1}, 3);
        b = new Tsr(new int[]{1}, -5);
        w = new Tsr(new int[]{1}, -2);
        z = new Tsr(new Tsr[]{x, b, w}, "I0*i1*i2");
        tester.testTensor(z, new String[]{"[1]:(30.0)"});

        x = new Tsr(new int[]{1}, 4).setRqsGradient(true);
        b = new Tsr(new int[]{1}, 0.5);
        w = new Tsr(new int[]{1}, 0.5);
        y = new Tsr(new Tsr[]{x, b, w}, "(2^i0^i1^i2^2");
        tester.testTensor(y, new String[]{"[1]:(4.0);", " ->d[1]:(1.38629E0), "});
        //===
        tester.close();
    }

    @Test
    public void testTensorFunctions() {

        NTester_Function tester = new NTester_Function("Testing function fcn and scalar calculations");
        //EXPRESSION TESTING:
        tester.testExpression("ig0*(igj)xI[g1]", "((Ig[0]*Ig[j])xIg[1])", "");
        tester.testExpression("sum(ij)", "sum(I[j])", "");
        tester.testExpression("sum(1*(4-2/ij))", "sum(1.0*(4.0-(2.0/I[j])))", "");
        tester.testExpression("quadratic(ligmoid(Ij))", "quad(lig(I[j]))", "");
        tester.testExpression("softplus(I[3]^(3/i1)/sum(Ij^2)-23+I0/i1)", "lig((((I[3]^(3.0/I[1]))/sum(I[j]^2.0))-23.0)+(I[0]/I[1]))", "");
        tester.testExpression("1+3+5-23+I0*45/(345-651^I3-6)", "(1.0+3.0+(5.0-23.0)+(I[0]*(45.0/(345.0-(651.0^I[3])-6.0))))", "");
        tester.testExpression("sin(23*i1)-cos(i0^0.3)+tanh(23)", "((sin(23.0*I[1])-cos(I[0]^0.3))+tanh(23.0))", "");
        tester.testExpression("2*3/2-1", "((2.0*(3.0/2.0))-1.0)", "");
        tester.testExpression("3x5xI[4]xI[3]", "(((3.0x5.0)xI[4])xI[3])", "");
        tester.testExpression("[1,0, 5,3, 4]:(tanh(i0xi1))", "([1,0,5,3,4]:(tanh(I[0]xI[1])))", "");
        tester.testExpression("[0,2, 1,3, -1](sig(I0))", "([0,2,1,3,-1]:(sig(I[0])))", "");
        tester.testExpression("I[0]<-I[1]->I[2]", "((I[0]<-I[1])->I[2])", "");
        tester.testExpression("quadratic(I[0]) -> I[1] -> I[2]", "((quad(I[0])->I[1])->I[2])", "");

        //ACTIVATION TESTING:
        double[] input1 = {};
        tester.testActivation("6/2*(1+2)", input1, 9, "");
        input1 = new double[]{2, 3.2, 6};
        tester.testActivation("sum(Ij)", input1, 11.2, "");
        input1 = new double[]{0.5, 0.5, 100};
        tester.testActivation("prod(Ij)", input1, 25, "");
        input1 = new double[]{0.5, 0.5, 10};
        tester.testActivation("prod(prod(Ij))", input1, (2.5 * 2.5 * 2.5), "");
        input1 = new double[]{5, 4, 3, 12};//12/4-5+2+3
        tester.testActivation("I3/i[1]-I0+2+i2", input1, (3), "");
        input1 = new double[]{-4, -2, 6, -3, -8};//-3*-2/(-8--4-2)
        tester.testActivation("i3*i1/(i4-i0-2)-sig(0)+tanh(0)", input1, (-1.5), "");
        input1 = new double[]{2, 3, -2};//-3*-2/(-8--4-2)
        tester.testDeriviation("(i0*i1)*i2", input1, 0, (-6), "");
        input1 = new double[]{2, 3, -2};//-3*-2/(-8--4-2)
        tester.testDeriviation("lig(i0*i1)*i2", input1, 0, (-5.985164261060192), "");
        input1 = new double[]{2, 3, -2};//-3*-2/(-8--4-2)
        tester.testDeriviation("prod(ij)", input1, 1, (-4), "");

        Tsr[] tsrs = new Tsr[]{new Tsr(new int[]{2}, new double[]{1, 2}), new Tsr(new int[]{2},new double[]{3, -4})};
        Tsr expected = new Tsr(new int[]{2}, new double[]{0.9701425001453319, -0.8944271909999159});
        tester.testActivation("tanh(sum(Ij))", tsrs, expected, "");

        expected = new Tsr(new int[]{2}, new double[]{0.31326168751822286, 0.6931471805599453});
        tester.testActivation("lig(prod(Ij-2))", tsrs, expected, "");

        tsrs = new Tsr[]{new Tsr(new int[]{2, 4}, new double[]{10, 12, 16, 21, 33, 66, 222, 15})};
        expected = new Tsr(new int[]{1, 2, 2, 2}, new double[]{8.000335406372896, 10.000045398899216, 14.000000831528373, 19.000000005602796, 31.000000000000032, 64.0, 220.0, 13.000002260326852});
        tester.testActivation("lig([-1, 0, -2, -2](Ij-2))", tsrs, expected, "");

        tsrs = new Tsr[]{
                new Tsr(new int[]{2}, new double[]{-1, 3}),
                new Tsr(new int[]{2}, new double[]{7, -1}),
                new Tsr(new int[]{2}, new double[]{2, 2}),
        };
        expected = new Tsr(new int[]{2}, new double[]{-0.0018221023888012912, 0.2845552390654007});
        tester.testDerivative("lig(i0*i1)*i2", tsrs, 1, expected, "");

        tester.close();
    }

    @Test
    public void testTensorOperationsAndAutograd() {

        NTester_Tensor tester = new NTester_Tensor("Testing core tensor functionality");

        Tsr x = new Tsr(
                new int[]{2, 2},
                new double[]{
                        -1, 2,
                        -3, 3,
                }
        ).setRqsGradient(true);
        Tsr y = new Tsr(new int[]{1, 1}, -3);
        Tsr z = new Tsr(new Tsr[]{x, y}, "I0xi1");
        tester.testTensor(z, new String[]{"[2x2]:(3.0, -6.0, 9.0, -9.0)"});
        z.backward(new Tsr(new int[]{2, 2}, 1));
        tester.testTensor(x, new String[]{"[2x2]:(-1.0, 2.0, -3.0, 3.0):g:(-3.0, -3.0, -3.0, -3.0)"});
        //---
        x = new Tsr(new int[]{1}, 0.1).setRqsGradient(true);
        IFunction tanh = FunctionBuilder.build("tanh(i0)", true);
        IFunction tenxx = FunctionBuilder.build("i0*100", true);
        z = tenxx.activate(new Tsr[]{tanh.activate(new Tsr[]{x})});
        tester.testTensor(z, new String[]{"[1]:(9.95037E0)"});
        z.backward(new Tsr(new int[]{1}, 1));
        tester.testTensor(x, new String[]{"[1]:(0.1):g:(99.0099E0)"});
        tester.testTensor(z, new String[]{"[1]:(9.95037E0); ->d[1]:(99.0099E0), "});
        //---
        tester.testContains(
                z.toString("dgc"),
                new String[]{"[1]:(9.95037E0); ->d[1]:(99.0099E0),"},
                "test double formatting"
        );
        tester.testContains(
                new Tsr(3).toString("dgc"),
                new String[]{"[1]:(3.0)"},
                "test FP formatting"
        );
        //---
        Tsr tensor1 = new Tsr(3).setRqsGradient(true);
        Tsr tensor2 = new Tsr(-4);
        Tsr tensor3 = new Tsr(2);
        tester.testInjection(
                new Tsr[]{tensor1, tensor2, tensor3},
                "(Ig[0]<-I[1])->I[2]",
                new String[][]{
                        {"empty"},//result
                        {"(3.0)", "g:(-4.0)"},//tensor1
                        {"(-4.0)"},//tensor2
                        {"(-4.0)"},//tensor3
                });
        tensor1 = new Tsr(3).setRqsGradient(true);
        tensor2 = new Tsr(-4);
        tensor3 = new Tsr(2);
        Tsr result = IFunction.setup.commit(new Tsr[]{tensor1, tensor2, tensor3}, "(Ig[0]<-I[1])->I[2]", true);
        tester.testContains(
                result.toString(),
                new String[]{"(-4.0)"},
                "Testing if IFunction.setup.commit() returns non unique result!"
        );
        //---
        tensor1 = new Tsr(new int[]{1, 3}, 2);
        tensor2 = new Tsr(new int[]{2, 1}, -1);
        tensor1.setRqsGradient(true);
        tensor2.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1, tensor2},
                "relu(I[0]xI[1])",
                new String[]{
                        "[2x3]:(-0.02, -0.02, -0.02, -0.02, -0.02, -0.02);",
                        " =>d|[ [2x3]:(0.01, 0.01, 0.01, 0.01, 0.01, 0.01) ]|:t{ [2x3]:(-2.0, -2.0, -2.0, -2.0, -2.0, -2.0);",
                        " =>d|[ [2x1]:(-1.0, -1.0) ]|:t{ [1x3]:(2.0, 2.0, 2.0) },",
                        " =>d|[ [1x3]:(2.0, 2.0, 2.0) ]|:t{ [2x1]:(-1.0, -1.0) },",
                        "  }, "
                });
        //---
        tensor1 = new Tsr(new int[]{1, 3}, 2);
        tensor2 = new Tsr(new int[]{2, 1}, -1);
        tensor1.setRqsGradient(true);
        tensor2.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1, tensor2},
                "lig((I[0]xI[1])*-100)",
                new String[]{
                        "[2x3]:(200.0, 200.0, 200.0, 200.0, 200.0, 200.0);",
                        " =>d|[ [2x3]:(-100.0, -100.0, -100.0, -100.0, -100.0, -100.0) ]|:t{ [2x3]:(-2.0, -2.0, -2.0, -2.0, -2.0, -2.0);",
                        " =>d|[ [1x3]:(2.0, 2.0, 2.0) ]|:t{ [2x1]:(-1.0, -1.0) },",
                        " =>d|[ [2x1]:(-1.0, -1.0) ]|:t{ [1x3]:(2.0, 2.0, 2.0) },",
                        "  }, "
                }
        );
        //---
        tensor1 = new Tsr(new int[]{2, 3, 4}, 2);
        tensor1.setRqsGradient(false);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1},//, tensor2},/<=TODO make this throw an exception (if input does not match function)
                "lig([-2, 1, 0, -2]:(I[0])*-100)",
                new String[]{"[2x3x2x2]:(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)"}
        );
        //---
        tensor1 = new Tsr(new int[]{2}, 2);
        tensor2 = new Tsr(new int[]{2}, 4);
        tensor1.setRqsGradient(true);
        tensor2.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1, tensor2},
                "lig(tanh(I[0]*I[1]*2)*I[1])",
                new String[]{
                        "[2]:(4.0105E0, 4.0105E0); ",
                        "=>d|[ [2]:(3.9275E0, 3.9275E0) ]|" +
                                ":t{ [2]:(0.99805E0, 0.99805E0); ",
                        "=>d|[ [2]:(0.01556E0, 0.01556E0) ]|:t{ [2]:(4.0, 4.0) }, ",
                        "=>d|[ [2]:(0.03112E0, 0.03112E0) ]|:t{ [2]:(2.0, 2.0) }, ",
                        "}, ",
                        "=>d|[ [2]:(0.97996E0, 0.97996E0) ]|" +
                                ":t{ [2]:(4.0, 4.0) }, "
                }
        );
        //---
        tensor1 = new Tsr(new int[]{3, 2, 1}, 4);
        tensor2 = new Tsr(new int[]{1, 1, 4}, -1);
        tensor3 = new Tsr(new int[]{3, 2, 1}, 2);
        tensor2.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1, tensor2, tensor3},
                "I[0]xI[1]xI[2]",
                new String[]{
                        "[1x1x4]:(-48.0, -48.0, -48.0, -48.0);",
                        " =>d|[ [3x2x1]:(2.0, 2.0, 2.0, 2.0, 2.0, 2.0) ]|" +
                                ":t{ [3x2x4]:(-4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0); ",
                        "=>d|[ [3x2x1]:(4.0, 4.0, 4.0, 4.0, 4.0, 4.0) ]|" +
                                ":t{ [1x1x4]:(-1.0, -1.0, -1.0, -1.0) },  }, "
                });
        //--
        tensor1 = new Tsr(new int[]{5, 1, 1}, 4);//-2*4 = 8 | *3 = -24
        tensor2 = new Tsr(new int[]{1, 4, 1}, -2);
        tensor3 = new Tsr(new int[]{1, 1, 2}, 3);
        tensor1.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1, tensor2, tensor3},
                "I[0]xI[1]xI[2]",
                new String[]{
                        "[5x4x2]:(-24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0, -24.0); =>d|[ [1x1x2]:(3.0, 3.0) ]|:t{ [5x4x1]:(-8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0, -8.0); =>d|[ [1x4x1]:(-2.0, -2.0, -2.0, -2.0) ]|:t{ [5x1x1]:(4.0, 4.0, 4.0, 4.0, 4.0) },  }, "
                }
        );
        //=====================
        tensor1 = new Tsr(new int[]{2, 2}, new double[]{1, 2, 3, 4});//-2*4 = 8 | *3 = -24
        tensor1.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1},
                "((i0+2)^2)",
                new String[]{"d|[ [2x2]:(6.0, 8.0, 10.0, 12.0) ]|:t{ [2x2]:(1.0, 2.0, 3.0, 4.0) }"}
        );
        //---
        //=====================
        tensor1 = new Tsr(new int[]{2}, new double[]{1, 2});//-2*4 = 8 | *3 = -24
        tensor1.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1},
                "cos(tanh(lig(i0)))",
                new String[]{"[2]:(0.69985E0, 0.61771E0); =>d|[ [2]:(-0.19165E0, -0.12539E0) ]|:t{ [2]:(1.0, 2.0) }, "}
        );
        //---
        //=====================
        tensor1 = new Tsr(new int[]{1}, new double[]{2});//-2*4 = 8 | *3 = -24
        tensor1.setRqsGradient(true);
        tester.testTensorAutoGrad(
                new Tsr[]{tensor1},//2 =>-2 =>-4 =>12 //2 =>-2 //-2,12 =>-24
                "(-3*(2*(i0*-1)))*(-1*i0)",
                new String[]{
                        "[1]:(-24.0); ",
                        "=>d|[ [1]:(12.0) ]|:" +
                                "t{ [1]:(-2.0); " +
                                "=>d|[ [1]:(-1.0) ]|:" +
                                "t{ [1]:(2.0) },  " +
                                "},",
                        "=>d|[ [1]:(-2.0) ]|:" +
                                "t{ [1]:(12.0); " +
                                "=>d|[ [1]:(6.0) ]|:" +
                                "t{ [1]:(2.0) },  " +
                                "},"
                }
        );
        //---
        //======================
        //TESTING INVERS:
        ///=================
        //---
        tensor1 = new Tsr(new int[]{2, 3}, new double[]{
                0, 0, //3, 1,
                0, 0, //-2, -1,
                0, 0, ///-4, 2,
        });
        //---
        tensor2 = new Tsr(new int[]{5, 2}, new double[]{
                -2, 3, 6, 3, -1,
                0, 2, 4, 2, 1
        });
        tensor3 = new Tsr(new int[]{4, 2}, new double[]{
                1, 2, -3, 2,//<= drain data!
                4, -2, -1, 5,
        });
        tester.testTensorAutoGrad(new Tsr[]{tensor1, tensor2, tensor3},
                "i0<<i1<<i2",
                new String[]{"empty"});
        result = IFunction.setup.commit(new Tsr[]{tensor1, tensor2, tensor3}, "i0<<i1<<i2", true);
        tester.testContains(
                result.toString(),
                new String[]{"[2x3]:(-8.0, 4.0, -9.0, -2.0, 2.0, 3.0)"},
                "Testing if IFunction.setup.commit() returns non unique result!"
        );
        tester.testTensorAutoGrad(new Tsr[]{tensor1, tensor2, tensor3},//TODO:REACTIVATE!
                "i2>>i1>>i0",
                new String[]{"empty"});
        result = IFunction.setup.commit(new Tsr[]{tensor1, tensor2, tensor3}, "i2>>i1>>i0", true);
        tester.testContains(
                result.toString(),
                new String[]{"[2x3]:(-8.0, 4.0, -9.0, -2.0, 2.0, 3.0)"},
                "Testing if IFunction.setup.commit() returns non unique result!"
        );
        //=====================
        //---
        tensor1 = new Tsr(new int[]{2, 2, 1}, new double[]{
                1, 2, //3, 1,
                2, -3, //-2, -1,
        });
        tensor1.setRqsGradient(true);
        //---
        tensor2 = new Tsr(new int[]{1, 2, 2}, new double[]{
                -2, 3,
                1, 2,
        });
        tester.testTensorAutoGrad(//4, 5, -13, -4 <= result values
                new Tsr[]{tensor1, tensor2},
                "i0xi1",
                new String[]{"[2x1x2]:(4.0, -13.0, 5.0, -4.0); =>d|[ [1x2x2]:(-2.0, 3.0, 1.0, 2.0) ]|:t{ [2x2x1]:(1.0, 2.0, 2.0, -3.0) }"},
                new Tsr(new int[]{2, 1, 2}, new double[]{1, 1, 1, 1}),
                new double[][]{{-1.0, -1.0, 5.0, 5.0}, null}
        );
        //---
        //======================
        int[] shape = {4, 2, 9, 5, 6, 2};
        int[] newForm = {1, 0, -1, -2, 2, 4, 3, -1, 5};
        int[] expected = {2, 4, 1, 2, 9, 6, 5, 1, 2};
        tester.testTensorUtility_reshape(shape, newForm, expected);
        //---
        shape = new int[]{4, 2, 9, 5, 6, 2};
        expected = new int[]{1, 4, 4 * 2, 4 * 2 * 9, 4 * 2 * 9 * 5, 4 * 2 * 9 * 5 * 6};
        tester.testTensorUtility_translation(shape, expected);
        //---
        shape = new int[]{4, 2, 9, 5, 6, 2};
        expected = new int[]{1, 1, 4, 0, 0, 0};
        tester.testTensorBase_idxFromAnchor(shape, 37, expected);
        //---
        shape = new int[]{4, 3, 2, 5};
        expected = new int[]{3, 2, 1, 2};
        int idx = (1) * 3 + (1 * 4) * 2 + (1 * 4 * 3) * 1 + (1 * 4 * 3 * 2) * 2;
        tester.testTensorBase_idxFromAnchor(shape, idx, expected);
        //---
        int[] frstShape = {4, 1};
        double[] frstData = {
                -2, -1, 4, 3,
        };
        int[] scndShape = {1, 3};
        double[] scndData = {
                2,
                -4,
                3,
        };
        tester.testTensCon(
                frstShape, scndShape,
                frstData, scndData,
                new double[]{
                        -4, -2, 8, 6,
                        8, 4, -16, -12,
                        -6, -3, 12, 9
                }
        );
        //---
        frstShape = new int[]{4, 2};
        frstData = new double[]{
                -1, 2, -3, 1,
                4, -5, 2, 3,
        };
        scndShape = new int[]{2, 3};
        scndData = new double[]{
                4, -2,
                8, -1,
                -5, 2,
        };
        tester.testTensCon(
                frstShape, scndShape,
                frstData, scndData,
                new double[]{
                        29, -28, -1,
                        -40, 48, -29,
                }
        );
        //---
        frstShape = new int[]{1, 1};
        frstData = new double[]{2};

        scndShape = new int[]{2, 3};
        scndData = new double[]{
                4, -2,
                8, -1,
                -5, 2,
        };
        tester.testTensCon(
                frstShape, scndShape,
                frstData, scndData,
                new double[]{
                        8, -4,
                        16, -2,
                        -10, 4,
                }
        );
        //---
        frstShape = new int[]{2, 3, 2};
        frstData = new double[]{
                1, 2,
                3, 4,
                0, 2,

                3, 4,
                2, -1,
                -2, -3
        };
        //---
        scndShape = new int[]{2, 2, 3};
        scndData = new double[]{
                -1, 3,
                0, 2,

                -3, 1,
                2, -3,

                0, 4,
                5, -1
        };
        tester.testTensCon(
                frstShape, scndShape,
                frstData, scndData,
                new double[]{
                        15, 11,
                        20, -22,
                }
        );
        //---
        //TESTING INVERS:
        ///=================
        //---
        frstShape = new int[]{2, 3};
        frstData = new double[]{
                0, 0, //3, 1,
                0, 0, //-2, -1,
                0, 0, ///-4, 2,
        };
        //---
        scndShape = new int[]{5, 2};
        scndData = new double[]{
                -2, 3, 6, 3, -1,
                0, 2, 4, 2, 1
        };
        tester.testInvTensCon(//
                frstShape, scndShape,
                frstData, scndData,
                new double[]{
                        1, 2, -3, 2,//<= drain data!
                        4, -2, -1, 5,
                },
                new double[]{
                        1 * -2 + 2 * 3 - 3 * 6 + 2 * 3, 1 * 3 + 2 * 6 - 3 * 3 + 2 * -1,
                        1 * 0 + 2 * 2 - 3 * 4 + 2 * 2 + 4 * -2 - 2 * 3 - 1 * 6 + 5 * 3, 1 * 2 + 2 * 4 - 3 * 2 + 2 * 1 + 4 * 3 - 2 * 6 - 1 * 3 + 5 * -1,
                        4 * 0 - 2 * 2 - 1 * 4 + 5 * 2, 4 * 2 - 2 * 4 - 1 * 2 + 5 * 1
                },
                true
        );
        tester.close();
        //===========================================
    }


}
