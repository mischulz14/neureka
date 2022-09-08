package neureka.backend.main.operations.operator.impl;

import neureka.Tsr;
import neureka.backend.api.ExecutionCall;
import neureka.backend.api.ImplementationFor;
import neureka.backend.main.functions.CPUBiFun;
import neureka.devices.host.CPU;
import neureka.ndim.iterator.NDIterator;

public abstract class CPUScalarBroadcast implements ImplementationFor<CPU>
{
    protected abstract CPUBiFun _getFun();
    protected abstract CPUBiFun _getDeriveAt0();
    protected abstract CPUBiFun _getDeriveAt1();

    @Override
    public Tsr<?> run(ExecutionCall<CPU> call) {
        call.getDevice()
            .getExecutor()
            .threaded(
                call.input(0).size(),
                _workloadFor(call)
            );

        return call.input(0);
    }

    public CPU.RangeWorkload _workloadFor(
            ExecutionCall<CPU> call
    ) {
        int offset = ( call.arity() == 3 ? 1 : 0 );
        Tsr<?> t0_drn = call.input( 0 );
        Tsr<?> src    = call.input( offset );

        Class<?> typeClass = call.input( 1 ).getItemType();

        int d = call.getDerivativeIndex();
        CPUBiFun f = ( d ==  0 ? _getDeriveAt0() : ( d == 1 ? _getDeriveAt1() : _getFun() ) );

        CPU.RangeWorkload workload = null;

        if ( typeClass == Double.class ) {
            double value = call.input(Number.class, 1 + offset).at(0).get().doubleValue();
            double[] t0_value = t0_drn.getUnsafe().getDataForWriting(double[].class);
            double[] t1_value = src.getUnsafe().getDataAs(double[].class);
            workload = ( i, end ) -> {
                NDIterator t0Idx = NDIterator.of(t0_drn);
                NDIterator srcIdx = NDIterator.of(src);
                t0Idx.set(t0_drn.indicesOfIndex(i));
                srcIdx.set(src.indicesOfIndex(i));
                while ( i < end ) // increment on drain accordingly:
                {
                    // setInto _value in drn:
                    t0_value[t0Idx.i()] = f.invoke(t1_value[srcIdx.i()], value);
                    // increment on drain:
                    t0Idx.increment();
                    srcIdx.increment();
                    //NDConfiguration.Utility.increment(t0Idx, t0Shp);
                    i++;
                }
            };
        }
        if ( typeClass == Float.class ) {
            float value = call.input(Number.class, 1 + offset).at(0).get().floatValue();
            float[] t0_value = t0_drn.getUnsafe().getDataForWriting(float[].class);
            float[] t1_value = src.getUnsafe().getDataAs(float[].class);
            workload = ( i, end ) -> {
                NDIterator t0Idx = NDIterator.of(t0_drn);
                NDIterator srcIdx = NDIterator.of(src);
                t0Idx.set(t0_drn.indicesOfIndex(i));
                srcIdx.set(src.indicesOfIndex(i));
                while (i < end) // increment on drain accordingly:
                {
                    // setInto _value in drn:
                    t0_value[t0Idx.i()] = f.invoke(t1_value[srcIdx.i()], value);
                    // increment on drain:
                    t0Idx.increment();
                    srcIdx.increment();
                    //NDConfiguration.Utility.increment(t0Idx, t0Shp);
                    i++;
                }
            };
        }
        if ( typeClass == Integer.class ) {
            int value = call.input(Number.class, 1 + offset).at(0).get().intValue();
            int[] t0_value = t0_drn.getUnsafe().getDataForWriting(int[].class);
            int[] t1_value = src.getUnsafe().getDataAs(int[].class);
            workload = ( i, end ) -> {
                NDIterator t0Idx = NDIterator.of(t0_drn);
                NDIterator srcIdx = NDIterator.of(src);
                t0Idx.set(t0_drn.indicesOfIndex(i));
                srcIdx.set(src.indicesOfIndex(i));
                while (i < end) // increment on drain accordingly:
                {
                    // setInto _value in drn:
                    t0_value[t0Idx.i()] = f.invoke(t1_value[srcIdx.i()], value);
                    // increment on drain:
                    t0Idx.increment();
                    srcIdx.increment();
                    //NDConfiguration.Utility.increment(t0Idx, t0Shp);
                    i++;
                }
            };
        }
        if ( t0_drn.getUnsafe().getData().getRef().getClass() == Object[].class ) {
            Object value = call.input( 1 + offset ).at(0).get();
            Object[] t0_value = t0_drn.getUnsafe().getDataForWriting(Object[].class);
            Object[] t1_value = src.getUnsafe().getDataAs(Object[].class);
            workload = ( i, end ) -> {
                NDIterator t0Idx = NDIterator.of(t0_drn);
                NDIterator srcIdx = NDIterator.of(src);
                t0Idx.set(t0_drn.indicesOfIndex(i));
                srcIdx.set(src.indicesOfIndex(i));
                while (i < end) // increment on drain accordingly:
                {
                    // setInto _value in drn:
                    t0_value[t0Idx.i()] = f.invoke(t1_value[srcIdx.i()], value);
                    // increment on drain:
                    t0Idx.increment();
                    srcIdx.increment();
                    //NDConfiguration.Utility.increment(t0Idx, t0Shp);
                    i++;
                }
            };
        }

        if ( workload == null )
            throw new IllegalArgumentException("");
        else
            return workload;
    }


}
