/*
MIT License

Copyright (c) 2019 Gleethos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            _         _                  _   _____             _
      /\   | |       | |                | | |  __ \           (_)
     /  \  | |__  ___| |_ _ __ __ _  ___| |_| |  | | _____   ___  ___ ___
    / /\ \ | '_ \/ __| __| '__/ _` |/ __| __| |  | |/ _ \ \ / / |/ __/ _ \
   / ____ \| |_) \__ \ |_| | | (_| | (__| |_| |__| |  __/\ V /| | (_|  __/
  /_/    \_\_.__/|___/\__|_|  \__,_|\___|\__|_____/ \___| \_/ |_|\___\___|


*/

package neureka.devices;

import neureka.Component;
import neureka.Tsr;
import neureka.backend.api.Algorithm;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.ImplementationFor;
import neureka.backend.api.Operation;
import neureka.calculus.args.Arg;
import neureka.framing.Relation;
import neureka.utility.CustomCleaner;
import neureka.utility.Messages;
import neureka.utility.NeurekaCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  The is the abstract precursor class providing
 *  some useful implementations for core concepts which are most likely
 *  applicable to most concrete implementations of the Device interface.
 *  These class provides the following features :
 *
 *  - A Cleaner instance used for freeing resources of the device.
 *
 *  - An component update implementations which simply calls the swap method of the device.
 *
 *  - An implementation for the execution method which calls the underlying calculus backend.
 *
 * @param <ValType>
 */
public abstract class AbstractDevice<ValType> extends AbstractBaseDevice<ValType>
{
    private static final NeurekaCleaner _CLEANER = new CustomCleaner();//Cleaner.create();
    private static final Logger _LOG = LoggerFactory.getLogger( AbstractDevice.class );

    protected Logger _log;

    protected AbstractDevice() {
        _log = LoggerFactory.getLogger( getClass() );
    }

    /**
     *  This method is the internal execution routine called by it's public counterpart
     *  and implemented by classes extending this very abstract class.
     *  It substitutes the implementation of this public "execute" method
     *  in order to make any execution call on any device extending this class
     *  checked before execution.
     *  The checking occurs in the public "execute" method of this class.
     *
     * @param tensors An array of input tensors.
     * @param d The index of the input which ought to be derived.
     * @param type The type of operation.
     */
    protected abstract void _execute( Tsr[] tensors, int d, Operation type );



    @Override
    public boolean update( OwnerChangeRequest<Tsr<ValType>> changeRequest ) {
        Tsr<ValType> oldOwner = changeRequest.getOldOwner();
        Tsr<ValType> newOwner = changeRequest.getNewOwner();
        if ( changeRequest.type() == IsBeing.REPLACED ) swap( oldOwner, newOwner );
        else if ( oldOwner == null && newOwner != null ) {
            if ( newOwner.has( Relation.class ) ) {
                Relation relation = newOwner.get(Relation.class);
                if (relation.hasParent()) { // Root needs to be found ! :
                    Tsr root = relation.findRootTensor();
                    if (!this.has(root) || !root.isOutsourced())
                        throw new IllegalStateException("Data parent is not outsourced!");
                }
            }
        }
        return true;
    }

    @Override
    public Device cleaning( Tsr tensor, Runnable action ) {
        _cleaning( tensor, action );
        return this;
    }

    protected void _cleaning( Object o, Runnable action ) {
        _CLEANER.register( o, action );
    }

    /**
     *  <b>This method plays an important role in connecting two main conceptional components of this library.</b>
     *  It dispatches the execution call object to the backends of Neureka.
     *  This is the single connection points where these two parts of the library come together.
     *
     * @param call The execution call object containing tensor arguments and settings for the device to execute on.
     * @return This very device instance in order to enable method chaining.
     */
    @Override
    public Device<ValType> execute( ExecutionCall<? extends Device<?>> call )
    {
        for ( Tsr<?> t : call.getTensors() ) {
            if ( t == null ) throw new IllegalArgumentException(
                    "Device arguments may not be null!\n" +
                    "One or more tensor arguments within the given ExecutionCall instance is null."
            );
        }
        call = (ExecutionCall<? extends Device<?>>) ExecutionCall.of(call.getTensors())
                                                                .andArgs(Arg.DerivIdx.of(call.getDerivativeIndex()))
                                                                .running(call.getOperation())
                                                                .on(this)
                                                                .forDeviceType(getClass());

        _execute( call.getTensors(), call.getDerivativeIndex(), call.getOperation() );

        Algorithm<?> algorithm = call.getAlgorithm();
        if ( algorithm == null ) {
            String message = Messages.Device.couldNotFindSuitableAlgorithmFor( this.getClass() );
            _LOG.error( message );
            throw new IllegalStateException( message );
        } else {
            ImplementationFor implementation = algorithm.getImplementationFor( this.getClass() );
            if ( implementation == null ) {
                String message = Messages.Device.couldNotFindSuitableImplementationFor( algorithm, this.getClass() );
                _LOG.error( message );
                throw new IllegalStateException( message );
            } else {
                implementation.run( call );
            }
        }

        return this;
    }

    @Override
    public <T extends ValType> Storage<ValType> store( Tsr<T> tensor ) {
        tensor.set( (Component) this ); // This way we move the storing procedure to the update function!
        return this;
    }
}
