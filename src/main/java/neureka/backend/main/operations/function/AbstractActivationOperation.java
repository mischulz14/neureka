package neureka.backend.main.operations.function;

import neureka.backend.api.AutoDiffMode;
import neureka.backend.api.template.operations.AbstractOperation;
import neureka.backend.api.template.operations.OperationBuilder;
import neureka.backend.main.algorithms.Activation;
import neureka.backend.main.algorithms.ScalarActivation;
import neureka.backend.main.algorithms.ScalarBroadcast;
import neureka.backend.main.algorithms.internal.Fun;
import neureka.calculus.Function;
import neureka.backend.api.template.algorithms.AbstractDeviceAlgorithm;
import neureka.devices.host.CPU;
import neureka.devices.opencl.OpenCLDevice;
import neureka.ndim.NDimensional;
import org.jetbrains.annotations.Contract;

abstract class AbstractActivationOperation extends AbstractOperation {

    AbstractActivationOperation(String identifier)
    {
        super(
            new OperationBuilder()
                .setIdentifier(       identifier    )
                .setOperator(         identifier    )
                .setArity(            1             )
                .setIsOperator(       false         )
                .setIsIndexer(        false         )
                .setIsDifferentiable( true          )
                .setIsInline(         false         )
        );
        setAlgorithm(
            new Activation().setSupplyADAgentFor( getDefaultAlgorithm() ).buildFunAlgorithm()
                .setImplementationFor(
                    CPU.class,
                    Activation.implementationForCPU()
                        .with(Fun.F64ToF64.pair(this::_activate, this::_derive))
                        .with(Fun.F32ToF32.pair(this::_activate, this::_derive))
                        .with(Fun.I32ToI32.pair(this::_activate, this::_derive))
                        .with(Fun.I64ToI64.pair(this::_activate, this::_derive))
                        .with(Fun.I8ToI8.pair(this::_activate, this::_derive))
                        .with(Fun.I16ToI16.pair(this::_activate, this::_derive))
                        .with(Fun.BoolToBool.pair(this::_activate, this::_derive))
                        .with(Fun.CharToChar.pair(this::_activate, this::_derive))
                        .with(Fun.ObjToObj.pair(this::_activate, this::_derive))
                        .get()
                )
                .setImplementationFor(
                    OpenCLDevice.class,
                    Activation.implementationForGPU( this.getIdentifier() )
                            .with( _activationCode() )
                            .and( _derivationCode() )
                )
        );

        setAlgorithm(
            new ScalarBroadcast(Fun.F64ToF64.pair(this::_activate, this::_derive))
            .setAutogradModeFor(
                    call -> call
                            .validate().allNotNullHaveSame(NDimensional::shape)
                            .ifValid(AutoDiffMode.FORWARD_AND_BACKWARD)
                            .orElse(AutoDiffMode.BACKWARD_ONLY)
            )
            .setDeviceExecution( (context, callback) -> AbstractDeviceAlgorithm.executeDeviceAlgorithm( context.call(), callback ) )
            .buildFunAlgorithm()
            .setImplementationFor(
                CPU.class,
                ScalarBroadcast.implementationForCPU()
                        .with(Fun.F64ToF64.pair(this::_activate, this::_derive))
                        .with(Fun.F32ToF32.pair(this::_activate, this::_derive))
                        .with(Fun.I32ToI32.pair(this::_activate, this::_derive))
                        .with(Fun.I64ToI64.pair(this::_activate, this::_derive))
                        .with(Fun.I8ToI8.pair(this::_activate, this::_derive))
                        .with(Fun.I16ToI16.pair(this::_activate, this::_derive))
                        .with(Fun.BoolToBool.pair(this::_activate, this::_derive))
                        .with(Fun.CharToChar.pair(this::_activate, this::_derive))
                        .get()
            )
        );

        setAlgorithm(
            new ScalarActivation(Fun.F64ToF64.pair(this::_activate, this::_derive))
            .setAutogradModeFor(
                call -> call
                        .validate().allNotNullHaveSame(NDimensional::shape)
                        .ifValid(AutoDiffMode.FORWARD_AND_BACKWARD)
                        .orElse(AutoDiffMode.BACKWARD_ONLY)
            )
            .setDeviceExecution( (context, callback) -> AbstractDeviceAlgorithm.executeDeviceAlgorithm( context.call(), callback ) )
            .buildFunAlgorithm()
            .setImplementationFor(
                CPU.class,
                ScalarActivation.implementationForCPU()
                        .with(Fun.F64ToF64.pair(this::_activate, this::_derive))
                        .with(Fun.F32ToF32.pair(this::_activate, this::_derive))
                        .with(Fun.I32ToI32.pair(this::_activate, this::_derive))
                        .with(Fun.I64ToI64.pair(this::_activate, this::_derive))
                        .with(Fun.I8ToI8.pair(this::_activate, this::_derive))
                        .with(Fun.I16ToI16.pair(this::_activate, this::_derive))
                        .with(Fun.BoolToBool.pair(this::_activate, this::_derive))
                        .with(Fun.CharToChar.pair(this::_activate, this::_derive))
                        .get()
            )
        );
    }

