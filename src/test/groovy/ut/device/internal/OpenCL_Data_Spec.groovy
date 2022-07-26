package ut.device.internal

import neureka.Neureka
import neureka.devices.opencl.CLContext
import neureka.devices.opencl.Data
import spock.lang.Specification

class OpenCL_Data_Spec extends Specification
{
    def setup() {
        Neureka.get().backend().get(CLContext).getSettings().autoConvertToFloat = false
    }

    def cleanup() {
        Neureka.get().backend().get(CLContext).getSettings().autoConvertToFloat = true
    }


    def 'The OpenCLDevice specific Data class represents JVM data for OpenCL.'(
            Object data, int start, int size, Class<?> expectedType, List<?> expected
    ) {
        given : 'We make sure that any data will not automatically be converted to floats!'
            if ( Neureka.get().backend.has(CLContext) )
                Neureka.get().backend.get(CLContext).settings.autoConvertToFloat = false
        and : 'We create 2 different data objects, a full and a partial/sliced array.'
            var full = Data.of(data)
            var slice = Data.of(data, size, start)
        and : 'An expected array based on the previous slice indices!'
            var expected2 = expected[start..(start+size-1)]

        expect: 'Both data objects report the expected array types!'
            full.array.class == expectedType
            slice.array.class == expectedType
        and : 'Also they report the expected data array size.'
            full.length == expected.size()
            slice.length == expected2.size()
        and :
            full.array !== slice.array
            full.array == expected
            (data instanceof Number && slice.array == [data] ) || slice.array == expected2
        and : 'They produce OpenCL specific pointer objects.'
            full.pointer != null
            slice.pointer != null

        cleanup :
            if ( Neureka.get().backend.has(CLContext) )
                Neureka.get().backend.get(CLContext).settings.autoConvertToFloat = true

        where :
            data                   | start | size || expectedType | expected
            [2, 3, 6] as float[]   | 1     | 2    || float[]      | [2, 3, 6]
            [8,-2,5,-1] as float[] | 1     | 3    || float[]      | [8,-2,5,-1]
            4 as Float             | 0     | 1    || float[]      | [4]
            [2, 3, 6] as double[]  | 1     | 2    || double[]     | [2, 3, 6]
            [8,-2,5,-1] as double[]| 1     | 3    || double[]     | [8,-2,5,-1]
            4 as Double            | 0     | 1    || double[]     | [4]
            [2, 3, 6] as int[]     | 1     | 2    || int[]        | [2, 3, 6]
            [8,-2,5,-1] as int[]   | 1     | 3    || int[]        | [8,-2,5,-1]
            4 as Integer           | 0     | 1    || int[]        | [4]
            [2, 3, 6] as short[]   | 1     | 2    || short[]      | [2, 3, 6]
            [8,-2,5,-1] as short[] | 1     | 3    || short[]      | [8,-2,5,-1]
            4 as Short             | 0     | 1    || short[]      | [4]
            [2, 3, 6] as long[]    | 1     | 2    || long[]       | [2, 3, 6]
            [8,-2,5,-1] as long[]  | 1     | 3    || long[]       | [8,-2,5,-1]
            4 as Long              | 0     | 1    || long[]       | [4]
            [2, 3, 6] as byte[]    | 1     | 2    || byte[]       | [2, 3, 6]
            [8,-2,5,-1] as byte[]  | 1     | 3    || byte[]       | [8,-2,5,-1]
            4 as Byte              | 0     | 1    || byte[]       | [4]
    }


    def 'The "Data" class can represent various OpenCL data types.'(
      Object array, Class<Object> arrayType, Class<?> type,
      int itemSize, int size, int offset
    ) {
        given :
            array = array.asType(arrayType)
            var data1 = Data.of(array, size, offset)
            var data2 = Data.of(type, size)
            var data3 = Data.of(array)

        expect :
            data1.array == array[offset..(offset+size-1)].asType(arrayType)
            data2.array == new int[size].asType(arrayType)
            data3.array == array
        and :
            data1.length == size
            data2.length == size
            data3.length == array.length
        and :
            data1.itemSize == itemSize
            data2.itemSize == itemSize
            data3.itemSize == itemSize
        and :
            data1.pointer != null
            data2.pointer != null
            data3.pointer != null

        where :
            array        | arrayType |  type    | itemSize | size | offset
            [1, 2]       | int[]     | Integer  |    4     | 1    | 1
            [8, 5, 2]    | int[]     | Integer  |    4     | 2    | 0
            [42]         | int[]     | Integer  |    4     | 1    | 0
            [1,2,3,4,5]  | int[]     | Integer  |    4     | 3    | 2

            [1, 2]       | short[]   | Short    |    2     | 1    | 1
            [8, 5, 2]    | short[]   | Short    |    2     | 2    | 0
            [42]         | short[]   | Short    |    2     | 1    | 0
            [1,2,3,4,5]  | short[]   | Short    |    2     | 3    | 2

            [0.3, -0.9]  | float[]   | Float    |    4     | 2    | 0
            [2, 0.5, 8]  | float[]   | Float    |    4     | 2    | 0

            [0.3, -0.9]  | double[]  | Double   |    8     | 2    | 0
            [2, 0.5, 8]  | double[]  | Double   |    8     | 2    | 0
            [0.6, 3, 0.2]| double[]  | Double   |    8     | 1    | 1

    }



}
