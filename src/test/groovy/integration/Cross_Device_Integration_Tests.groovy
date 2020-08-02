package integration

import groovy.transform.CompileDynamic
import integration.tests.CrossDeviceIntegrationTest
import integration.tests.SimpleNNIntegrationTest
import neureka.Neureka
import neureka.Tsr
import neureka.acceleration.Device
import neureka.acceleration.host.HostCPU
import neureka.acceleration.opencl.OpenCLDevice
import neureka.acceleration.opencl.OpenCLPlatform
import spock.lang.Specification
import testutility.mock.DummyDevice

@CompileDynamic
class Cross_Device_Integration_Tests extends Specification
{

    def "Test cross device integration with default and legacy indexing."(
            Device device, boolean legacyIndexing
    ) {
        given :
            if (
                device instanceof OpenCLDevice && // OpenCL cannot run inside TravisCI ! :/
                !System.getProperty("os.name").toLowerCase().contains("windows")
            ) return
            Neureka.instance().reset()
            Neureka.instance().settings().debug().isKeepingDerivativeTargetPayloads = true
            Neureka.instance().settings().view().isUsingLegacyView = true

        and :
            Neureka.instance().settings().indexing().isUsingLegacyIndexing = legacyIndexing
            if ( device instanceof OpenCLDevice ) OpenCLPlatform.PLATFORMS().get(0).recompile()

        expect : CrossDeviceIntegrationTest.on(device, legacyIndexing)

        where :
            device               || legacyIndexing
            HostCPU.instance()   ||    false
            HostCPU.instance()   ||    true
            Device.find('first') ||    true
            Device.find('first') ||    false
    }



    def 'Test simple NN implementation with manual backprop'()
    {
        given :
            Neureka.instance().reset()
            Neureka.instance().settings().view().setIsUsingLegacyView(true)

        when : Device device = new DummyDevice()
        then : SimpleNNIntegrationTest.on(device)

        and :
        //=========================================================================
        if(!System.getProperty("os.name").toLowerCase().contains("windows")) return
        //=========================================================================

        when : Device gpu = OpenCLPlatform.PLATFORMS().get(0).getDevices().get(0)
        then : SimpleNNIntegrationTest.on(gpu)

        // Some more asserts:
        and : Tsr t = new Tsr([2, 2], 4).setRqsGradient(true).add(gpu)
        when :
            t.backward(1)
            Tsr g = t.find(Tsr.class)

        then :
            assert g.toString().contains("[2x2]:(1.0, 1.0, 1.0, 1.0)")
            assert t.toString().contains("[2x2]:(4.0, 4.0, 4.0, 4.0):g:(1.0, 1.0, 1.0, 1.0)")
            assert t.isOutsourced()
            assert g.isOutsourced()
            //t.setIsOutsourced(false)
            //assert !g.isOutsourced()

    }





}
