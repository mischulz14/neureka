package neureka.core.device;

import neureka.core.Tsr;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakTensorReference extends WeakReference<Tsr>
{
    /**
     * Creates a new phantom reference that refers to the given object and
     * is registered with the given queue.
     *
     * <p> It is possible to create a phantom reference with a {@code null}
     * queue, but such a reference is completely useless: Its {@code get}
     * method will always return {@code null} and, since it does not have a queue,
     * it will never be enqueued.
     *
     * @param referent the object the new phantom reference will refer to
     * @param q        the queue with which the reference is to be registered,
     *                 or {@code null} if registration is not required
     */
    private int _hash;

    public WeakTensorReference(Tsr referent, ReferenceQueue<? super Tsr> q) {
        super(referent, q);
        _hash = referent.hashCode();
    }

    @Override
    public int hashCode(){
        //Tsr t = this.get();
        //if(t!=null){
        //    return t.hashCode();
        //}
        return _hash;
    }

    //@Override
    //public boolean equals(Object o){
    //    return o.hashCode() == _hash;
    //}

}
