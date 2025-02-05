package ut.device

import neureka.Neureka
import neureka.Tsr
import neureka.backend.api.Algorithm
import neureka.backend.api.ExecutionCall
import neureka.backend.api.template.algorithms.AbstractDeviceAlgorithm
import neureka.common.utility.DataConverter
import neureka.devices.Device
import neureka.devices.file.FileDevice
import neureka.devices.host.CPU
import neureka.devices.opencl.OpenCLDevice
import neureka.view.NDPrintSettings
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import testutility.mock.DummyDevice

import java.util.function.BiConsumer

@Title("Finding Device Types")
@Narrative('''

    Neureka introduces a the concept of a `Device` which is an interface
    that represents a computational device used for executing tensor / nd-array operations on them.
    The `Device` interface is implemented by various classes which represent
    different types of accelerator hardware such as `CPUs`, `GPUs`, `TPUs`, `FPGAs`, etc.
    These various `Device` types can not be instantiated directly because they model 
    the concrete and finite hardware that is available on any given system Neureka is running on.
    This means that they are usually instantiated lazily upon access request or 
    upfront by the library backend (usually a backend extension built fo a specific device).
    In order to find these instances embedded in the library backend the `Device` interface
    exposes various static methods which can be used to find a device instance by name or type.

''')
@Subject([Device])
class Cross_Device_Type_Spec extends Specification
{
    def setupSpec()
    {
        reportHeader """
                Specified below is the behaviour of the factory methods in the
                Device interface as well as its various implementations 
                which should adhere to a certain set of common behaviours.
        """
    }

