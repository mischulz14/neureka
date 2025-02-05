package testutility;

import neureka.Neureka;
import neureka.Tsr;
import neureka.autograd.ADAction;
import neureka.autograd.GraphNode;
import neureka.backend.api.AutoDiffMode;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.template.algorithms.AbstractDeviceAlgorithm;
import neureka.backend.main.algorithms.Broadcast;
import neureka.backend.main.algorithms.NDConvolution;
import neureka.backend.main.implementations.fun.api.CPUBiFun;
import neureka.backend.main.implementations.CPUImplementation;
import neureka.backend.main.implementations.broadcast.CPUBroadcast;
import neureka.calculus.Function;
import neureka.calculus.args.Arg;
import neureka.devices.Device;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import neureka.dtype.DataType;
import neureka.ndim.NDUtil;
import neureka.ndim.NDimensional;
import neureka.ndim.config.NDConfiguration;

import java.util.List;

public class UnitTester_Tensor extends UnitTester
{
    public UnitTester_Tensor(String name)
    {
        super(name);
    }

    public int testShareDevice( Device device, Tsr[] tsrs ){
        printSessionStart("Testing if tensors share device!");
        println(BAR +"  Device: "+device.toString());
        println(BAR +"-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        for(Tsr t : tsrs){
            Device found = t.getDevice();//((Device)t.get(Device.class));
            this.assertStringContains("result", (found==null)?"null":found.toString(), device.toString());
        }
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensor(
            Tsr tensor,
            List<String> expected
    ){
        Object[] array = expected.toArray();
        String[] strings = new String[expected.size()];
        for(int i=0; i<strings.length; i++){
            strings[ i ] = (String)array[ i ];
        }
        DataType type = tensor.getDataType();
        if ( type != null ) {
            if ( tensor.getDevice() instanceof OpenCLDevice ) {
                this.assertStringContains("Tensor data type :", type.getRepresentativeType().getName(), "F32");
            } else this.assertStringContains("Tensor data type :", type.getRepresentativeType().getName(), "F64");
        }
        else this.assertStringContains("Tensor data type :", "null", "F64");
        return testTensor(tensor, strings);
    }

    public int testTensor(Tsr tensor, String[] expected){
        printSessionStart("Testing Tensor!");
        String result = tensor.toString();
        println(BAR +"  Tensor: "+result);
        println(BAR +"-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        for ( String element : expected ) {
            this.assertStringContains("result", result, element);
        }
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensorAutoGrad(Tsr[] source, String operation, String[] expected){
        printSessionStart("Testing Tsr: autograd!");
        println(BAR +"  Function: "+operation);
        println(BAR +"-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        Tsr product = Tsr.of(operation, source);
        String result = product.toString("rc");
        for(String element : expected){
            this.assertStringContains("result", result, element);
        }
        boolean productInInputs = false;
        for ( Tsr t : source ) productInInputs = (t == product || productInInputs);
        if ( !productInInputs ) product.getMut().delete();
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensorAutoGrad(Tsr[] source, String operation, String[] expected, Tsr error, double[][] expectedGradient){
        printSessionStart("Testing Tsr: autograd!");
        println(BAR +"  Function: "+operation);
        println(BAR +"-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        Tsr product = Tsr.of(operation, source);
        product.backward(error);
        String result = product.toString("rc");
        for(String element : expected){
            this.assertStringContains("result", result, element);
        }
        for(int i=0; i<source.length; i++){
            double[] gradient;
            if ( source[ i ].hasGradient() && source[ i ].is(Float.class) ) {
                float[] data = (float[]) source[i].getGradient().getRawItems();
                gradient = new double[data.length];
                for ( int ii = 0; ii < data.length; ii++ ) gradient[ii] = data[ii];
            } else
                gradient = source[ i ].hasGradient()
                                ? (double[])source[ i ].getGradient().getRawItems()
                                : null;
            this.assertIsEqual(
                    stringified(gradient),
                    stringified(expectedGradient[ i ])
            );
        }
        product.getMut().delete();
        return (printSessionEnd()>0)?1:0;
    }


    public int testTensorUtility_reshape(int[] dim, int[] newForm, int[] expected){
        int[] result = NDConfiguration.Utility.rearrange(dim, newForm);
        printSessionStart("Testing Tsr.indexing: dimension reshaping!");
        assertIsEqual(stringified(result), stringified(expected));
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensorUtility_translation(int[] dim, int[] expected){
        int [] result =  NDConfiguration.Layout.ROW_MAJOR.newTranslationFor(dim);
        printSessionStart("Testing Tsr.indexing: dimension _translation!");
        assertIsEqual(stringified(result), stringified(expected));
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensorUtility_makeFit( int[] a, int[] b, int[][] expected ){
        int [][] result =  NDUtil.makeFit( a, b );
        printSessionStart("Testing Tsr.indexing: dimension _translation!");
        if ( result.length != 2 ) throw new AssertionError("Invalid result!");
        assertIsEqual(
                stringified(result[0]),
                stringified(expected[0])
        );
        assertIsEqual(
                stringified(result[1]),
                stringified(expected[1])
        );
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensCon(
            int[] frstShp, int[] scndShp, double[] frstData, double[] scondData, double[] expctd
    ){
        printSessionStart("Test tensor indexing: tensMul_mxd");
        int[] drnMxd  = _shpOfCon(frstShp, scndShp);
        double[] rsltData = new double[NDConfiguration.Utility.sizeOfShape(drnMxd)];
        Neureka.get().backend().getOperation("x")
                .getAlgorithm(NDConvolution.class)
                .getImplementationFor( CPU.class )
                .run(
                        ExecutionCall.of(
                                    Tsr.of(drnMxd, rsltData),
                                    Tsr.of(frstShp, frstData),
                                    Tsr.of(scndShp, scondData)
                                )
                                .andArgs(Arg.DerivIdx.of(-1))
                                .running(Neureka.get().backend().getOperation("x"))
                                .on(CPU.get())
        );
        assertIsEqual(stringified(rsltData), stringified(expctd));
        return (printSessionEnd()>0)?1:0;
    }

    public int testInvTensCon(
            int[] frstShp, int[] scndShp,
            double[] frstData, double[] scondData, double[] drnData,
            double[] expctd, boolean first
    ){
        printSessionStart("Test Tsr.indexing: tensMul_mxd");
        int[] drnMxd  = _shpOfCon(frstShp, scndShp);
        Neureka.get().backend().getOperation(((char) 171)+"x")
                .getAlgorithm(NDConvolution.class)
                .getImplementationFor( CPU.class )
                .run(
                        ExecutionCall.of(
                                Tsr.of(frstShp, frstData),
                                (first)?Tsr.of(scndShp, scondData):Tsr.of(drnMxd, drnData),
                                (first)?Tsr.of(drnMxd, drnData):Tsr.of(scndShp, scondData)
                            )
                            .andArgs( Arg.DerivIdx.of(0) )
                            .running(Neureka.get().backend().getOperation(((char) 171)+"x"))
                            .on(CPU.get())
                );
        assertIsEqual(stringified((first)?frstData:scondData), stringified(expctd));
        return (printSessionEnd()>0)?1:0;
    }

    public int testTensBroadcast(int[] frstShp, int[] scndShp, double[] frstData, double[] scondData, double[] expctd){
        printSessionStart("Test Tsr.indexing: tensor broadcast_template.cl");
        int[] drnMxd  = _shpOfBrc(frstShp, scndShp);
        double[] rsltData = new double[NDConfiguration.Utility.sizeOfShape(drnMxd)];

        Neureka.get().backend().getOperation("*")
                .getAlgorithm(Broadcast.class)
                .getImplementationFor( CPU.class )
                .run(
                        ExecutionCall.of(
                                Tsr.of(drnMxd, rsltData),
                                Tsr.of(frstShp, frstData),
                                Tsr.of(scndShp, scondData)
                            )
                            .andArgs(Arg.DerivIdx.of(-1))
                            .running(Neureka.get().backend().getOperation("*"))
                            .on(CPU.get())
                );
        assertIsEqual(stringified(rsltData), stringified(expctd));
        return (printSessionEnd()>0)?1:0;
    }

    public int testInvTensBroadcast(
            int[] frstShp, int[] scndShp,
            double[] frstData, double[] scondData, double[] drnData,
            double[] expctd, boolean first
    ){
        printSessionStart("Test Tsr.indexing: tensor broadcast_template.cl");
        int[] drnMxd  = _shpOfBrc(frstShp, scndShp);

        Broadcast right = new Broadcast( AbstractDeviceAlgorithm::executeDeviceAlgorithm )
                                .setAutogradModeFor(
                                        call -> call
                                                .validate().allNotNullHaveSame(NDimensional::shape)
                                                .ifValid(AutoDiffMode.FORWARD_AND_BACKWARD)
                                                .orElse(AutoDiffMode.BACKWARD_ONLY)
                                )
                                .setDeviceExecution(
                                    (call, callback) -> AbstractDeviceAlgorithm.executeDeviceAlgorithm(call, callback),
                                    ( Function f, ExecutionCall<? extends Device<?>> adCall ) ->
                                    {
                                        Tsr<?> ctxDerivative = (Tsr<?>) adCall.getValOf(Arg.Derivative.class);
                                        Function mul = Neureka.get().backend().getFunction().mul();
                                        if ( ctxDerivative != null ) {
                                            return ADAction.of( target -> mul.execute( target.error(), ctxDerivative ) );
                                        }
                                        int d = adCall.getDerivativeIndex();
                                        Tsr<?> derivative = f.executeDerive( adCall.inputs(), d );
                                        return ADAction.of( target -> mul.execute( target.error(), derivative ) );
                                    }
                                )
                                .setCallPreparation(
                                        call -> {
                                            int offset = ( call.input( 0 ) == null ) ? 1 : 0;
                                            return
                                                ExecutionCall.of(call.input( offset ), call.input( 1+offset )).andArgs(Arg.DerivIdx.of(-1)).running(Neureka.get().backend().getOperation("idy")).on( call.getDevice() );
                                        }
                                )
                                .buildFunAlgorithm()
                                .setImplementationFor(
                                        CPU.class,
                                        CPUImplementation
                                                .withArity(3)
                                                .andImplementation(
                                                        new TestBroadcast()
                                                )
                                );

        Broadcast left = new Broadcast( AbstractDeviceAlgorithm::executeDeviceAlgorithm )
                                    .setAutogradModeFor(
                                        call -> call
                                                .validate().allNotNullHaveSame(NDimensional::shape)
                                                .ifValid(AutoDiffMode.FORWARD_AND_BACKWARD)
                                                .orElse(AutoDiffMode.BACKWARD_ONLY)
                                    )
                                    .setDeviceExecution(
                                        (call, callback) -> AbstractDeviceAlgorithm.executeDeviceAlgorithm(call, callback),
                                        ( Function f, ExecutionCall<? extends Device<?>> adCall ) ->
                                        {
                                            Tsr<?> ctxDerivative = (Tsr<?>) adCall.getValOf(Arg.Derivative.class);
                                            Function mul = Neureka.get().backend().getFunction().mul();
                                            if ( ctxDerivative != null ) {
                                                return ADAction.of( target -> mul.execute( target.error(), ctxDerivative ) );
                                            }
                                            Tsr<?>[] inputs = adCall.inputs();
                                            int d = adCall.getDerivativeIndex();
                                            Tsr<?> derivative = f.executeDerive( inputs, d );
                                            return ADAction.of( target -> mul.execute( target.error(), derivative ) );
                                        }
                                    )
                                    .setCallPreparation(
                                        call -> {
                                            Tsr<?>[] tsrs = call.inputs();
                                            int offset = ( tsrs[ 0 ] == null ) ? 1 : 0;
                                            return ExecutionCall.of(tsrs[offset], tsrs[1+offset]).andArgs(Arg.DerivIdx.of(-1)).running(Neureka.get().backend().getOperation("idy")).on( call.getDevice() );
                                        }
                                    )
                                    .buildFunAlgorithm()
                                    .setImplementationFor(
                                            CPU.class,
                                            CPUImplementation
                                                    .withArity(3)
                                                    .andImplementation(
                                                            new TestBroadcast()
                                                    )
                                    );

        left.getImplementationFor( CPU.class )
                .run(
                    ExecutionCall.of(
                                Tsr.of(frstShp, frstData),
                                (first)?Tsr.of(scndShp, scondData):Tsr.of(drnMxd, drnData),
                                (first)?Tsr.of(drnMxd, drnData):Tsr.of(scndShp, scondData)
                        )
                        .andArgs( Arg.DerivIdx.of(0) )
                        .running(Neureka.get().backend().getOperation("*"))
                        .on(CPU.get())
                );
        assertIsEqual(stringified((first)?frstData:scondData), stringified(expctd));

        right.getImplementationFor( CPU.class )
                .run(
                        ExecutionCall.of(
                                    Tsr.of(frstShp, frstData),
                                    (first)?Tsr.of(scndShp, scondData):Tsr.of(drnMxd, drnData),
                                    (first)?Tsr.of(drnMxd, drnData):Tsr.of(scndShp, scondData)
                            )
                            .andArgs( Arg.DerivIdx.of(0) )
                            .running(Neureka.get().backend().getOperation("*"))
                            .on(CPU.get())
        );
        assertIsEqual(stringified((first)?frstData:scondData), stringified(expctd));

        return ( printSessionEnd() > 0 ) ? 1 : 0;
    }


    /**
     * @param tensors
     * @param f
     * @return
     */
    public int testInjection( Tsr[] tensors, String f, String[][] expected )
    {
        printSessionStart("Test injection: I[0] <- I[1], I[0] -> I[1] : "+f);
        Tsr[] result = new Tsr[tensors.length+1];
        result[0] = Tsr.of( f, false, tensors );
        assert result[0] == null || result[0].get( GraphNode.class ) == null; // Because "doAD" is false!
        System.arraycopy(tensors, 0, result, 1, result.length - 1);
        for(int i=0; i<result.length; i++){
            if(expected[ i ]!=null && expected[ i ].length!=0){
                String[] parts = expected[ i ];
                String str = ( result[ i ] == null ? "null" : result[ i ].toString("rdgc") );
                for(String part : parts){
                    this.assertStringContains("tensor["+i+"]", str, part);
                }
            }
        }
        return (printSessionEnd()>0)?1:0;
    }

    
    private static int[] _shpOfBrc( int[] shp1, int[] shp2 ) {
        int[] shape = new int[ ( shp1.length + shp2.length ) / 2 ];
        for ( int i = 0; i < shp1.length && i < shp2.length; i++ ) {
            shape[ i ] = Math.max( shp1[ i ], shp2[ i ] );
            if ( Math.min(shp1[ i ], shp2[ i ]) != 1 && Math.max( shp1[ i ], shp2[ i ] ) != shape[ i ] ) {
                throw new IllegalStateException("Broadcast not possible. Shapes do not match!");
            }
        }
        return shape;
    }

    
    private static int[] _shpOfCon( int[] shp1, int[] shp2 ) {
        int[] shape = new int[ ( shp1.length + shp2.length ) / 2 ];
        for ( int i = 0; i < shp1.length && i < shp2.length; i++ )
            shape[ i ] = Math.abs( shp1[ i ] - shp2[ i ] ) + 1;
        return shape;
    }

    private static class TestBroadcast extends CPUBroadcast {

        @Override
        protected CPUBiFun _getFun() {
            return new CPUBiFun() {
                @Override public double invoke(double a, double b) { return a*b; }
                @Override public float invoke(float a, float b) { return a*b; }
            };
        }

        @Override
        protected CPUBiFun _getDeriveAt0() {
            return _getFun();
        }

        @Override
        protected CPUBiFun _getDeriveAt1() {
            return _getFun();
        }
    }

}
