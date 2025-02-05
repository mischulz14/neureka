package ut.tensors

import neureka.Neureka
import neureka.Tsr
import neureka.calculus.Function
import neureka.calculus.args.Arg
import neureka.calculus.assembly.FunctionParser
import neureka.common.utility.SettingsLoader
import neureka.devices.Device
import neureka.devices.host.CPU
import neureka.dtype.DataType
import neureka.view.NDPrintSettings
import spock.lang.IgnoreIf
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import testutility.Statistics

import java.util.function.BiFunction

@Title("Running Tensors through operations")
@Narrative('''

    This specification covers the interaction 
    between tensors and operations, more specifically it
    runs tensors through operations and validates that the results are valid. 

''')
class Tensor_Operation_Spec extends Specification
{
    def setup() {
        // The following is similar to Neureka.get().reset() however it uses a groovy script for library settings:
        SettingsLoader.tryGroovyScriptsOn(Neureka.get(), script -> new GroovyShell(getClass().getClassLoader()).evaluate(script))
        // Configure printing of tensors to be more compact:
        Neureka.get().settings().view().ndArrays({ NDPrintSettings it ->
            it.isScientific      = true
            it.isMultiline       = false
            it.hasGradient       = true
            it.cellSize          = 1
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

    def 'The "dot" operation reshapes and produces valid "x" operation result.'( Class<?> type )
    {
        given : 'Two multi-dimensional tensors.'
            var a = Tsr.of([1, 4, 4, 1   ], 4f..12f).mut.toType(type)
            var b = Tsr.of([1, 3, 5, 2, 1], -5d..3d).mut.toType(type)

        when : 'The "dot" method is being called on "a" receiving "b"...'
            var c = a.convDot(b)

        then : 'The result tensor contains the expected shape.'
            c.toString().contains("(4x2x5x2)")
        and :
            c.itemType == type

        where :
            type << [Double, Float]
    }


    def 'The "matMul" operation produces the expected result.'(
            Class<?> type, Double[] A, Double[] B, int M, int K, int N, double[] expectedC
    ) {
        given : 'Two 2-dimensional tensors.'
            var a = Tsr.of(Double.class).withShape(M, K).andFill(A).mut.toType(type)
            var b = Tsr.of(Double.class).withShape(K, N).andFill(B).mut.toType(type)

        when : 'The "matMul" method is being called on "a" receiving "b"...'
            var c = a.matMul(b)

        then : 'The result tensor contains the expected shape and values.'
            c.shape == [M, N]
            c.items == expectedC as List
        and :
            c.itemType == type

        where : 'We use the following data and matrix dimensions!'
            type   | A            | B                  | M | K | N || expectedC
            Double | [4, 3, 2, 1] | [-0.5, 1.5, 1, -2] | 2 | 2 | 2 || [ 1, 0, 0, 1 ]
            Double | [-2, 1]      | [-1, -1.5]         | 1 | 2 | 1 || [ 0.5 ]
            Double | [-2, 1]      | [-1, -1.5]         | 2 | 1 | 2 || [ 2.0, 3.0, -1.0, -1.5 ]
            Float  | [4, 3, 2, 1] | [-0.5, 1.5, 1, -2] | 2 | 2 | 2 || [ 1, 0, 0, 1 ]
            Float  | [-2, 1]      | [-1, -1.5]         | 1 | 2 | 1 || [ 0.5 ]
            Float  | [-2, 1]      | [-1, -1.5]         | 2 | 1 | 2 || [ 2.0, 3.0, -1.0, -1.5 ]
            Long   | [4, 3, 2, 1] | [-0.5, 1.5, 1, -2] | 2 | 2 | 2 || [ 3, -2, 1, 0 ]
            Long   | [-2, 1]      | [-1, -1.5]         | 1 | 2 | 1 || [ 1 ]
            Long   | [-2, 1]      | [-1, -1.5]         | 2 | 1 | 2 || [ 2, 2, -1, -1 ]
            Integer| [4, 3, 2, 1] | [-0.5, 1.5, 1, -2] | 2 | 2 | 2 || [ 3, -2, 1, 0 ]
            Integer| [-2, 1]      | [-1, -1.5]         | 1 | 2 | 1 || [ 1 ]
            Integer| [-2, 1]      | [-1, -1.5]         | 2 | 1 | 2 || [ 2, 2, -1, -1 ]
    }

    def 'The "random" function/operation populates tensors randomly.'(
            Class<?> type
    ) {
        given :
            var t = Tsr.of(type).withShape(2,4).all(-42)
        and :
            var f = Function.of('random(I[0])')
        expect :
            t.itemType == type

        when :
            var r = f(t)

        then :
            r === t
        and :
            ( r.mut.data.ref as float[] ) == [1.0588075, 1.4017555, 1.2537496, -1.3897222, 1.0374786, 0.743316, 1.1692946, 1.3977289] as float[]

        when :
            r = f.with(Arg.Seed.of(42)).call(t)

        then :
            r === t
        and :
            ( r.mut.data.ref as float[] ) == [2.2639139286289724, -0.2763464310754003, 0.3719153742868813, -0.9768504740489802, 0.5154099159307729, 1.1608137295804097, 2.1905023977046336, -0.5449569795660217] as float[]

        where :
            type << [Double, Float]
    }

    def 'The values of a randomly populated tensor seems to adhere to a gaussian distribution.'(
            Class<?> type
    ) {

        given :
            var t = Tsr.of(type).withShape(20, 40, 20).all(0)
        and :
            var f = Function.of('random(I[0])')

        when :
            f.with(Arg.Seed.of(-73L)).call(t)
            var stats = new Statistics( t.mut.data.ref as double[] )
        then :
            -0.05d < stats.mean && stats.mean < 0.05d
        and :
            0.875d < stats.variance && stats.variance < 1.125d

        where :
            type << [Double, Float]
    }

    def 'New method "asFunction" of String added at runtime is callable by groovy and also works.'(
            Class<?> type, String code, String expected
    ) {
        given : 'We create two tensors and convert them to a desired type.'
            var a = Tsr.of([1,2], [3d, 2d]).mut.toType(type)
            var b = Tsr.of([2,1], [-1f, 4f]).mut.toType(type)
        and : 'We prepare bindings for the Groovy shell.'
            Binding binding = new Binding()
            binding.setVariable('a', a)
            binding.setVariable('b', b)

        expect : 'The tensors have the type...'
            a.itemType == type
            b.itemType == type

        when : 'The groovy code is being evaluated.'
            var c = new GroovyShell(binding).evaluate((code)) as Tsr

        then : 'The resulting tensor (toString) will contain the expected String.'
            c.toString().contains(expected)
        and :
            c.itemType == type

        where :
            type   | code                               || expected
            Double | '"I[0]xI[1]".asFunction()([a, b])' || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Double | '"I[0]xI[1]"[a, b]'                || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Double | '"i0 x i1"%[a, b]'                 || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Double | '"i0"%a'                           || "(1x2):[3.0, 2.0]"
            Float  | '"I[0]xI[1]".asFunction()([a, b])' || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Float  | '"I[0]xI[1]"[a, b]'                || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Float  | '"i0 x i1"%[a, b]'                 || "(2x2):[-3.0, -2.0, 12.0, 8.0]"
            Float  | '"i0"%a'                           || "(1x2):[3.0, 2.0]"
    }

    def 'New operator methods added to "SDK-types" at runtime are callable by groovy and also work.'(
            Class<?> type, String code, String expected
    ) {
        given :
            Neureka.get().settings().view().ndArrays({it.hasSlimNumbers=true})
            Tsr a = Tsr.of(5d).mut.toType(type)
            Tsr b = Tsr.of(3f).mut.toType(type)
            Binding binding = new Binding()
            binding.setVariable('a', a)
            binding.setVariable('b', b)

        when : '...calling methods on types like Double and Integer that receive Tsr instances...'
            Tsr c = new GroovyShell(binding).evaluate((code)) as Tsr

        then : 'The resulting tensor (toString) will contain the expected String.'
            c.toString().endsWith("[$expected]")

        where :
            type   | code       || expected
            Double | '(2+a)'    || "7"
            Double | '(2*b)'    || "6"
            Double | '(6/b)'    || "2"
            //Double | '(2^b)'   || "8"
            Double | '(2**b)'   || "8"
            Double | '(4-a)'    || "-1"
            Double | '(2.0+a)'  || "7"
            Double | '(2.0*b)'  || "6"
            Double | '(6.0/b)'  || "2"
            //Double | '(2.0^b)'  || "8"
            Double | '(2.0**b)' || "8"
            Double | '(4.0-a)'  || "-1"
            Float  | '(2+a)'    || "7"
            Float  | '(2*b)'    || "6"
            Float  | '(6/b)'    || "2"
            //Float  | '(2^b)'    || "8"
            Float  | '(2**b)'   || "8"
            Float  | '(4-a)'    || "-1"
            Float  | '(2.0+a)'  || "7"
            Float  | '(2.0*b)'  || "6"
            Float  | '(6.0/b)'  || "2"
            //Float  | '(2.0^b)'  || "8"
            Float  | '(2.0**b)' || "8"
            Float  | '(4.0-a)'  || "-1"

    }

    def 'Overloaded operation methods on tensors produce expected results when called.'()
    {
        given :
            Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(true)
            Tsr a = Tsr.of(2d).setRqsGradient(true)
            Tsr b = Tsr.of(-4d)
            Tsr c = Tsr.of(3d).setRqsGradient(true)

        expect :
            ( a / a                      ).toString().contains("[1]:(1.0)")
            ( c % a                      ).toString().contains("[1]:(1.0)")
            ( ( ( b / b ) ** c % a ) * 3 ).toString().contains("[1]:(3.0)")
            ( a *= b                     ).toString().contains("(-8.0)")
            ( a += -c                    ).toString().contains("(-11.0)")
            ( a -= c                     ).toString().contains("(-14.0)")
            ( a /= Tsr.of(2d)      ).toString().contains("(-7.0)")
            ( a %= c                     ).toString().contains("(-1.0)")
    }

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.device == null }) // We need to assure that this system supports OpenCL!
    def 'Simple slice addition produces expected result.'(
            Device device
    ) {
        given :
            Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(false)
            Tsr a = Tsr.of([11, 11], 3d..19d).to( device )
            Tsr x = a[1..-2,0..-1]
            Tsr y = a[0..-3,0..-1]

        when :
            Tsr t = x + y
            String tAsStr = t.toString({it.setRowLimit(50)})

        then :
            tAsStr.contains("(9x11):[17.0, 19.0, 21.0, 23.0, 25.0, 27.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, " +
                    "26.0, 28.0, 30.0, 32.0, 17.0, 19.0, 21.0, 23.0, 25.0, 27.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, " +
                    "26.0, 28.0, 30.0, 32.0, 17.0, 19.0, 21.0, 23.0, 25.0, 27.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0, 28.0, 30.0, ... + 49 more]")

        where : 'The following data is being used for tensor instantiation :'
            device  << [
                    CPU.get(),
                    Device.get("openCL")
            ]
    }

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.device == 'GPU' })
    def 'Auto reshaping and broadcasting works and the result can be back propagated.'(// TODO: Cover more broadcasting operations!
            Class<Object> type, boolean whichGrad, List<Integer> bShape,
            BiFunction<Tsr<?>, Tsr<?>, Tsr<?>> operation, String cValue, String wGradient, String device
    ) {
        given :
            Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(true)
        and :
            String wValue = whichGrad
                                ? "8" + ( bShape.inject(1, {x,y->x*y}) > 1 ? ", 9" : "" )
                                : "1, 2, 3, 4"
        and :
            def aShape = [2, 2]
        and :
            Tsr<Double> a = Tsr.of(aShape, 1d..5d).setRqsGradient(!whichGrad).to(Device.get(device))
            Tsr<Double> b = Tsr.of(bShape, 8d..9d).setRqsGradient(whichGrad).to(Device.get(device))
        and :
            a.mut.toType(type)
            b.mut.toType(type)
        and :
            String wShape = ( whichGrad ? bShape : aShape ).join("x")
            Tsr    w      = ( whichGrad ? b      : a      )

        expect :
            a.itemType == type
            b.itemType == type

        when :
            Tsr c = operation.apply(a, b)
        then :
            c.toString({it.hasSlimNumbers = true}).startsWith("[2x2]:($cValue)")
            w.toString({it.hasSlimNumbers = true}) == "[$wShape]:($wValue):g:(null)"

        when :
            c.backward(Tsr.of([2, 2], [5, -2, 7, 3]).mut.toType(type))
        then :
            w.toString({it.hasSlimNumbers = true}) == "[$wShape]:($wValue):g:($wGradient)"

        when :
            Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(false)
        then :
            c.toString({it.hasSlimNumbers = true}) == "(2x2):[$cValue]"

        where:
            device | type   | whichGrad | bShape |    operation      ||     cValue      | wGradient

            'CPU'  | Double | false     | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "5, -2, 7, 3"
            'GPU'  | Double | false     | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "5, -2, 7, 3"
            'CPU'  | Float  | false     | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "5, -2, 7, 3"
            'GPU'  | Float  | false     | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "5, -2, 7, 3"

            'CPU'  | Double | false     | [1]    | { x, y -> x * y } || "8, 16, 24, 32" | "40, -16, 56, 24"
            'GPU'  | Double | false     | [1]    | { x, y -> x * y } || "8, 16, 24, 32" | "40, -16, 56, 24"
            'CPU'  | Float  | false     | [1]    | { x, y -> x * y } || "8, 16, 24, 32" | "40, -16, 56, 24"
            'GPU'  | Float  | false     | [1]    | { x, y -> x * y } || "8, 16, 24, 32" | "40, -16, 56, 24"

            'CPU'  | Double | true      | [2,1]  | { x, y -> x + y } || "9, 10, 12, 13" | "3, 10"
            //'GPU'  | Double |  true     | [2,1]  | { x, y -> x + y } || "9, 10, 12, 13" | "3, 10"
            'CPU'  | Float  | true      | [2,1]  | { x, y -> x + y } || "9, 10, 12, 13" | "3, 10"
            //'GPU'  | Float  |  true     | [2,1]  | { x, y -> x + y } || "9, 10, 12, 13" | "3, 10"

            'CPU'  | Double | true      | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "13"
            'GPU'  | Double | true      | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "13"
            'CPU'  | Float  | true      | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "13"
            'GPU'  | Float  | true      | [1]    | { x, y -> x + y } || "9, 10, 11, 12" | "13"

            'CPU'  | Double | true      | [1,2]  | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'GPU'  | Double | true      | [1,2]  | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'CPU'  | Float  | true      | [1,2]  | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'GPU'  | Float  | true      | [1,2]  | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"

            'CPU'  | Double | true      | [2]    | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'GPU'  | Double | true      | [2]    | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'CPU'  | Float  | true      | [2]    | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"
            'GPU'  | Float  | true      | [2]    | { x, y -> x + y } || "9, 11, 11, 13" | "12, 1"

            'CPU'  | Double | true      | [1,2]  | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'GPU'  | Double | true      | [1,2]  | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'CPU'  | Float  | true      | [1,2]  | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'GPU'  | Float  | true      | [1,2]  | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"

            'CPU'  | Double | true      | [2]    | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'GPU'  | Double | true      | [2]    | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'CPU'  | Float  | true      | [2]    | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"
            'GPU'  | Float  | true      | [2]    | { x, y -> x - y } || "-7, -7, -5, -5"| "-12, -1"

            'CPU'  | Double | true      | [1,2]  | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'GPU'  | Double | true      | [1,2]  | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'CPU'  | Float  | true      | [1,2]  | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'GPU'  | Float  | true      | [1,2]  | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"

            'CPU'  | Double | true      | [2]    | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'GPU'  | Double | true      | [2]    | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'CPU'  | Float  | true      | [2]    | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
            'GPU'  | Float  | true      | [2]    | { x, y -> y - x } || "7, 7, 5, 5"    | "12, 1"
    }

    def 'Operators "+,*,**" produce expected results with gradients which can be accessed via a "Ig[0]" Function instance'()
    {
        given : 'Neurekas view is set to legacy and three tensors of which one requires gradients.'
            Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(true)
            Tsr x = Tsr.of(3d).setRqsGradient(true)
            Tsr b = Tsr.of(-4d)
            Tsr w = Tsr.of(2d)

        when : Tsr y = ( (x+b)*w )**2

        then : y.toString().contains("[1]:(4.0); ->d[1]:(-8.0)")

        when : y = ((x+b)*w)**2
        then : y.toString().contains("[1]:(4.0); ->d[1]:(-8.0)")

        and : Neureka.get().settings().debug().setIsKeepingDerivativeTargetPayloads(true)

        when :
            y.backward(Tsr.of(1d))
        and :
            Tsr t2 = Tsr.of( "Ig[0]", [x] )
            Tsr t1 = Tsr.of( "Ig[0]", [y] ) // The input does not have a gradient!

        then :
            thrown(IllegalArgumentException)
        and :
            t2.toString() == "[1]:(-8.0)"
        and :
            t2 == x.gradient

        and : Neureka.get().settings().debug().setIsKeepingDerivativeTargetPayloads(false)

        when :
            Tsr[] trs = new Tsr[]{x}
        and :
            def fun = new FunctionParser( Neureka.get().backend() ).parse("Ig[0]", false)
        then :
            fun(trs).toString() == "[1]:(-8.0)"

        when :
            trs[0] = y
        and :
            fun = new FunctionParser( Neureka.get().backend() ).parse("Ig[0]", false)
        and :
            fun(trs)

        then :
            thrown(IllegalArgumentException)
    }


    def 'Activation functions work across types on slices and non sliced tensors.'(
            Class<?> type, String funExpression
    ) {
        given : 'We create a function based on the provided expression.'
            var func = Function.of(funExpression)
        and : 'We create 2 tensors storing the same values, one sliced and the other a normal tensor.'
            var t1 = Tsr.of(type).withShape(2, 3).andSeed("Tempeh")
            var t2 = Tsr.of(type).withShape(4, 5).all(0)[1..2, 1..3]
            t2.mut[0..1, 0..2] = t1

        expect : 'The types of both tensors should match what was provided during instantiation.'
            t1.dataType == DataType.of(type)
            t1.itemType == type
            t2.dataType == DataType.of(type)
            t2.itemType == type

        when : 'We apply the function to both tensors...'
            var result1 = func(t1)
            var result2 = func(t2)
        then :
            result1.itemType == type
            result2.itemType == type

        and : 'The data of the first (non slice) tensor should be as expected.'
            result1.mut.data.ref == expected instanceof Map ? expected['r1'] : expected
        and : 'As well the value of the slice tensor (Its data would be a sparse array).'
            result2.items == expected instanceof Map ? expected['r2'] : expected

        where :
            type   |  funExpression     || expected

            Double | 'tanh(i0)'         || [-0.2608431635405718, -0.6400224689534015, -0.15255723053856546, 0.1566537867655921, 0.5489211983894932, -0.17031712209680225] as double[]
            Float  | 'tanh(i0)'         || [-0.26084316, -0.64002246, -0.15255724, 0.15665378, 0.54892117, -0.17031713] as float[]
            Integer| 'tanh(i0)'         || [-1, -1, 1, -1, 1, -1] as int[]

            Double | 'relu(i0)'         || [-0.0027019706408068795, -0.008329762613111082, -0.001543641184315801, 0.15861207834235577, 0.6567031992927272, -0.001728424711189524] as double[]
            Float  | 'relu(i0)'         || [-0.0027019705, -0.008329763, -0.0015436412, 0.15861207, 0.6567032, -0.0017284247] as float[]
            Integer| 'relu(i0)'         || [-7156386, -18495716, 248181051, -13634228, 919305478, -15169971] as int[]

            Double | 'relu(i0*i0)'      || [0.07300645343782339, 0.6938494519078316, 0.023828281059158886, 0.025157791396081604, 0.43125909196130346, 0.029874519822505895] as double[]
            Float  | 'relu(i0*i0)'      || [0.07300645, 0.6938495, 0.023828283, 0.025157789, 0.43125907, 0.02987452] as float[]
            Integer| 'relu(i0*i0)'      || [988699588, -17870520, 141304729, 1260971300, 210951204, 1018550276] as int[]

            Double | 'relu(i0-i0)'      || [0.0, 0.0, 0.0, 0.0, 0.0, 0.0] as double[]
            Float  | 'relu(i0-i0)'      || [0.0, 0.0, 0.0, 0.0, 0.0, 0.0] as float[]
            Integer| 'relu(i0-i0)'      || [0, 0, 0, 0, 0, 0] as int[]

            Double | 'relu(i0)-i0'      || [0.26749509343988104, 0.8246464986979971, 0.1528204772472643, 0.0, 0.0, 0.17111404640776287] as double[]
            Float  | 'relu(i0)-i0'      || [0.2674951, 0.82464653, 0.15282048, 0.0, 0.0, 0.17111404] as float[]
            Integer| 'relu(i0)-i0'      || [708482220, 1831075919, 0, 1349788550, 0, 1501827147] as int[]

            Double | 'relu(-i0)+i0'     || [0.0, 0.0, 0.0, 0.15702595755893223, 0.6501361672998, 0.0] as double[]
            Float  | 'relu(-i0)+i0'     || [0.0, 0.0, 0.0, 0.15702595174312592, 0.650136142373085, 0.0] as float[]
            Integer| 'relu(-i0)+i0'     || [0, 0, 245699240, 0, 910112423, 0] as int[]

            Double | 'relu(-i0)/i0'     || [-1.0, -1.0, -1.0, -0.01, -0.01, -1.0] as double[]
            Float  | 'relu(-i0)/i0'     || [-1.0, -1.0, -1.0, -0.01, -0.01, -1.0] as float[]
            Integer| 'relu(-i0)/i0'     || [-1, -1, 0, -1, 0, -1] as int[]

            Double | 'relu(-i0-5)+i0*3' || [-0.857889221601257, -2.540599021320214, -0.5115487141104245, 0.42425011424364373, 1.9135425658852545, -0.5667989886456676] as double[]
            Float  | 'relu(-i0-5)+i0*3' || [-0.85788924, -2.540599, -0.51154876, 0.4242501, 1.9135424, -0.566799] as float[]
            Integer| 'relu(-i0-5)+i0*3' || [-1431277217, 595824021, 742061342, 1568121735, -1546243917, 1260973055] as int[]

            Double | 'abs(i0*10)%3'     || [2.7019706408068793, 2.3297626131110825, 1.5436411843158009, 1.5861207834235578, 0.5670319929272729, 1.7284247111895241] as double[]
            Float  | 'abs(i0*10)%3'     || [2.7019706, 2.3297625, 1.5436412, 1.5861207, 0.56703186, 1.7284248] as float[]
            Integer| 'abs(i0*10)%3'     || [2, 0, 1, 1, 2, 1] as int[]

            Double | 'gaus(i0)*100%i0'  || [0.011693048642643422, 0.8192993419907051, 0.08721424132459399, 0.1277867634692304, 0.6121424058303924, 0.09210498892686086] as double[]
            Float  | 'gaus(i0)*100%i0'  || [0.011690378, 0.81929654, 0.087213635, 0.12778962, 0.6121441, 0.09210494] as float[]
            Integer| 'gaus(i0)*100%i0'  || [0, 0, 0, 0, 0, 0] as int[]

            Double | 'random(i0)'       || ['r2':[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.242396559265774, 0.23980663860290638, 0.4667980401594514, 0.0, 0.0, -1.0840395336123059, 0.43090823203242123, 1.0381081218392283, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0] as double[], 'r1':[2.242396559265774, 0.23980663860290638, 0.4667980401594514, -1.0840395336123059, 0.43090823203242123, 1.0381081218392283] as double[]]
            Float  | 'random(i0)'       || ['r2':[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.2423966, 0.23980664, 0.46679804, 0.0, 0.0, -1.0840396, 0.43090823, 1.0381081, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0] as float[], 'r1':[2.2423966, 0.23980664, 0.46679804, -1.0840396, 0.43090823, 1.0381081] as float[]]
    }

}
