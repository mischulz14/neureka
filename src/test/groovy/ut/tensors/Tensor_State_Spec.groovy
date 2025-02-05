package ut.tensors

import neureka.Neureka
import neureka.Tsr
import neureka.dtype.DataType
import neureka.dtype.custom.I8
import neureka.view.NDPrintSettings
import spock.lang.IgnoreIf
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("The Tensor Initialization and State Specification")
@Narrative('''
    
    This specification defines the expected states of freshly instantiated
    and initialized tensors.
    After a tensor was created successfully we expect it 
    to have certain properties like a shape, rank, type and data array
    among other things.
    
''')
class Tensor_State_Spec extends Specification
{
    def setupSpec() {
        reportHeader """    
            Note: This specification is a little older, meaning initially it was not written with 
            the intend to be read as living documentation, but rather as a unit test...
        """
    }

    def setup()
    {
        Neureka.get().reset()
        // Configure printing of tensors to be more compact:
        Neureka.get().settings().view().ndArrays({ NDPrintSettings it ->
            it.isScientific      = true
            it.isMultiline       = false
            it.hasGradient       = true
            it.cellSize          = 15
            it.hasValue          = true
            it.hasRecursiveGraph = false
            it.hasDerivatives    = true
            it.hasShape          = true
            it.isCellBound       = false
            it.postfix           = ""
            it.prefix            = ""
            it.hasSlimNumbers    = false
        })
    }

    def 'A tensor can be instantiated from a item type class and nested lists.'(
            Class<Object> type, List<Object> list, List<Integer> shape, Object expected
    ) {
        given : 'We instantiate a tensor using a type and a list of things (or list of list of things, or..).'
            var t = Tsr.of(type, list)

        expect : 'The tensor has the item type, shape and data array!'
            t.itemType == type
            t.shape() == shape
            t.mut.data.ref == expected

        where :
            type   | list        || shape    | expected
            Double | [1,2,1]     || [3]      | [1, 2, 1] as double[]
            Float  | [5, -4]     || [2]      | [5, -4] as float[]
            Byte   | [3, 4]      || [2]      | [3, 4] as byte[]
            Byte   | [[3], [4]]  || [2, 1]   | [3, 4] as byte[]
            String | [['3', '4']]|| [1, 2]   | ['3', '4'] as String[]

    }

    def 'Tensors as String can be formatted on an entry based level.'()
    {
        given : 'A new tensor of rank 2 storing Strings:'
            Tsr t = Tsr.of(DataType.of(String.class), [2, 3], (i, indices) -> {
                return ["sweet", "salty", "blue", "spinning", "confused", "shining"].get( (i + 17**i)%6 ) + ' ' +
                       ["Saitan", "Apple", "Tofu",  "Strawberry", "Almond", "Salad"].get( (i + 7**i)%6  )
            })

        expect: 'The tensor converted to a String with custom cell bounds, size, etc... should be as expected:'
            t.toString({ NDPrintSettings it ->
                                      it.setHasSlimNumbers(false)
                                      .setIsScientific(true)
                                      .setIsCellBound(true)
                                      .setIsMultiline(true)
                                      .setCellSize(5)
                                })  == "(2x3):[\n" +
                                         "   [ sal.., swe.., spi.. ],\n" +
                                         "   [ blu.., shi.., con.. ]\n" +
                                         "]"
        and: 'When increase the cell size, most String entries will be printed fully.'
            t.toString({ NDPrintSettings it ->
                                it.setHasSlimNumbers(false)
                                .setIsScientific(true)
                                .setIsCellBound(true)
                                .setIsMultiline(true)
                                .setCellSize(15)
                    }) == "(2x3):[\n" +
                                       "   [   salty Apple  ,    sweet Tofu  , spinning Stra.. ],\n" +
                                       "   [   blue Almond  ,  shining Salad , confused Saitan ]\n" +
                                       "]"
        and: 'Now we try a cell size of 10:'
            t.toString(
                    { NDPrintSettings it -> it.setIsCellBound(true).setCellSize(10)}
            ) == "(2x3):[salty Ap.., sweet Tofu, spinning.., blue Alm.., shining .., confused..]"

        and: 'We can also configure a postfix and prefix as well as limit the number of entries in a row:'
            t.toString({ NDPrintSettings it ->
                                        it.setPrefix('START<|').setPostfix('|>END').setCellSize(0).setIsCellBound(false).setRowLimit(2) }
            ) == "START<|(2x3):[salty Apple, sweet Tofu, ... + 4 more]|>END"
    }


