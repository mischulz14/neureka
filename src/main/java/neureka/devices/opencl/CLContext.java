package neureka.devices.opencl;

import neureka.backend.api.BackendContext;
import neureka.backend.api.BackendExtension;
import neureka.backend.api.BackendRegistry;
import neureka.backend.api.Extensions;
import neureka.backend.main.algorithms.*;
import neureka.backend.main.implementations.broadcast.CLBroadcastAddition;
import neureka.backend.main.implementations.broadcast.CLBroadcastPower;
import neureka.backend.main.implementations.broadcast.CLScalarBroadcastAddition;
import neureka.backend.main.implementations.broadcast.CLScalarBroadcastPower;
import neureka.backend.main.implementations.elementwise.CLBiElementwisePower;
import neureka.backend.main.implementations.elementwise.CLElementwiseFunction;
import neureka.backend.main.implementations.fun.api.ScalarFun;
import neureka.backend.main.implementations.scalar.CLScalarFunction;
import neureka.backend.main.operations.functions.*;
import neureka.backend.main.operations.operator.Addition;
import neureka.backend.main.operations.operator.Power;
import neureka.calculus.assembly.ParseUtil;
import neureka.common.composition.Component;
import neureka.devices.Device;
import neureka.devices.opencl.utility.Messages;
import org.jocl.cl_platform_id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jocl.CL.clGetPlatformIDs;

/**
 *  This is an OpenCL context component for any given {@link BackendContext} which
 *  extends a given backend context instance for additional functionality, which in
 *  this case is the OpenCL backend storing platform and device information.
 *  {@link BackendContext}s are thread local states
 *  used for managing {@link neureka.backend.api.Operation}, {@link neureka.calculus.Function}
 *  as well as {@link Component} implementation instances like this one.
 *  A given state might not be compatible with the concepts introduced in other contexts
 *  which is why it makes sense to have separate "worlds" with potential different operations...
 *  The component system of the {@link BackendContext} exist so that a given context
 *  can be extended for more functionality
 *  and also to attach relevant states like for example in this case the {@link CLContext}
 *  instance will directly or indirectly reference kernels, memory objects and other concepts
 *  exposed by OpenCL...
 */
public final class CLContext implements BackendExtension
{
    private static final Logger _LOG = LoggerFactory.getLogger(CLContext.class);

    private final List<OpenCLPlatform> _platforms = new ArrayList<>();
    private final CLSettings _settings = new CLSettings();

    /**
     *  Use this constructor if you want to create a new OpenCL world in which there
     *  are unique {@link OpenCLPlatform} and {@link OpenCLDevice} instances.
     */
    public CLContext() {}

    /**
     * @return The number of all {@link OpenCLDevice} instances across all {@link OpenCLPlatform}s.
     */
    public int getTotalNumberOfDevices() {
        List<OpenCLPlatform> platforms = getPlatforms();
        if ( getPlatforms().isEmpty() ) return 0;
        return platforms.stream().mapToInt( p -> p.getDevices().size() ).sum();
    }

    /**
     * @return A list of context specific {@link OpenCLPlatform} instances possible containing {@link OpenCLDevice}s.
     */
    public List<OpenCLPlatform> getPlatforms() { return Collections.unmodifiableList( _platforms ); }

    /**
     * @return A container for OpenCL specific settings.
     */
    public CLSettings getSettings() { return _settings; }

    /**
     *  Updating the CLContext will cause the list of existing {@link OpenCLPlatform} instances to be
     *  cleared and refilled with completely new {@link OpenCLPlatform} instances.
     *  This will in effect also cause the recreation of any {@link OpenCLDevice} instances
     *  as part of these {@link OpenCLPlatform}s.
     *  This will subsequently cause the recompilation of many OpenCL kernels.
     */
    @Override
    public boolean update( OwnerChangeRequest<Extensions> changeRequest ) {
        _platforms.clear();
        _platforms.addAll( _findLoadAndCompileForAllPlatforms() );
        changeRequest.executeChange(); // This can be an 'add', 'remove' or 'transfer' of this component!
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"@"+Integer.toHexString(hashCode())+"[" +
                    "platforms=["+
                        _platforms.stream().map(Object::toString).collect(Collectors.joining(","))+
                    "]" +
                "]";
    }

