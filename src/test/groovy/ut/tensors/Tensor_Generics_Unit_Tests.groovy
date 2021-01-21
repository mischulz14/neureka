package ut.tensors

import neureka.Neureka
import neureka.Tsr
import neureka.utility.TsrAsString
import spock.lang.Specification

class Tensor_Generics_Unit_Tests extends Specification
{
    def setupSpec()
    {
        reportHeader """
                <h2> Tensors as Containers </h2>
                <br> 
                <p>
                    Tensors do not just store numeric data.
                    They can hold anything which can be stuffed into a "Object[]" array.
                    You could even create a tensor of tensors!            
                </p>
            """
    }

    def setup() {
        Neureka.instance().reset()
        // Configure printing of tensors to be more compact:
        Neureka.instance().settings().view().asString = "dgc"
    }

    def 'Anonymous tensor instance has the default datatype class as defined in Neureka settings.'() {

        given :
            Tsr<Double> t = new Tsr()

        expect :
            t.getValueClass() == Neureka.instance().settings().dtype().defaultDataTypeClass

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
