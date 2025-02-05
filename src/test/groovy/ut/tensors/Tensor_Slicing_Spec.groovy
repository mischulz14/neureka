package ut.tensors

import neureka.Neureka
import neureka.Tsr
import neureka.calculus.Function
import neureka.calculus.args.Arg
import neureka.devices.Device
import neureka.devices.host.CPU
import neureka.devices.opencl.OpenCLDevice
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Tensors within Tensors")
@Narrative('''

    ND-Array data structures can be "sliced" in the sense
    that one can create a subset view of the underlying data inside a tensor
    through a new tensor instance...
    This can be a tedious and complicated procedure.
    Therefore a tensor should expose a various user friendly API for slicing which
    are also fit for various languages.
    This specification covers these APIs for tensor slicing.
                    
''')
@Subject([Tsr])
class Tensor_Slicing_Spec extends Specification
{
    def setupSpec() {
        reportHeader """
                This specification covers the behavior of tensors when being sliced
                on multiple different device types using the slice builder API.           
            """
    }

    def setup() {
        Neureka.get().reset()
        Neureka.get().settings().view().getNDPrintSettings().setIsLegacy(false)
    }

    def 'When Slicing only one axis using the SliceBuilder API, the other axes will be sliced implicitly.'(
            Device device
    ) {
        given : 'A device could be found.'
            if ( device == null ) return

        and: 'The found device is also supported (Which might not always be the case for the OpenCLDevice).'
            if ( device instanceof OpenCLDevice && !Neureka.get().canAccessOpenCLDevice() ) return

        and : 'A 3 dimensional tensor which will be sliced.'
            Tsr<Double> t = Tsr.of([2, 4, 3], -3d..7d)

        and : 'Which will be placed on a given device:'
            t.to(device)

        when : 'Slicing axis 1 of the tensor using the "from" & "to" methods...'
            Tsr s = t.slice()
                        .axis(1).from(1).to(2)
                        .get() // Note: Axis 0 and 2 will be sliced implicitly if not specified!

        then : 'This will result in a slice which has 4 axis entries less than the original tensor.'
            s.shape().sum() == t.shape().sum() - 2

        and : 'This new slice will be displayed as follows when printed (with adjusted indent):'
            s.toString().replace('\n', '\n'+" "*20) ==
                """(2x2x3):[
                       [
                          [   0.0 ,   1.0 ,   2.0  ],
                          [   3.0 ,   4.0 ,   5.0  ]
                       ],
                       [
                          [   1.0 ,   2.0 ,   3.0  ],
                          [   4.0 ,   5.0 ,   6.0  ]
                       ]
                    ]"""

        and : 'As already shown by the printed view, the tensor as the expected shape:'
            s.shape() == [2, 2, 3]

        where: 'This works both on the GPU as well as CPU of course.'
            device << [Device.get('gpu'), CPU.get() ]
    }

    def 'The "at" method and the "from" / "to" methods can be mixed when slicing a tensor.' (
            Device device
    ) {
        given : 'A device could be found.'
            if ( device == null ) return

        and: 'The found device is also supported (Which might not always be the case for the OpenCLDevice).'
            if ( device instanceof OpenCLDevice && !Neureka.get().canAccessOpenCLDevice() ) return

        and : 'A 3 dimensional tensor which will be sliced.'
            Tsr<Double> t = Tsr.of([3, 3, 4], -11d..3d)

        and : 'Which will be placed on a given device:'
            t.to(device)

        when : 'Slicing the tensor using both "at", "from"/"to" and an implicit full ranged slice for axis 1...'
            Tsr s = t.slice()
                        .axis(0).at(1)
                        // Note: Axis 1 will be sliced implicitly if not specified!
                        .axis(2).from(1).to(2)
                        .get()

        then : 'This will result in a slice which has 4 axis entries less than the original tensor.'
            s.shape().sum() == t.shape().sum() - 4

        and : 'This new slice will be displayed as follows when printed (with adjusted indent):'
            s.toString().replace('\n', '\n'+" "*20) ==
                """(1x3x2):[
                       [
                          [   2.0 ,   3.0  ],
                          [  -9.0 ,  -8.0  ],
                          [  -5.0 ,  -4.0  ]
                       ]
                    ]"""

        and : 'The "at" method sliced a single axis point whereas the "from" & "to" sliced from 1 to 2.'
            s.shape() == [1, 3, 2]

        where: 'This works both on the GPU as well as CPU of course.'
            device << [Device.get('gpu'), CPU.get() ]
    }