    def setup() {
        Neureka.get().reset()
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

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.type == OpenCLDevice }) // We need to assure that this system supports OpenCL!
    def 'We can find Device implementations or null by passing search keys to the "get" method.'(
            String query, Class type
    ) {
        reportInfo """
            Note that the examples specified in the table below
            may not be the same on every system because the devices
            returned by the "get" method are dependent on the current 
            system and the available hardware.
        """

        when : 'We pass a query key word to the "get" method...'
            var device = Device.get(query)

        then : '...the result should be a non-null device (if our query key matches something).'
            device != null
        and : 'The resulting Device variable has the expected type (CPU, OpenCLDevice, ...).'
            device.class == type

        where : 'The query key words and the expected device types returned.'
            query                          || type
            "cPu"                          || CPU
            "jVm"                          || CPU
            "natiVe"                       || CPU
            "Threaded"                     || CPU
            "first CPU"                    || CPU
            "openCl"                       || OpenCLDevice
            "first gpu"                    || OpenCLDevice
            "nvidia or amd or intel"       || OpenCLDevice // This assumes that there is an amd/intel/nvidia gpu!
            "rocm or cuda or amd or nvidia"|| OpenCLDevice // This assumes that there is an amd/intel/nvidia gpu!
            "nvidia || amd or intel"       || OpenCLDevice // This assumes that there is an amd/intel/nvidia gpu!
            "cuda||rocm || gpu"            || OpenCLDevice // This assumes that there is an amd/intel/nvidia gpu!
            "first"                        || OpenCLDevice
    }

    def 'We can query the backend for devices by specifying both the requested type and a key word.'(
            Class<?> type, String key, Device<?> expected
    ) {
        reportInfo  """
            Note that the examples specified in the table below
            may not be the same on every system because the devices
            returned by the "get" and "find" methods are dependent on the current 
            system and the available hardware that Neureka encounters.
        """
        expect : 'Querying for a device using a device type and key works as expected.'
            Device.get(type, key) === expected
        and : 'We can use the "find" method if we want the result to be wrapped in a nice and safe Optional instance.'
            Device.find(type, key).isEmpty() && expected == null || Device.find(type, key).get() === expected

        where : 'The we can use the following device type, key and expected device instance.'
            type        | key          || expected
            Device      | 'cpu'        || CPU.get()
            Device      | 'jvm'        || CPU.get()
            Device      | 'processor'  || CPU.get()
            DummyDevice | 'first'      || null
            DummyDevice | 'any'        || null
            DummyDevice | 'cpu'        || null
            DummyDevice | 'gpu'        || null
    }

    def 'In total there are 3 different types of methods for finding device instances.'(
            String key, Device<?> expected
    ) {
        expect : 'The "get" method returns a device instance or null.'
            Device.get(key) === expected
        and : 'The "any" method returns a device instance or the "CPU device", which is the library default device.'
            Device.any(key) === expected || Device.any(key) === CPU.get()
        and : 'The "find" method returns a device instance wrapped in an Optional instance.'
            Device.find(key).isEmpty() && expected == null || Device.find(key).get() === expected

        where : 'The we can use the following search key and expected device instance.'
            key                 || expected
            'cpu'               || CPU.get()
            'central processing'|| CPU.get()
            'e9rt56hqfwe5f0'    || null
            '5638135dh90978'    || null
            'banana device'     || null
            'cupcake'           || null
    }

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.device == null })
    def 'Passing a numeric array to a tensor should modify its contents!'(
            Device device, Object data1, Object data2, String expected
    ) {
        reportInfo """
            Here we demonstrate that a tensor located on a non-CPU `Device` will be
            updated when passing a float or double array, even if it its data is stored
            on the GPU or somewhere else!
        """

        given : 'A 2D tensor which we transfer to a device using the "to" method...'
            Tsr t = Tsr.of(new int[]{3, 2}, new double[]{2, 4, -5, 8, 3, -2}).to(device)

        when : 'A numeric array is passed to said tensor...'
            t.mut.setItems(data1)
            t.mut.setItems(data2)

        then : 'The tensor (as String) contains the expected String.'
            t.toString().contains(expected)

        where : 'The following data is being used :'
            device                | data1                       | data2                      || expected
            Device.get("cpu")     | new float[0]                | new float[0]               || "(3x2):[2.0, 4.0, -5.0, 8.0, 3.0, -2.0]"
            Device.get("cpu")     | new float[]{2, 3, 4, 5, 6}  | new float[]{1, 1, 1, 1}    || "(3x2):[1.0, 1.0, 1.0, 1.0, 6.0, -2.0]"
            Device.get("cpu")     | new float[]{3, 5, 6}        | new float[]{4, 2, 3}       || "(3x2):[4.0, 2.0, 3.0, 8.0, 3.0, -2.0]"
            Device.get("cpu")     | new double[]{9, 4, 7, -12}  | new double[]{-5, -2, 1}    || "(3x2):[-5.0, -2.0, 1.0, -12.0, 3.0, -2.0]"
            Device.get("cpu")     | new float[]{22, 24, 35, 80} | new double[]{-1, -1, -1}   || "(3x2):[-1.0, -1.0, -1.0, 80.0, 3.0, -2.0]"

            Device.get("openCL")  | new float[0]                | new float[0]               || "(3x2):[2.0, 4.0, -5.0, 8.0, 3.0, -2.0]"
            Device.get("openCL")  | new float[]{2, 3, 4, 5, 6}  | new float[]{1, 1, 1, 1, 1} || "(3x2):[1.0, 1.0, 1.0, 1.0, 1.0, -2.0]"
            Device.get("openCL")  | new float[]{3, 5, 6}        | new float[]{4, 2, 3}       || "(3x2):[4.0, 2.0, 3.0, 8.0, 3.0, -2.0]"
            Device.get("openCL")  | new double[]{9, 4, 7, -12}  | new double[]{-5, -2, 1}    || "(3x2):[-5.0, -2.0, 1.0, -12.0, 3.0, -2.0]"
            Device.get("openCL")  | new float[]{22, 24, 35, 80} | new double[]{-1, -1, -1}   || "(3x2):[-1.0, -1.0, -1.0, 80.0, 3.0, -2.0]"
    }

    def 'Devices expose an API for accessing (reading and writing) the data of a tensor.'(
            Device device, int[] shape, Object data, List<Float> expected
    ) {
        given : 'Because in some environments OpenCL might not be available, the test will be stopped!'
            if ( device == null ) return

        when : 'A 2D tensor is being instantiated by passing the given shape and data...'
            Tsr t = Tsr.of(shape, data).to(device)

        then : 'The tensor values (as List) are as expected.'
            Arrays.equals(t.getItemsAs(double[].class), DataConverter.get().convert(expected,double[].class))

        when : 'The same underlying data is being queried by calling the device...'
            var result = (0..<t.size()).collect{device.access(t).readAt(it)}

        then : 'This other result also contains the same elements.'
            result == expected

        when :
            result = (0..<t.size()).collect{device.access(t).readArray(data.getClass(), it, 1)[0]}
        then : 'This other result also contains the same elements.'
            result == expected


        where : 'The following data is being used for tensor instantiation :'
            device                | shape           | data                                               || expected
            Device.get("cpu")     | new int[]{3, 2} | new double[]{-5.0, -2.0, 1.0, -12.0, 3.0, -2.0}    || [-5.0, -2.0, 1.0, -12.0, 3.0, -2.0]
            Device.get("cpu")     | new int[]{3, 2} | new int[]{-5, -2, 1, -12, 3, -2}                   || [-5, -2, 1, -12, 3, -2]
            Device.get("cpu")     | new int[]{3, 2} | new long[]{-5, -2, 1, -12, 3, -2}                  || [-5, -2, 1, -12, 3, -2]
            Device.get("cpu")     | new int[]{3, 2} | new byte[]{-5, -2, 1, -12, 3, -2}                  || [-5, -2, 1, -12, 3, -2]
            Device.get("cpu")     | new int[]{3, 2} | new short[]{-5, -2, 1, -12, 3, -2}                 || [-5, -2, 1, -12, 3, -2]
            Device.get("cpu")     | new int[]{3, 2} | new float[]{-5, -2, 1, -12, 3, -2}                 || [-5, -2, 1, -12, 3, -2]
            Device.get("cpu")     | new int[]{3, 2} | new boolean[]{true, false, false}                  || [true, false, false, true, false, false]
            Device.get("openCL")  | new int[]{3, 2} | new double[]{-5.0, -2.0, 1.0, -12.0, 3.0, -2.0}    || [-5.0, -2.0, 1.0, -12.0, 3.0, -2.0]
            Device.get("openCL")  | new int[]{3, 2} | new float[]{-1.0, -1.0, -1.0, 80.0, 3.0, -2.0}     || [-1.0, -1.0, -1.0, 80.0, 3.0, -2.0]
    }

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.device instanceof OpenCLDevice || data.device == null })
    def 'Execution calls containing null arguments will cause an exception to be thrown in device instances.'(
        Device device
    ) {
        reportInfo """
            Every argument within an ExecutionCall instance has a purpose. Null is not permissible.
        """

        given : 'A mocked ExecutionCall with mocked algorithm...'
            var call = Mock(ExecutionCall)
            var implementation = Mock(Algorithm)
        and : 'We construct a plausible mocked call by making it expose the given device.'
            call.getDevice() >> device

        when : 'The call is being passed to the execution utility method ..'
            AbstractDeviceAlgorithm.prepareAndExecuteRecursively( call, AbstractDeviceAlgorithm::executeDeviceAlgorithm )

        then : '...the implementation is being accessed in order to access the mocked lambda...'
            (1.._) * call.getAlgorithm() >> implementation
        and : 'The tensor array is being accessed to check for null. (For exception throwing)'
            1 * call.inputs() >> new Tsr[]{ Mock(Tsr), null }
        and : 'The expected exception is being thrown alongside a descriptive message.'
            def exception = thrown(IllegalArgumentException)
            exception.message == "Device arguments may not be null!\n" +
                    "One or more tensor arguments within the given ExecutionCall instance is null."

        where : 'The following Device instances are being tested :'
            device << [
                    CPU.get(),
                    Device.get("openCL")
                ]

    }

    def 'Devices store tensors which can also be restored.'(
            Device device
    ) {
        reportInfo """
            Device implementations also behave like storage units for tensors,
            which you can see below.
        """

        given : 'The given device is available and Neureka is being reset.'
            if ( device == null ) return
        and : 'Two tensors which will be transferred later on...'
            int initialNumber = device.size()
            Tsr a = Tsr.of([2, 3], ";)")
            Tsr b = Tsr.of([3, 4], ":P")

        expect : 'The given device is initially empty.'
            device.isEmpty() == ( device.size() == 0 )
            !device.has( a ) || device instanceof CPU
            !device.has( b ) || device instanceof CPU

        when : 'The the first tensor is being passed to the device...'
            device.store( a )

        then : '...tensor "a" is now on the device.'
            !device.isEmpty()
            device.size() == initialNumber + 1
            device.has( a )
            !device.has( b ) || device instanceof CPU

        when : 'The the second tensor is being passed to the device...'
            device.store( b )

        then : '...tensor "b" is now also on the device.'
            !device.isEmpty()
            device.size() == initialNumber + 2
            device.has( a )
            device.has( b )

        when : 'They are being removed again...'
            device.free( a ).free( b )

        then : '...the device is empty again.'
            device.isEmpty() == ( initialNumber == 0 )
            device.size() == initialNumber
            !device.has( a ) || device instanceof CPU
            !device.has( b ) || device instanceof CPU

        where : 'The following Device instances are being tested :'
            device << [
                    CPU.get(),
                    Device.get( "openCL" ),
                    FileDevice.at( "build/test-can" )
                ]

    }

    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.device == null })
    def 'Devices also store slices which can also be restored just like normal tensors.'(
            Device device
    ) {
        given : 'The given device is available and Neureka is being reset.'
            if ( device == null ) return
        and : 'Two tensors which will be transferred later on...'
            int initialNumber = device.size()
            Tsr a = Tsr.of([2, 3], ";)")
            Tsr b = a[1, 0..2]

        expect : 'The given device is initially empty.'
            device.isEmpty() == ( device.size() == 0 )
            !device.has( a ) || device instanceof CPU
            !device.has( b ) || device instanceof CPU

        when : 'The the first tensor is being passed to the device...'
            device.store( a )

        then : '...tensor "a" is now on the device.'
            !device.isEmpty()
            device.size() == initialNumber + 1
            device.has( a )
        and :
            !device.has( b ) || device instanceof CPU

        when :
            device.free( a )

        then : '...the device is empty again.'
            device.isEmpty() == ( initialNumber == 0 )
            device.size() == initialNumber
            !device.has( a ) || device instanceof CPU
            !device.has( b ) || device instanceof CPU

        where : 'The following Device instances are being tested :'
        device << [
                CPU.get(),
                Device.get( "openCL" )
            ]
    }

    @Ignore
    def 'Devices cannot store slices whose parents are not already stored.'(
            Device device, BiConsumer<Device, Tsr> storageMethod
    ) {
        given : 'The given device is available and Neureka is being reset.'
            if ( device == null ) return
        and : 'Two tensors which will be transferred later on...'
            Tsr a = Tsr.of([2, 3], ";)")
            Tsr b = a[1, 0..2]
        and :
            var initialSize = device.size()

        expect : 'The given device is initially empty.'
            device.isEmpty() == ( device.size() == 0 )
            !device.has( a )
            !device.has( b )

        when : 'The the first tensor is being passed to the device...'
            device.store( b )

        then : '...tensor "a" is now on the device.'
            var exception = thrown(IllegalStateException)
            exception.message.contains("Data parent is not outsourced!")

        expect : 'The given device is initially empty.'
            device.isEmpty() == ( device.size() == 0 )
            !device.has( a )
            !device.has( b )

        when :
            storageMethod(device, a)

        then :
            !device.isEmpty()
            a.isOutsourced()
            b.isOutsourced()
        and :
            device.has( a )
            b.mut.data.ref == null
            device.has( b )
            device.size() == initialSize

        where : 'The following Device instances are being tested :'
            device                                  | storageMethod
            Device.get( "openCL" ) | { d, t -> d.store(t) }
            Device.get( "openCL" ) | { d, t -> t.to(d)   }
            //FileDevice.at( "build/test-can" )     | { d, t -> d.store(t) }
            //CPU.get()                    | { d, t -> t.set(d)   }
            //FileDevice.at( "build/test-can" )     | { d, t -> t.set(d)   }
            //CPU.get()                    | { d, t -> d.store(t) }
    }


    @IgnoreIf({ !Neureka.get().canAccessOpenCLDevice() && data.deviceType == 'GPU' })
    def 'Virtual tensors stay virtual when outsourced.'(
            String deviceType
    ) {
        reportInfo """
            Note: A virtual tensor is a tensor which is not yet fully initialized
            in the sense that the data array is not yet allocated according to the 
            tensors size (number of elements they hold).
            This is the case for tensor which are filled homogeneously with a single value,
            like for example an all 0 tensor.    
        """

        given : 'We create a homogeneously filled tensor, which is therefor "virtual".'
            var t = Tsr.ofFloats().withShape(4,3).all(-0.54f)
        and : 'We also get a device for testing...'
            var device = Device.get(deviceType)

        expect : 'We expect that the tensor is virtual, meaning its underlying data array stores only a single value...'
            t.isVirtual()

        when : 'We send the tensor to the device...'
            t.to(device)
        then : 'This should cause it to be "outsourced", (except dor a CPU device of course).'
            t.isOutsourced() != ( device instanceof CPU )
        and : '...we expect the tensor to stay virtual on the device!'
            t.isVirtual()

        when : 'We restore the device...'
            device.restore(t)
        then : 'The tensor should no longer be outsourced.'
            !t.isOutsourced()
        and : 'It should still be virtual!'
            t.isVirtual()

        where : 'We test on the following devices:'
            deviceType << ['CPU','GPU']
    }

}