    /**
     * @return A new list of freshly created {@link OpenCLPlatform} instances containing freshly instantiated {@link OpenCLDevice}s and kernels.
     */
    private static List<OpenCLPlatform> _findLoadAndCompileForAllPlatforms()
    {
        // Obtain the number of platforms
        int[] numPlatforms = new int[ 1 ];
        clGetPlatformIDs( 0, null, numPlatforms );

        // Obtain the platform IDs
        cl_platform_id[] platforms = new cl_platform_id[ numPlatforms[ 0 ] ];
        clGetPlatformIDs( platforms.length, platforms, null );

        List<OpenCLPlatform> loadedPlatforms = new ArrayList<>();
        for ( cl_platform_id id : platforms ) {
            OpenCLPlatform newPlatform = null;
            try {
                newPlatform = new OpenCLPlatform( id );
            } catch ( Exception e ) {
                String message =
                        "Failed to instantiate '"+OpenCLPlatform.class.getSimpleName()+"' " +
                        "with id '0x"+Long.toHexString(id.getNativePointer())+"'!";
                _LOG.error( message, e );
            }
            if ( newPlatform != null )
                loadedPlatforms.add( newPlatform );
        }
        if ( loadedPlatforms.isEmpty() || loadedPlatforms.stream().allMatch( p -> p.getDevices().isEmpty() ) )
            _LOG.warn( Messages.clContextCouldNotFindAnyDevices() );

        return loadedPlatforms;
    }

    @Override
    public DeviceOption find( String searchKey ) {
        Device<Number> result = null;
        double score = 0;
        for ( OpenCLPlatform p : _platforms ) {
            for ( OpenCLDevice d : p.getDevices() ) {
                double similarity = Stream.of("opencl",d.type().name(),d.name(),d.vendor())
                                            .map( word -> word.trim().toLowerCase() )
                                            .mapToDouble( word -> ParseUtil.similarity( word, searchKey ) )
                                            .max()
                                            .orElse(0);
                if ( similarity > score ) {
                    result = d;
                    score = similarity;
                    if ( score == 1 )
                        return new DeviceOption( result, score );
                }
            }
        }
        return new DeviceOption( result, score );
    }

    /**
     *  This method will free all the resources occupied by this context,
     *  meaning that all platforms and their devices will be disposed.
     *  Their kernels will be removed and their tensors restored.
     */
    @Override
    public void dispose() {
        for ( OpenCLPlatform platform : _platforms ) {
            for ( OpenCLDevice device : platform.getDevices() ) device.dispose();
            platform.dispose();
        }
        _platforms.clear();
    }

    @Override
    public void load( BackendRegistry registry ) {
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Power.class )
                .set( Scalarization.class, context -> new CLScalarBroadcastPower(context.getIdentifier()) )
                .set( Broadcast.class,     context -> new CLBroadcastPower(context.getIdentifier())       )
                .set( BiElementWise.class, context -> new CLBiElementwisePower(context.getIdentifier())   );

        registry.forDevice( OpenCLDevice.class )
                .andOperation( Addition.class )
                .set( Scalarization.class, context -> new CLScalarBroadcastAddition(context.getIdentifier()) )
                .set( Broadcast.class,     context -> new CLBroadcastAddition(context.getIdentifier())       );

        registry.forDevice( OpenCLDevice.class )
                .andOperation( Absolute.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.ABSOLUTE) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.ABSOLUTE) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Cosinus.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.COSINUS) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.COSINUS) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( GaSU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.GASU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.GASU) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( GaTU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.GATU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.GATU) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Gaussian.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.GAUSSIAN) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.GAUSSIAN) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( GaussianFast.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.GAUSSIAN_FAST) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.GAUSSIAN_FAST) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( GeLU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.GELU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.GELU) );
        //registry.forDevice( OpenCLDevice.class )
        //        .andOperation( Identity.class )
        //        .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.IDENTITY) )
        //        .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.IDENTITY) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Logarithm.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.LOGARITHM) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.LOGARITHM) );
        //registry.forDevice( OpenCLDevice.class )
        //        .andOperation( Quadratic.class )
        //        .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.QUADRATIC) )
        //        .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.QUADRATIC) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( ReLU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.RELU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.RELU) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( SeLU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SELU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SELU) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Sigmoid.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SIGMOID) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SIGMOID) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( SiLU.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SILU) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SILU) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Sinus.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SINUS) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SINUS) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Softplus.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SOFTPLUS) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SOFTPLUS) );
        registry.forDevice( OpenCLDevice.class )
                .andOperation( Softsign.class )
                .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.SOFTSIGN) )
                .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.SOFTSIGN) );
        //registry.forDevice( OpenCLDevice.class )
        //        .andOperation( Tanh.class )
        //        .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.TANH) )
        //        .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.TANH) );
        //registry.forDevice( OpenCLDevice.class )
        //        .andOperation( TanhFast.class )
        //        .set( Activation.class, context -> new CLElementwiseFunction(context.getIdentifier(), ScalarFun.TANH_FAST) )
        //        .set( ScalarActivation.class, context -> new CLScalarFunction(ScalarFun.TANH_FAST) );

    }

}