    def 'Numeric tensors as String can be formatted on an entry based level.'()
    {
        given : 'A new tensor of rank 2 storing floats:'
            var t = Tsr.of(DataType.of(Float.class), [2, 3], (i, indices) -> (i%4)/3 as float )

        expect : 'When we convert the tensor to a String with scientific formatting.'
            t.toString(
                    { NDPrintSettings it -> it.setHasSlimNumbers(false).setIsScientific(true).setIsCellBound(false).setIsMultiline(false).setCellSize(3) }
            ) == "(2x3):[0.0, 0.33333, 0.66666, 1.0, 0.0, 0.33333]"
        and : 'If the numbers are still to verbose, we can make them "slim":'
            t.toString(
                { NDPrintSettings it -> it.setHasSlimNumbers(true).setIsScientific(true).setCellSize(0) }
                ) == "(2x3):[0, .33333, .66666, 1, 0, .33333]"
    }


    def 'Tensors as String can be formatted depending on shape.'(
            List<Integer> shape, String mode, String expected
    ){
        given: 'We configure a NDPrintSettings object.'
            def settings =
                    Neureka.get()
                            .settings()
                            .view()
                            .getNDPrintSettings()
                            .clone()
                            .setRowLimit(  mode.contains( "s" ) ? 3 : 32 )
                            .setIsScientific( mode.contains( "c" )  )
                            .setIsMultiline( mode.contains( "f" ) )
                            .setHasGradient( mode.contains( "g" ) )
                            .setCellSize(  mode.contains( "p" ) ? 6 : mode.contains( "f" ) ? 2 : 1  )
                            .setHasValue( !(mode.contains( "shp" ) || mode.contains("shape")) )
                            .setHasRecursiveGraph( mode.contains( "r" ) )
                            .setHasDerivatives(  mode.contains( "d" ) )
                            .setHasShape(  !mode.contains( "v" ) )
                            .setIsCellBound(  mode .contains( "b" ) )
                            .setPostfix(  "" )
                            .setPrefix(  "" )
                            .setHasSlimNumbers(  false )

        and : 'Four tensors of various data types:'
            Tsr<Float>   t1 = Tsr.of( Float.class,   shape, -4f..5f ).set( Tsr.of( shape, -7f..3f ) )
            Tsr<Double>  t2 = Tsr.of( Double.class,  shape, -4d..5d ).set( Tsr.of( shape, -7d..3d ) )
            Tsr<Integer> t3 = Tsr.of( Integer.class, shape, -4..5   ).set( Tsr.of( shape, -7..3   ) )
            Tsr<Short>   t4 = Tsr.of( Short.class,   shape, (-4 as short)..(5 as short) ).set( Tsr.of( shape, (-7 as short)..(3 as short) ) )
            Tsr<Byte>    t5 = Tsr.of( Byte.class,    shape, (-4 as byte )..(5 as byte ) ).set( Tsr.of( shape, (-7 as byte)..(3 as byte) ) )

        expect: 'The first tensor has the expected internals and produces the correct String representation.'
            t1.toString(settings) == expected
            t1.dataType == DataType.of( Float.class )
            t1.mut.data.ref instanceof float[]
        and : 'The second tensor has the expected internals and produces the correct String representation.'
            t2.toString(settings) == expected
            t2.dataType == DataType.of( Double.class )
            t2.mut.data.ref instanceof double[]
        and : 'The third tensor has the expected internals and produces the correct String representation.'
            t3.toString(settings).replace(' ','') == expected.replace('.0','  ').replace(' ','')
            t3.dataType == DataType.of( Integer.class )
            t3.mut.data.ref instanceof int[]
        and : 'The fourth tensor has the expected internals and produces the correct String representation.'
            t4.toString(settings).replace(' ','') == expected.replace('.0','  ').replace(' ','')
            t4.dataType == DataType.of( Short.class )
            t4.mut.data.ref instanceof short[]
        and : 'The fifth tensor has the expected internals and produces the correct String representation.'
            t5.toString(settings).replace(' ','') == expected.replace('.0','  ').replace(' ','')
            t5.dataType == DataType.of( Byte.class )
            t5.mut.data.ref instanceof byte[]

        where : 'The print configurations codes "mode", a common shape and expected String representation will be supplied:'
            shape     | mode | expected
            [2,3]     | "fap"| "(2x3):[\n   [  -4.0 ,  -3.0 ,  -2.0  ],\n   [  -1.0 ,   0.0 ,   1.0  ]\n]"
            [2,3]     | "fa" | "(2x3):[\n   [ -4.0, -3.0, -2.0 ],\n   [ -1.0, 0.0, 1.0 ]\n]"
            [3,2]     | "fp" | "(3x2):[\n   [  -4.0 ,  -3.0  ],\n   [  -2.0 ,  -1.0  ],\n   [   0.0 ,   1.0  ]\n]"
            [2,3,4]   | "fp" | "(2x3x4):[\n   [\n      [  -4.0 ,  -3.0 ,  -2.0 ,  -1.0  ],\n      [   0.0 ,   1.0 ,   2.0 ,   3.0  ],\n      [   4.0 ,   5.0 ,  -4.0 ,  -3.0  ]\n   ],\n   [\n      [  -2.0 ,  -1.0 ,   0.0 ,   1.0  ],\n      [   2.0 ,   3.0 ,   4.0 ,   5.0  ],\n      [  -4.0 ,  -3.0 ,  -2.0 ,  -1.0  ]\n   ]\n]"
            [2,2,3,4] | "fp" | "(2x2x3x4):[\n   [\n      [\n         [  -4.0 ,  -3.0 ,  -2.0 ,  -1.0  ],\n         [   0.0 ,   1.0 ,   2.0 ,   3.0  ],\n         [   4.0 ,   5.0 ,  -4.0 ,  -3.0  ]\n      ],\n      [\n         [  -2.0 ,  -1.0 ,   0.0 ,   1.0  ],\n         [   2.0 ,   3.0 ,   4.0 ,   5.0  ],\n         [  -4.0 ,  -3.0 ,  -2.0 ,  -1.0  ]\n      ]\n   ],\n   [\n      [\n         [   0.0 ,   1.0 ,   2.0 ,   3.0  ],\n         [   4.0 ,   5.0 ,  -4.0 ,  -3.0  ],\n         [  -2.0 ,  -1.0 ,   0.0 ,   1.0  ]\n      ],\n      [\n         [   2.0 ,   3.0 ,   4.0 ,   5.0  ],\n         [  -4.0 ,  -3.0 ,  -2.0 ,  -1.0  ],\n         [   0.0 ,   1.0 ,   2.0 ,   3.0  ]\n      ]\n   ]\n]"
            [2, 70]   | "f"  | "(2x70):[\n   [ -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, ..38 more.., 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 ],\n   [ -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, ..38 more.., 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 ]\n]"
            [2, 100]  | "f"  | "(2x100):[\n   [ -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, ..68 more.., 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 ],\n   [ -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, ..68 more.., 1.0, 2.0, 3.0, 4.0, 5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 ]\n]"
            [70, 2]   | "f"  | "(70x2):[\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ],\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ],\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ],\n   [ -4.0, -3.0 ],\n   ... 38 more ...\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ],\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ],\n   [ -4.0, -3.0 ],\n   [ -2.0, -1.0 ],\n   [ 0.0, 1.0 ],\n   [ 2.0, 3.0 ],\n   [ 4.0, 5.0 ]\n]"
    }



