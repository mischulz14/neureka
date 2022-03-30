package neureka.devices.host;

import neureka.Tsr;
import neureka.backend.api.Operation;
import neureka.calculus.Function;
import neureka.devices.AbstractDevice;
import neureka.devices.Device;
import neureka.devices.host.concurrent.Parallelism;
import neureka.devices.host.concurrent.WorkScheduler;
import neureka.devices.host.machine.ConcreteMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 *  The CPU class, one of many implementations of the {@link Device} interface,
 *  is simply supposed to be an API for dispatching threaded workloads onto the CPU.
 *  Contrary to other types of devices, the CPU will host tensor data by default, simply
 *  because the tensors will be stored in RAM (JVM heap) by default if no device was specified.
 *  This means that they are implicitly "stored" on the {@link CPU} device.
 *  The class is also a singleton instead of being part of a {@link neureka.backend.api.BackendExtension}.
 */
public class CPU extends AbstractDevice<Number>
{
    private static final Logger _LOG = LoggerFactory.getLogger( CPU.class );
    private static final CPU _INSTANCE;

    private static final WorkScheduler.Divider _DIVIDER;
    private static final IntSupplier _PARALLELISM;

    public static int PARALLELIZATION_THRESHOLD = 32;

    static {
        _INSTANCE = new CPU();
        _DIVIDER = new WorkScheduler.Divider(CPU.get().getExecutor().getPool());
        _PARALLELISM = Parallelism.THREADS;
    }

    private final JVMExecutor _executor = new JVMExecutor();
    private final Set<Tsr<Number>> _tensors = Collections.newSetFromMap(new WeakHashMap<>());

    private CPU() { super(); }

    /**
     *  Use this method to access the singleton instance of this {@link CPU} class,
     *  which is a {@link Device} type and default location for freshly instantiated {@link Tsr} instances.
     *  {@link Tsr} instances located on the {@link CPU} device will reside in regular RAM
     *  causing operations to run on the JVM and thereby the CPU.
     *
     * @return The singleton instance of this {@link CPU} class.
     */
    public static CPU get() { return _INSTANCE; }

    /**
     *  The {@link JVMExecutor} offers a similar functionality as the parallel stream API,
     *  however it differs in that the {@link JVMExecutor} is processing {@link RangeWorkload} lambdas
     *  instead of simply exposing a single index or concrete elements for a given workload size.
     *
     * @return A parallel range based execution API running on the JVM.
     */
    public JVMExecutor getExecutor() { return _executor; }

    @Override
    protected boolean _approveExecutionOf( Tsr<?>[] tensors, int d, Operation operation ) { return true; }

    /**
     *  This method will shut down the internal thread-pool used by this
     *  class to execute JVM/CPU based operations in parallel.
     */
    @Override
    public void dispose() {
        _executor.getPool().shutdown();
        _tensors.clear();
        _LOG.warn(
                "Main thread pool in '"+this.getClass()+"' shutting down! " +
                "Newly incoming operations will not be executed in parallel."
        );
    }

    @Override
    public <T extends Number> Device<Number> write( Tsr<T> tensor, Object value ) { return this; }

    @Override
    public <T extends Number> Object dataFor(Tsr<T> tensor ) { return tensor.getValue(); }

    @Override
    public <T extends Number> Number dataFor(Tsr<T> tensor, int index ) { return tensor.getValueAt( index ); }

    @Override
    public CPU restore( Tsr<Number> tensor ) { return this; }

    @Override
    public <T extends Number> CPU store( Tsr<T> tensor ) {
        //super.store(tensor);
        _tensors.add( (Tsr<Number>) tensor);
        return this;
    }

    @Override
    protected <T extends Number> T _readItem(Tsr<T> tensor, int index) {
        return null;
    }

    @Override
    protected <T extends Number, A> A _readArray(Tsr<T> tensor, Class<A> arrayType, int start, int size) {
        return null;
    }

    @Override
    protected <T extends Number> void _writeItem(Tsr<T> tensor, T item, int start, int size) {

    }

    @Override
    protected <T extends Number> void _writeArray(Tsr<T> tensor, Object array, int offset, int start, int size) {

    }

    @Override
    public <T extends Number> CPU store( Tsr<T> tensor, Tsr<T> parent ) {
        _tensors.add( (Tsr<Number>) tensor);
        _tensors.add( (Tsr<Number>) parent);
        return this;
    }

    @Override
    public <T extends Number> boolean has( Tsr<T> tensor ) { return _tensors.contains( tensor ); }

    @Override
    public <T extends Number> CPU free( Tsr<T> tensor ) {
        _tensors.remove( tensor );
        return this;
    }

    @Override
    public <T extends Number> CPU swap( Tsr<T> former, Tsr<T> replacement ) { return this; }

    @Override
    public <T extends Number> Device<Number> updateNDConf( Tsr<T> tensor ) {
        return this;
    }

    @Override
    public Collection<Tsr<Number>> getTensors() { return _tensors; }

    @Override
    public Operation optimizedOperationOf( Function function, String name ) { throw new IllegalStateException(); }

