package ut.dtype

import neureka.dtype.custom.F32
import neureka.dtype.custom.F64
import neureka.dtype.custom.I16
import neureka.dtype.custom.I32
import neureka.dtype.custom.I64
import neureka.dtype.custom.I8
import neureka.dtype.NumericType
import neureka.dtype.custom.UI16
import neureka.dtype.custom.UI32
import neureka.dtype.custom.UI64
import neureka.dtype.custom.UI8;
import spock.lang.Specification;

class NumericType_Unit_Tests extends Specification
{

    def setupSpec()
    {
        reportHeader """
            This specification covers implementations
            of the "NumericType" interface.
            Such classes are responsible for
            representing all numeric types including the ones
            which are foreign to the JVM, namely : 
            unsigned integer types.
        """
    }

    def 'NumericType implementations return their expected properties.'(
            NumericType type, int bytes, Class<?> target, Class<?> array, boolean signed
    ){

        expect : 'The type instance describes the expected number of bytes.'
            type.numberOfBytes() == bytes
        and : 'It describes the expected JVM target type.'
            type.targetType() == target
        and : 'It also describes the expected array type of said JVM target type.'
            type.targetArrayType() == array
        and : 'The instance knows if it is signed or not.'
            type.signed() == signed


        where : 'The following data is being used: '
            type        || bytes | target           | array                 | signed
            new I8()    || 1     | Byte.class       | byte[].class          | true
            new UI8()   || 1     | Short.class      | short[].class         | false
            new I16()   || 2     | Short.class      | short[].class         | true
            new UI16()  || 2     | Integer.class    | int[].class           | false
            new I32()   || 4     | Integer.class    | int[].class           | true
            new UI32()  || 4     | Long.class       | long[].class          | false
            new I64()   || 8     | Long.class       | long[].class          | true
            new UI64()  || 8     | BigInteger.class | BigInteger[].class    | false
            new F32()   || 4     | Float.class      | float[].class         | true
            new F64()   || 8     | Double.class     | double[].class        | true
    }


    def 'NumericType implementations behave as expected.'(
            NumericType type, List<Byte> data, Number converted
    ){
        given :
            def result = type.foreignHolderBytesToTarget( data as byte[] )

        expect : 'The array of bytes  is being converted to a fitting JVM type.'
            result == converted
        and : 'The original byte array can be recreated by converting with the inverse...'
            type.targetToForeignHolderBytes(result) == ( data as byte[] )

        where : 'The following NumericType instances and bytes are being used :'
            type      | data                         || converted
            new I8()  | [-23]                        || -23
            new UI8() | [-23]                        || 233

            new I16() | [2, 3]                       || new BigInteger(new byte[]{2, 3}).shortValueExact()
            new I16() | [-16, -53]                   || ((short)-3893)
            new I16() | [16, -53]                    || 4299
            new I16() | [-1, -1]                     || -1

            new UI16()| [2, 3]                       || new BigInteger(new byte[]{2, 3}).shortValueExact()
            new UI16()| [-16, -53]                   || (int)(0x10000 + ((short)-3893)) //:= https://stackoverflow.com/questions/7932701/read-byte-as-unsigned-short-java
            new UI16()| [16, -53]                    || 4_299
            new UI16()| [-1, -1]                     || 65_535

            new I32() | [22,-2, 3,-4]                || 385_745_916
            new I32() | [-22,-2, -3,-4]              || -352_387_588

            new UI32()| [22,-2, 3,-4]                || 385_745_916
            new UI32()| [-22,-2, -3,-4]              || 3_942_579_708//721_354_236 ?

            new I64() | [99, 2, 1, 35, 2, 5, 37, 22] || 7_134_266_009_577_661_718
            new I64() | [-99, 2, 1, -35, 2,5,-37,22] || - 7_133_136_811_068_105_962

            new UI64()| [99, 2, 1, 35, 2, 5, 37, 22] || 7_134_266_009_577_661_718
            new UI64()| [-99, 2, 1, -35, 2,5,-37,22] || 11_313_607_262_641_445_654
    }



    def 'Conversion goes both ways and produces expected numeric values.'(
            NumericType num, Number original, byte[] rawOriginal, Number target
    ) {
        when : 'We apply a filter in order to guarantee that the right data type is being used.'
            original = [
                    'UI8' : { o -> o as Byte },
                    'UI16': { o -> o as Short },
                    'UI32': { o -> o as Integer },
                    'UI64': { o -> o as Long },
                    'I8' : { o -> o as Byte },
                    'I16': { o -> o as Short },
                    'I32': { o -> o as Integer },
                    'I64': { o -> o as Long },
                    'F32': { o -> o as Float },
                    'F64': { o -> o as Double }
            ][ num.class.simpleName ](original)
        and : 'The convert the raw type (might represent unsigned value) to a JVM compatible target type...'
            def resultTarget = num.foreignHolderBytesToTarget( rawOriginal )
        and : 'Then convert this result to the true byte array of the value...'
            def backToRaw = num.targetToForeignHolderBytes( resultTarget )

        then : 'This produces the expected values which express the following relationships:'
            resultTarget == target
            backToRaw == rawOriginal
            num.toTarget( original ) == target

        where : 'The following "NumericType" implementation instances and numeric data is being used: '
            num       |    original   |     rawOriginal                        || target
            new UI8() |      -3       |       [-3]                             || 255 - 2
            new UI16()|      -3       |      [255, 253]                        || 65_535 - 2
            new UI32()|      -3       |[255, 255, 255, 253]                    || 4_294_967_295 - 2
            new UI64()|      -3       |[255, 255, 255, 255, 255, 255, 255, 253]|| 18_446_744_073_709_551_615 - 2
            new I8()  |      -3       |       [-3]                             || - 3
            new I16() |      -3       |      [255, 253]                        || - 3
            new I32() |      -3       |[255, 255, 255, 253]                    || - 3
            new I64() |      -3       |[255, 255, 255, 255, 255, 255, 255, 253]|| - 3
            new F32() |     -0.3      | [-66, -103, -103, -102]                || - 0.3 as Float
            new F64() |     -0.3      | [-65, -45, 51, 51, 51, 51, 51, 51]     || - 0.3 as Double
            new F32() |  -5432.39928  | [-59, -87, -61, 50]                    || -5432.39928 as Float
            new F64() |  -5432.39928  | [-64, -75, 56, 102, 55, 54, -51, -14]  || -5432.39928 as Double
        /*
            Verify F32 & F64 byte arrays with the following :
            ------------------------------------------------
            print new I64().targetToForeignBytes( Double.doubleToLongBits( -8495432.3992898 ) )
            print new I32().targetToForeignBytes( Float.floatToIntBits( -8495432.3992898 ) )
        */
    }


}