    @Override
    public final String stringify( String[] children ) {
        String expression = String.join( ", ", children );
        if ( expression.startsWith("(") && expression.endsWith(")") ) return getIdentifier() + expression;
        return getIdentifier() + "(" + expression + ")";
    }

    @Override
    public final double calculate( double[] inputs, int j, int d, Function[] src ) {
        boolean derive = d >= 0;
        double inner = ( !derive ? 1 : src[ 0 ].derive( inputs, d, j ) );
        return calculate( src[ 0 ].call( inputs, j ),  derive ) * inner;
    }

    @Contract(pure = true)
    private double calculate(double input, boolean derive ) {
        if ( !derive )
            return _activate( input );
        else
            return _derive( input ) ;
    }

    protected abstract String _activationCode();

    protected abstract String _derivationCode();

    protected abstract double _activate(double x);

    protected abstract double _derive(double x);

    protected float _activate(float x) { return (float) _activate( (double) x ); }

    protected float _derive(float x) { return (float) _derive( (double) x ); }

    protected int _activate(int x) { return (int) Math.round( _activate( (double) x ) ); }

    protected int _derive(int x) { return (int) Math.round( _derive( (double) x ) ); }

    protected long _activate(long x) { return Math.round( _activate( (double) x ) ); }

    protected long _derive(long x) { return Math.round( _derive( (double) x ) ); }

    protected byte _activate(byte x) { return (byte) Math.round( _activate( (double) x ) ); }

    protected byte _derive(byte x) { return (byte) Math.round( _derive( (double) x ) ); }

    protected short _activate(short x) { return (short) Math.round( _activate( (double) x ) ); }

    protected short _derive(short x) { return (short) Math.round( _derive( (double) x ) ); }

    protected boolean _activate(boolean x) { return Math.round( _activate( x ? 1 : 0 ) ) != 0; } // Some default behaviors, it might make sense to override this for some activations.

    protected boolean _derive(boolean x) { return Math.round( _derive( x ? 1 : 0 ) ) != 0; } // Some default behaviors, it might make sense to override this for some activations.

    protected char _activate(char x) { return (char) Math.round( _activate( (int) x ) ); } // Some default behaviors, it might make sense to override this for some activations.

    protected char _derive(char x) { return (char) Math.round( _derive( (int) x ) ); } // Some default behaviors, it might make sense to override this for some activations.

    protected Object _activate(Object x) { throw new IllegalStateException("Not implemented for operation "+getClass().getSimpleName()); }

    protected Object _derive(Object x) { throw new IllegalStateException("Not implemented for operation "+getClass().getSimpleName()); }

}
