package ut.device

import neureka.Neureka
import neureka.Tsr
import neureka.devices.storage.FileDevice
import neureka.devices.storage.FileHead
import neureka.devices.storage.IDXHead
import neureka.devices.storage.JPEGHead
import spock.lang.Specification

class FileDevice_Unit_Tests extends Specification
{
    def setupSpec()
    {
        reportHeader """
                <h2> FileDevice Behavior </h2>
                <br> 
                <p>
                    This specification covers the behavior of the "FileDevice"
                    class, which enables the persistence of tensor data.           
                </p>
            """
    }

    def setup() {
        Neureka.instance().reset()
    }

    def 'A file device stores tensors in idx files by default.'(
        String path, String filename
    ) {
        given : 'A new tensor is being created for testing.'
            Tsr a = new Tsr([2, 4], [ 5, 4, -7, 3, -2, 6, -4, 3 ])
        and : 'A file device instance is being accessed for a given path.'
            def device = FileDevice.instance( path )

        expect : 'Initially the device does not store our newly created tensor.'
            !device.contains(a)

        when : 'Tensor "a" is being stored in the device...'
            device.store( a, filename )

        then : 'The expected file is being created at the given path.'
            new File( path + '/' + filename + '.idx' ).exists()

        and : 'Tensor "a" does no longer have a value (stored in RAM).'
            a.data == null

        when :
            device.free( a )

        then :
            !new File( path + '/' + filename + '.idx' ).exists()


        where :
            path             | filename
            "build/test-can" | "tensor_2x4_"
    }

    def 'A file device stores tensors in various file formats.'(
            String path, String filename, Class<FileHead<?,Number>> fileHeadClass
    ) {
        given : 'A new tensor is being created for testing.'
            Tsr a = new Tsr([2, 4, 3], -8..8)
        and : 'A file device instance is being accessed for a given path.'
            def device = FileDevice.instance( path )

        expect : 'Initially the device does not store our newly created tensor.'
            !device.contains(a)

        when : 'Tensor "a" is being stored in the device...'
            if ( filename != null ) device.store( a, filename ) else device.store( a )

        then : 'The expected file is being created at the given path.'
            new File( path + '/' + filename ).exists()
                    ||
            new File( path ).listFiles().any {
                it.name.startsWith('tensor_2x4x3_f64_') && it.name.endsWith('.idx')
            }

        and : 'Tensor "a" does no longer have a value (stored in RAM).'
            a.data == null

        and : 'The device contains a "FileHead" instances of the expected type.'
            device.fileHeadOf( a ).class == fileHeadClass

        when :
            device.free( a )

        then :
            !new File( path + '/' + filename ).exists()
            !new File( path ).listFiles().any {it.name.startsWith("tensor_2x4x3_f64_") }

        where :
            path             | filename            || fileHeadClass
            "build/test-can" | "tensor_2x4x3_.idx" || IDXHead.class
            "build/test-can" | "tensor_2x4x3_.jpg" || JPEGHead.class
            "build/test-can" | null                || IDXHead.class
    }


}
