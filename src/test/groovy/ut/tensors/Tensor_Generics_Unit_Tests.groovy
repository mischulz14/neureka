package ut.tensors

import neureka.Tsr
import spock.lang.Specification

class Tensor_Generics_Unit_Tests extends Specification
{

    def 'Anonymous tensor instance has no inherent generic type class.'() {

        given :
            Tsr<Double> t = new Tsr()

        expect :
            t.getValueClass() == null

    }

    def 'String tensor instance discovers expected class.'(){

        given :
            Tsr t = new Tsr([2, 4], ["Hi", "I'm", "a", "String", "list"])

        expect :
            t.getValueClass() == String.class

        and :
            t.toString() == "(2x4):[Hi, I'm, a, String, list, Hi, I'm, a]"

    }



}