    def 'A tensor can be sliced by passing ranges in the form of primitive arrays.' (
            Device device
    ) {
        given : 'A device could be found.'
            if ( device == null ) return

        and: 'The found device is also supported (Which might not always be the case for the OpenCLDevice).'
            if ( device instanceof OpenCLDevice && !Neureka.get().canAccessOpenCLDevice() ) return

        and : 'A 3 dimensional tensor which will be sliced.'
            Tsr t = Tsr.of([3, 3, 4], -11..3)

        and : 'Which will be placed on a given device:'
            t.to(device)

        when : 'Slicing the tensor using primitive int arrays...'
            var s = t.getAt(
                        new int[]{1},    // Axis 0
                        new int[]{0, 2}, // Axis 1
                        new int[]{1, 2}  // Axis 2
                    )

        then : 'This will result in a slice which has 4 axis entries less than the original tensor.'
            s.shape().sum() == t.shape().sum() - 4

        and : 'This new slice will have the expected shape and items:'
            s.shape == [1, 3, 2]
            s.items == [2, 3, -9, -8, -5, -4]

        and : 'The the slice will have the following shape'
            s.shape() == [1, 3, 2]

        where: 'This works both on the GPU as well as CPU of course.'
            device << [Device.get('gpu'), CPU.get() ]

    }



    def 'A tensor can be sliced by passing ranges in the form of lists (Groovy ranges).' (
            Device device
    ) {
        given : 'A device could be found.'
            if ( device == null ) return

        and: 'The found device is also supported (Which might not always be the case for the OpenCLDevice).'
            if ( device instanceof OpenCLDevice && !Neureka.get().canAccessOpenCLDevice() ) return

        and : 'A 3 dimensional tensor which will be sliced.'
            var t = Tsr.of([3, 3, 4], -11..3)

        and : 'Which will be placed on a given device:'
            t.to(device)

        when : 'Slicing the tensor using lists of integers generated by the Groovy range operator..'
            var s = t[1, 0..2, 1..2]

        then : 'This will result in a slice which has 4 axis entries less than the original tensor.'
            s.shape().sum() == t.shape().sum() - 4

        and : 'This new slice will have the expected shape and items:'
            s.shape == [1, 3, 2]
            s.items == [2, 3, -9, -8, -5, -4]

        where: 'This works both on the GPU as well as CPU of course.'
            device << [ Device.get('gpu'), CPU.get() ]

    }


    def 'The slice builder also supports slicing with custom step sizes.' (
            Device device
    ) {
        given : 'A device could be found.'
            if ( device == null ) return

        and: 'The found device is also supported (Which might not always be the case for the OpenCLDevice).'
            if ( device instanceof OpenCLDevice && !Neureka.get().canAccessOpenCLDevice() ) return

        and : 'A 3 dimensional tensor which will be sliced.'
            Tsr<Double> t = Tsr.of([3, 3, 4], -11d..3d)

        and : 'Which will be placed on a given device:'
            t.to(device)

        when : 'Slicing the tensor using lists of integers generated by the Groovy range operator..'
            Tsr s = t.slice()
                        .axis(0).at(0)
                        .axis(1).at(0)
                        .axis(2).from(0).to(3).step(2)
                        .get()

        then : 'This will result in a slice which has 4 axis entries less than the original tensor.'
            s.shape().sum() == t.shape().sum() - 6

        and : 'This new slice will be displayed as follows when printed:'
            s.toString() == "(1x1x2):[\n" +
                            "   [\n" +
                            "      [  -11.0,  -9.0  ]\n" +
                            "   ]\n" +
                            "]"

        and : 'The the slice will have the following shape'
            s.shape() == [1, 1, 2]

        where: 'This works both on the GPU as well as CPU of course.'
            device << [Device.get('gpu'), CPU.get() ]

    }


