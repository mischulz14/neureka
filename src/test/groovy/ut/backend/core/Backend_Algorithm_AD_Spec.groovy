package ut.backend.core

import neureka.Neureka
import neureka.Tsr
import neureka.autograd.ADAgent
import neureka.backend.api.Operation
import neureka.backend.api.AutoDiffMode
import neureka.calculus.args.Arg
import neureka.calculus.Function
import neureka.backend.api.ExecutionCall
import neureka.backend.api.Algorithm
import neureka.backend.main.algorithms.Activation
import neureka.backend.main.algorithms.Broadcast
import neureka.backend.main.algorithms.Convolution
import neureka.backend.main.algorithms.ElementWise
import neureka.devices.Device
import neureka.ndim.config.NDConfiguration
import spock.lang.Specification

class Backend_Algorithm_AD_Spec extends Specification
{

    def 'Operator implementations behave as expected.'(
            Algorithm imp
    ){

        given : 'The current Neureka instance is being reset.'
            Neureka.get().reset()

        and : 'A mock ExecutionCall.'
            def call = Mock(ExecutionCall)
            call.inputs() >> new Tsr<?>[0]

        when : 'A auto diff mode is being created by every algorithm...'
            AutoDiffMode mode = imp.autoDiffModeFrom( call )

        then : 'The agent is configured to perform forward-AD and it contains the derivative generated by the function!'
            mode != null

        where : 'The variable "imp" is from a List of OperationType implementations of type "Operator".'
            imp << Neureka.get().backend()
                    .getOperations()
                    .stream()
                    .filter(
                            e -> e.isOperator() && e.getOperator().length()==1 && e.supports( ElementWise.class )
                    ).map(
                    e -> e.getAlgorithm( ElementWise.class )
                    )
    }


    def 'Activation implementations behave as expected.'(
            Algorithm imp
    ){

        given : 'The current Neureka instance is being reset.'
            Neureka.get().reset()

        and : 'A mock Function.'
            def function = Mock(Function)
            def derivative = Mock(Tsr)
            def mutate = Mock(Tsr.Unsafe)
            function.derive(*_) >> derivative
            function.executeDerive(*_) >> derivative
            derivative.getUnsafe() >> mutate

        and : 'A mock ExecutionCall.'
            var call = ExecutionCall.of().running(Mock(Operation)).algorithm(imp).on(Mock(Device))

        when :
            var suitability = imp.isSuitableFor(call)
        then :
            0 <= suitability && suitability <= 1

        when :
            var mode = imp.autoDiffModeFrom(call)
        then :
            mode != null

        where : 'The variable "imp" is from a List of OperationType implementations of type "Activation".'
            imp << Neureka.get().backend()
                    .getOperations()
                    .stream()
                    .filter( e -> e.supports( Activation.class ) )
                    .map( e -> e.getAlgorithm( Activation.class ) )
    }


    def 'Convolution implementations behave as expected.'( Algorithm imp ){

        given : 'The current Neureka instance is being reset.'
            Neureka.get().reset()

        and : 'A mock ExecutionCall.'
            var call = ExecutionCall.of().running(Mock(Operation)).algorithm(imp).on(Mock(Device))

        when :
            var suitability = imp.isSuitableFor(call)
        then :
            0 <= suitability && suitability <= 1

        when :
            var mode = imp.autoDiffModeFrom(call)
        then :
            mode != null

        where : 'The variable "imp" is from a List of Operation implementations of type "Convolution".'
            imp << Neureka.get().backend()
                                .getOperations()
                                .stream()
                                .filter(
                                        e ->
                                                e.isOperator() &&
                                                        e.getOperator().length()==1 &&
                                                            e.supports( Convolution.class )
                                ).map( e -> e.getAlgorithm( Convolution.class ) )
    }


    def 'Broadcast implementations behave as expected.'( Algorithm imp )
    {
        given : 'The current Neureka instance is being reset.'
            Neureka.get().reset()

        and : 'A mock Function.'
            def function = Mock( Function )
            def derivative = Mock( Tsr )
            def ndConf = Mock(NDConfiguration)
            function.derive(*_) >> derivative
            function.executeDerive(*_) >> derivative
            derivative.getNDConf() >> ndConf
            ndConf.shape() >> [1, 2]
            derivative.getItemType() >> Float
            derivative.itemType() >> Float

        and : 'A mock ExecutionCall.'
            def call = Mock( ExecutionCall )
            def arg = Mock(Arg.Derivative)
            call.get(Arg.Derivative.class) >> arg
            call.autogradMode() >> AutoDiffMode.FORWARD_AND_BACKWARD // This should cause an error! Broadcast does not support forward prop!


        when : 'A new ADAgent is being instantiated by calling the given implementation with these arguments...'
            ADAgent agent = imp.supplyADAgentFor(function, call)

        then : 'An exception is being thrown because implementations of type "Broadcast" can only perform reverse mode AD!'
            def exception = thrown( IllegalArgumentException )
            exception.message == "Broadcast implementation does not support forward-AD!"

        when : 'The agent generator is called once more with the forward flag set to false...'
            agent = imp.supplyADAgentFor(function, call)

        then :
            (0.._) * call.inputs() >> new Tsr[]{derivative, derivative}
            (0.._) * call.input(_) >> derivative
            (1.._) * call.autogradMode() >> AutoDiffMode.BACKWARD_ONLY // This fixes the problem!

        and : 'No exception is being thrown and the agent is configured to perform backward-AD.'
            agent.partialDerivative().get() == derivative || agent.partialDerivative().get().toString({it.isMultiline=false}) == "(1x2):[  0.0 ,   0.0 ]"

        where : 'The variable "imp" is from a List of OperationType implementations of type "Convolution".'
            imp << Neureka.get().backend()
                                .getOperations()
                                .stream()
                                .filter(
                                        e ->
                                                e.isOperator() &&
                                                        e.supports( Broadcast.class )
                                )
                                .map( e -> e.getAlgorithm( Broadcast.class ) )
    }



}
