package neureka.dtype.custom;

import neureka.dtype.AbstractNumericType;
import neureka.utility.DataConverter;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


public class I8 extends AbstractNumericType<Byte, byte[], Byte, byte[]>
{
    public I8(){
        super();
        _set( double[].class, DataConverter.Utility::byteToDouble);
        _set( int[].class,    DataConverter.Utility::byteToInt);
        _set( float[].class,  DataConverter.Utility::byteToFloat);
        _set( long[].class,   DataConverter.Utility::byteToLong );
    }

    @Override
    public boolean signed() {
        return true;
    }

    @Override
    public int numberOfBytes() {
        return 1;
    }

    @Override
    public Class<Byte> targetType() {
        return Byte.class;
    }

    @Override
    public Class<byte[]> targetArrayType() {
        return byte[].class;
    }

    @Override
    public Class<Byte> foreignType() {
        return Byte.class;
    }

    @Override
    public Class<byte[]> foreignArrayType() {
        return byte[].class;
    }

    @Override
    public Byte foreignBytesToTarget(byte[] bytes) {
        return bytes[ 0 ];
    }

    @Override
    public Byte toTarget(Byte original) {
        return original;
    }

    @Override
    public byte[] targetToForeignBytes(Byte number) {
        return new byte[]{number};
    }

    @Override
    public byte[] readAndConvertDataFrom(DataInput stream, int size) throws IOException {
        byte[] bytes = new byte[size];
        stream.readFully(bytes, size, size);
        return bytes;
    }

    @Override
    public byte[] readForeignDataFrom(DataInput stream, int size) throws IOException {
        byte[] bytes = new byte[size];
        stream.readFully(bytes, size, size);
        return bytes;
    }


}