    def 'Slicing is also a Function with autograd support!'(
    ) {
        given : 'A 2 dimensional tensor requiring gradients.'
            var t = Tsr.ofBytes().withShape(4, 4).andFill(-1, 7, 3).setRqsGradient(true)
        and : '3 arrays we need to slice (the slice shape, offset and spread/strides).'
            int[] newShape  = [2, 2]
            int[] newOffset = [1, 1]
            int[] newSpread = [1, 1]

        when : 'Slicing the tensor using the slice function.'
            Tsr<Object> slice = Function.of("slice(I[0])", true)
                                    .with(Arg.Shape.of(newShape),Arg.Offset.of(newOffset),Arg.Stride.of(newSpread))
                                    .call(t)
        then : 'The resulting tensor will have the correct shape and values.'
            slice.toString() == "(2x2):[\n" +
                                "   [    3  ,   -1   ],\n" +
                                "   [   -1  ,    7   ]\n" +
                                "]"

        when : 'We perform a backward pass using an "error" of -8.'
            slice.backward(-8)

        then : 'The gradient of the original tensor will be contain 4 times -8.'
            t.toString() == "(4x4):[\n" +
                           "   [   -1  ,    7  ,    3  ,   -1   ],\n" +
                           "   [    7  ,    3  ,   -1  ,    7   ],\n" +
                           "   [    3  ,   -1  ,    7  ,    3   ],\n" +
                           "   [   -1  ,    7  ,    3  ,   -1   ]\n" +
                           "]:g:[\n" +
                           "   [    0  ,    0  ,    0  ,    0   ],\n" +
                           "   [    0  ,   -8  ,   -8  ,    0   ],\n" +
                           "   [    0  ,   -8  ,   -8  ,    0   ],\n" +
                           "   [    0  ,    0  ,    0  ,    0   ]\n" +
                           "]"
    }


    def 'Normal slicing will try to do autograd.'()
    {
        given : 'A 1 dimensional tensor (vector) requiring gradients.'
            var t = Tsr.ofBytes().withShape(5).andFill(-1, 7, 3).setRqsGradient(true)

        when : 'Slicing the tensor using the subscription operator (which calls the getAt(List) method).'
            var s = t[2..3]
        and : 'We perform a backward pass using an "error" of 42.'
            s.backward(42)

        then : 'The gradient of the original tensor will be contain 2 times 42.'
            t.toString() == "(5):[   -1  ,    7  ,    3  ,   -1  ,    7   ]:g:[    0  ,    0  ,   42  ,   42  ,    0   ]"
    }

    def 'We can avoid autograd when slicing by using the "detached" instead of the "get" method.'()
    {
        given : 'A 1 dimensional tensor (vector) requiring gradients.'
            var t = Tsr.ofBytes().withShape(4).andFill(-1, 7, 3).setRqsGradient(true)

        when : 'We slice the tensor through the fluent slicer, then get it detached and finally backpropagation an error of 73.'
            t.slice()
                .axis(0).from(0).to(1)
                .detached()
                .backward(73)

        then : 'The gradient of the original tensor will still be null, because we performed detached slicing.'
            t.toString() == "(4):[   -1  ,    7  ,    3  ,   -1   ]:g:[null]"

        when : 'We slice the tensor through the fluent slicer but this time use the get method and then again backpropagation an error of 73.'
            t.slice()
                .axis(0).from(0).to(1)
                .get()
                .backward(73)

        then : 'This time there will be 2 times 73 in the gradient.'
            t.toString() == "(4):[   -1  ,    7  ,    3  ,   -1   ]:g:[   73  ,   73  ,    0  ,    0   ]"
    }

    def 'We can slice a scalar tensor from a larger tensor of rank 4.'()
    {
        given : 'A 4 dimensional tensor.'
            var t = Tsr.ofBytes().withShape(4, 2, 3, 2).andFill(-1, 7, 9, 5, 4, 3)

        when : 'Slicing the tensor using lists of integers...'
            var s = t[1, 1, 2, 0]

        then : 'The slice will contain only a single number, namely: 4.'
            s.items == [4]
        and : 'We verify this through the "getItemAt" method.'
            s.items == [t.item(1, 1, 2, 0)]
        and : 'A variation of the previous verification (here we test 0 padding of getItemAt).'
            s.items == [t.item(1, 1, 2)] // This is the same as getItemAt(1, 1, 2, 0)
    }


}
