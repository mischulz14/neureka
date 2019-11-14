package neureka.core.device;

import neureka.core.Tsr;

import java.util.Collection;

public interface Device
{
    void dispose();

    Device get(Tsr tensor);

    Device add(Tsr tensor);
    Device add(Tsr tensor, Tsr parent);

    boolean has(Tsr tensor);

    Device rmv(Tsr tensor);

    Device overwrite64(Tsr tensor, double[] value);

    Device overwrite32(Tsr tensor, float[] value);

    Device swap(Tsr former, Tsr replacement);



    Device execute(Tsr[] tsrs, int f_id, int d);

    double[] value64Of(Tsr tensor, boolean grd);

    float[] value32Of(Tsr tensor, boolean grd);

    Collection<Tsr> tensors();

}
