package ut.backend.core

import neureka.Data
import neureka.MutateTsr
import neureka.Neureka
import neureka.Tsr
import neureka.backend.api.DeviceAlgorithm
import neureka.devices.host.CPU
import neureka.devices.opencl.OpenCLDevice
import neureka.backend.api.ExecutionCall
import neureka.backend.api.Algorithm
import neureka.backend.main.algorithms.Activation
import neureka.backend.main.algorithms.BiElementWise
import neureka.ndim.config.NDConfiguration
import spock.lang.Specification

class Backend_Algorithm_Implementation_Spec extends Specification
{
    def setupSpec()
    {
        Neureka.get().reset()

        reportHeader """
                   This specification defines the behavior of implementations of the 
                   Algorithm interface! <br> 
        """
    }

    def 'Operator implementations have expected Executor instances.'(
            Algorithm imp
    ){

        when : 'Host- and CL- executor instance are being fetched...'
            def hostExecutor = imp.getImplementationFor( CPU.class )
            def clExecutor = imp.getImplementationFor( OpenCLDevice.class )

        then : 'The variables containing the executor instances are not null.'
            hostExecutor != null
            clExecutor != null || !Neureka.get().canAccessOpenCLDevice()

        where : 'The variable "imp" is from a List of OperationType implementations of type "Operator".'
            imp << Neureka.get().backend()
                    .getOperations()
                    .stream()
                    .filter(
                        e ->
                                    e.isOperator() &&
                                    e.getOperator().length() == 1 &&
                                    e.supports( BiElementWise.class )
                    ).map( e -> e.getAlgorithm( BiElementWise.class ) )

    }



    def 'Activation implementations have expected Executor instances.'(
            Algorithm imp
    ){
        when : 'Host- and CL- executor instance are being fetched...'
            def hostExecutor = imp.getImplementationFor( CPU.class )
            def clExecutor = imp.getImplementationFor( OpenCLDevice.class )

        then : 'The variables containing the executor instances are not null.'
            hostExecutor != null
            clExecutor != null || !Neureka.get().canAccessOpenCLDevice()

        where : 'The variable "imp" is from a List of OperationType implementations of type "Operator".'
            imp << Neureka.get()
                    .backend()
                    .getOperations()
                    .stream()
                    .filter( e -> e.supports( Activation.class ) )
                    .map( e -> e.getAlgorithm( Activation.class ) )
    }


    def 'HostExecutors of Operator implementations behave as expected.'(
            DeviceAlgorithm imp
    ){
        given : 'Mock instances to simulate an ExecutionCall instance.'
            var call = Mock( ExecutionCall )
            var device = Mock( CPU )
            var tensor = Mock( Tsr )
            var mutate = Mock(MutateTsr)
            var ndConf = Mock(NDConfiguration)
            var hostExecutor = imp.getImplementationFor( CPU.class )
            var nativeExecutor = Mock( CPU.JVMExecutor )
            var dataObj = Mock(Data)

        when : 'Host-executor instance is being called...'
            hostExecutor.run( call )

        then : 'The mock objects are being called as expected.'
            (0.._) * tensor.getMut() >> mutate
            (0.._) * tensor.mut() >> mutate
            (1.._) * call.getDevice() >> device
            1 * device.getExecutor() >> nativeExecutor
            1 * nativeExecutor.threaded( _, _ )
            (0.._) * call.inputs() >> new Tsr[]{ tensor, tensor, tensor }
            (0.._) * call.input({it >= 0 && it <= 2}) >> tensor
            (0.._) * call.input( Number.class, 0 ) >> tensor
            (0.._) * call.input(0) >> tensor
            (0.._) * call.input( Number.class, 1 ) >> tensor
            (1.._) * tensor.size() >> 0
            (0.._) * tensor.itemType >> Double
            (0.._) * tensor.getDataAs(double[]) >> new double[0]
            (0.._) * mutate.data >> dataObj
            (0.._) * dataObj.ref >> new double[0]
            (0.._) * mutate.getDataAs(double[]) >> new double[0]
            (0.._) * mutate.getDataForWriting(double[]) >> new double[0]
            (1.._) * tensor.getNDConf() >> ndConf
            (1.._) * ndConf.isSimple() >> false

        where : 'The variable "imp" is from a List of OperationType implementations of type "Operator".'
            imp << Neureka.get().backend()
                            .getOperations()
                            .stream()
                            .filter(
                                e ->
                                            e.isOperator() &&
                                            e.getOperator().length() == 1 &&
                                            e.supports( BiElementWise.class )
                            )
                            .map( e -> e.getAlgorithm( BiElementWise.class ) )

    }

}
