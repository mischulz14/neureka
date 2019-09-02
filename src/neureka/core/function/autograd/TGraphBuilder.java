package neureka.core.function.autograd;

import neureka.core.T;
import neureka.core.function.TFunction;

import java.util.function.Consumer;

public class TGraphBuilder
{

    public static void connect(T drain, T[] src, TFunction function, boolean derive){//, boolean derive
        if(function.isFlat()&&derive){
            _performDifferentiation(drain, function, src);
        }
    }

    private static void _performDifferentiation(T drain, TFunction function, T[] source)
    {//--------------------------------------------------------------------------------------
        TGraphLock gid = ((TGraphNode)source[0].find(TGraphNode.class)).gid();
        TGraphNode node = new TGraphNode(drain,function, source, gid);
        drain.add(node);
        if(node.usesAD() && function.isFlat()){
            /**
             *  Preparing for back propagation:
             * */
            if(node.usesForwardAD()){
                int i = 0;
                for(T src : source){
                    TGraphNode src_node = ((TGraphNode) src.find(TGraphNode.class));
                    if(src_node.function()!=null && src_node.function().id()==18){
                        T d = function.derive(source, i);
                        node.put(src, d);// Sources created by x-mul are revers-mode cases!
                    }else{
                        if(src_node.usesAD()){
                            T d = function.derive(source, i);
                            if(src_node.size()==0 && node.size()==0){
                                node.put(source[i], d);
                            } else {
                                src_node.forEach(
                                    (t, g)->{
                                    /**
                                     *  Chain rule (forward) for every
                                     *  _gradient w.r.t. leaves (reverseAD or user leaves):
                                     * */
                                    if(node.has(t)){
                                        T dg = node.get(t);
                                        node.put(t, T.factory.addition(dg,T.factory.multiplication(d, g)));
                                    }else{
                                        node.put(t, T.factory.multiplication(d, g));
                                    }
                                    //TODO: flag within src tsrs that grant that the tensor has been created by function constructor!
                                });
                            }

                        }
                        i++;
                    }
                }
            }else if(node.usesReverseAD()) {
                int i = 0;
                for(T src : source){
                    TGraphNode src_node = ((TGraphNode) src.find(TGraphNode.class));
                    if(src_node.mode()!=0 || src.rqsGradient()){
                        T d = function.derive(source, i);
                        node.put(src, d);// Add gradients with respect to every source tensor!
                    }
                    i++;
                }
            }
        }
    }
    //--------------------------------------------------------------------------------------

    public static void backward(T error, T drain){
        if(true){
            return;
        }
        if(drain.rqsGradient()){
            drain.setGradient(
                    (drain.gradient()==null)
                            ?error
                            :T.factory.addition(error, T.factory.newTensor(drain.value(), drain.shape()))
            );
        }
        TGraphNode drnGradients = (TGraphNode) drain.find(TGraphNode.class);
        if(drnGradients!=null){
            drnGradients.forEach((target, g)->{
             //   target.backward(T.factory.multiplication(error, g));
            });
        }
    }

}