    def 'We can create scalar tensors.'()
    {
        given: 'A new instance of a scalar tensor.'
            Tsr<Double> t = Tsr.of( 6d )
        expect: 'The tensor is not stored on another device, meaning that it is not "outsourced".'
            !t.isOutsourced()
        and : 'The tensor contains the expected data.'
            t.mut.data.ref == [6] as double[]
            t.items == [6d]
            t.rawItems == [6] as double[]
        and : 'We can read the data in various array types:'
            t.getItemsAs( double[].class ) == [6] as double[]
            t.getItemsAs( float[].class  ) == [6] as float[]
            t.getDataAs( double[].class ) == [6] as double[]
            t.getDataAs( float[].class  ) == [6] as float[]
    }

    def 'Tensor created from shape and datatype has expected state.'()
    {
        given : 'A new vector tensor is being instantiated.'
            Tsr<Byte> t = Tsr.of(  DataType.of(I8.class ), new int[]{ 2 } )
        expect : 'The vector is initialized with zeros.'
            t.items == [0, 0] as List<Byte>
        and : 'We can access and verify this through the following ways as well:'
            t.getItemsAs( double[].class ) == [0, 0] as double[]
            t.getItemsAs( float[].class ) == [0, 0] as float[]
            t.mut.data.ref == [0] as byte[]
        and : 'It is not stored on another device, meaning that it is not "outsourced".'
            !t.isOutsourced()
            t.isVirtual()
    }

    @IgnoreIf({ data.device == 'GPU' && !Neureka.get().canAccessOpenCLDevice() }) // We need to assure that this system supports OpenCL!
    def 'The data and the value of a tensor a 2 different things!'(
        String device
    ) {
        given : 'We create a simple vector:'
            var v = Tsr.ofFloats().withShape(3).andFill(-2, 4, 8)
        and : 'And then we store it on the device we want to test.'
            v.to(device)

        when : 'We create a slice of the above vector, a scalar...'
            var s = v.slice().axis(0).at(1).get()

        then : 'The slice contains the expected value with respect to the slice parent...'
            v.at(1).get() == s.at(0).get()

        and : 'They both do not share the same value array.'
            v.items != s.items
        and : 'They do however share the same underlying data.'
            v.mut.data.ref == s.mut.data.ref
        and : 'The tensor simply stores the number 4.'
            s.items == [4f]

        where : 'We test the following devices:'
            device << ['CPU', 'GPU']
    }


}