    /**
     *  This method is part of the component system built into the {@link Tsr} class.
     *  Do not use this as part of anything but said component system.
     *
     * @param changeRequest An API which describes the type of update and a method for executing said update.
     * @return The truth value determining if this {@link Device} ought to be added to a tensor (Here always false!).
     */
    @Override
    public boolean update( OwnerChangeRequest<Tsr<Number>> changeRequest ) {
        super.update( changeRequest );
        return false; // This type of device can not be a component simply because it is the default device
    }

    /**
     * Returns the number of CPU cores available to the Java virtual machine.
     * This value may change during a particular invocation of the virtual machine.
     * Applications that are sensitive to the number of available processors should
     * therefore occasionally poll this property and adjust their resource usage appropriately.
     *
     * @return The maximum number of CPU cores available to the JVM.
     *         This number is never smaller than one!
     */
    public int getCoreCount() { return Runtime.getRuntime().availableProcessors(); }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[coreCount="+getCoreCount()+"]";
    }

    /**
     *  A simple functional interface for executing a range whose implementations will
     *  either be executed sequentially or they are being dispatched to
     *  a thread-pool, given that the provided workload is large enough.
     */
    @FunctionalInterface
    public interface RangeWorkload {
        void execute( int start, int end );
    }

    /**
     *  The {@link JVMExecutor} offers a similar functionality as the parallel stream API,
     *  however it differs in that the {@link JVMExecutor} is processing {@link RangeWorkload} lambdas
     *  instead of simply exposing a single index or concrete elements for a given workload size.
     *  This means that a {@link RangeWorkload} lambda will be called with the work range of a single worker thread
     *  processing its current workload.
     *  This range is dependent on the number of available threads as well as the size of the workload.
     *  If the workload is very small, then the current main thread will process the entire workload range
     *  whereas the underlying {@link ThreadPoolExecutor} will not be used to avoid unnecessary overhead.
     */
    public static class JVMExecutor
    {
        private static final AtomicInteger _COUNTER = new AtomicInteger();
        private static final ThreadGroup   _GROUP   = new ThreadGroup("neureka-daemon-group");

        /*
            The following 2 constants determine if any given workload size will be parallelize or not...
            We might want to adjust this some more for better performance...
         */
        private static final int _MIN_THREADED_WORKLOAD_SIZE = 32;
        private static final int _MIN_WORKLOAD_PER_THREAD    = 8;

        private final ThreadPoolExecutor _pool =
                                            new ThreadPoolExecutor(
                                                    ConcreteMachine.ENVIRONMENT.units,
                                                    Integer.MAX_VALUE,
                                                    5L,
                                                    TimeUnit.SECONDS,
                                                    new SynchronousQueue<Runnable>(), // This is basically always of size 1
                                                    _newThreadFactory("neureka-daemon-")
                                            );

        private static ThreadFactory _newThreadFactory( final String name ) {
            return _newThreadFactory( _GROUP, name );
        }

        private static ThreadFactory _newThreadFactory( final ThreadGroup group, final String name ) {

            String prefix = name.endsWith("-") ? name : name + "-";

            return target -> {
                Thread thread = new Thread(
                                    group,
                                    target,
                                    prefix + _COUNTER.incrementAndGet() // The name, including the thread number.
                                );
                thread.setDaemon(true);
                return thread;
            };
        }

        public ThreadPoolExecutor getPool() { return _pool; }

        /**
         *  This method slices the provided workload size into multiple ranges which can be executed in parallel.
         *
         * @param workloadSize The total workload size which ought to be split into multiple ranges.
         * @param workload The range lambda which ought to be executed across multiple threads.
         */
        public void threaded( int workloadSize, RangeWorkload workload )
        {
            int cores = get().getCoreCount();
            cores = ( cores == 0 ? 1 : cores );
            if ( workloadSize >= _MIN_THREADED_WORKLOAD_SIZE && ( ( workloadSize / cores ) >= _MIN_WORKLOAD_PER_THREAD) ) {
                threaded(0, workloadSize, workload );
            }
            else sequential( workloadSize, workload );
        }

        /**
         *  This method will simply execute the provided {@link RangeWorkload} lambda sequentially
         *  with 0 as the start index and {@code workloadSize} as the exclusive range.       <br><br>
         *
         * @param workloadSize The workload size which will be passed to the provided {@link RangeWorkload} as second argument.
         * @param workload The {@link RangeWorkload} which will be executed sequentially.
         */
        public void sequential( int workloadSize, RangeWorkload workload ) {
            workload.execute( 0, workloadSize );
        }


        /**
         *  Takes the provided range and divides it into multi-threaded workloads.
         *
         * @param first The start index of the threaded workload range.
         * @param limit The limit for the workload range, which is exclusive.
         * @param rangeWorkload A workload lambda which will be called by different threads with different sub-ranges.
         */
        public void threaded(
                final int first,
                final int limit,
                final RangeWorkload rangeWorkload
        ) {
            _DIVIDER.parallelism( _PARALLELISM )
                    .threshold( PARALLELIZATION_THRESHOLD )
                    .divide( first, limit, rangeWorkload);
        }

    }

}