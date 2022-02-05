package ut.tensors.exceptions

import neureka.Neureka
import neureka.Tsr
import org.slf4j.Logger
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title('Tensors Exception Behavior')
@Narrative('''

    This specification covers the behavior of the $Tsr class in
    exceptional scenarios which are contrary to its intended use.
    The purpose of this is to assert that the $Tsr class will provide
    useful feedback to a user to explain that a misuse of its API
    occurred so that the user can correct this misuse.

''')
class Tensor_Exception_Spec extends Specification
{
    def setupSpec() {
        reportHeader """
                <h2> Tensors Exception Behavior </h2>
                <br> 
                <p>
                    This specification covers the behavior of tensors
                    in exceptional situations.           
                </p>
            """
    }

    @Shared def oldLogger

    def setup() {
        Neureka.get().reset()
        if (Tsr._LOG != null) oldLogger = Tsr._LOG
        Tsr._LOG = Mock( Logger )
    }

    def cleanup() {
        Tsr._LOG = oldLogger
    }

    def "Trying to inject an empty tensor into another causes fitting exception."()
    {
        given : 'A new tensor instance used for exception testing.'
            Tsr t = Tsr.of([6, 6], -1)

        when : 'We try to inject an empty tensor whose size does of course not match...'
            t[[1..3], [1..3]] = Tsr.newInstance()

        then : 'The expected message is being thrown, which tells us that '
            def exception = thrown(IllegalArgumentException)
            assert exception.message.contains("Provided tensor is empty! Empty tensors cannot be injected.")

        // TODO: FIX THE FOLLOWING:
        //and : 'The exception has also been logged.'
        //    1 * Tsr._LOG.error( "Provided tensor is empty! Empty tensors cannot be injected." )
    }

    def 'Passing an invalid object into Tsr constructor causes descriptive exception.'()
    {
        when : 'A tensor is being instantiated with a nonsensical parameter.'
            Tsr.ofRandom(Scanner.class, 2, 4)
        then : 'An exception is being thrown which tells us about it.'
            def exception = thrown(IllegalArgumentException)
            exception.message.contains(
                    "Could not create a random tensor for type 'class java.util.Scanner'!"
            )
    }


    def 'Passing an invalid key object into the "getAt" method causes a descriptive exception.'()
    {
        given : 'A new test tensor is being instantiated.'
            Tsr t = Tsr.of( [2, 3], -1..6 )

        when : 'A nonsensical object is being passed to the tensor.'
            t[ Integer.class ]

        then : 'An exception is being thrown which tells us about it.'
            def exception = thrown(IllegalArgumentException)
            exception.message.contains(
                    "Cannot create tensor slice from key of type 'java.lang.Class'!"
            )
        and : 'The logger logs the exception message!'
            1 * Tsr._LOG.error( "Cannot create tensor slice from key of type 'java.lang.Class'!" )
    }


    def 'Out of dimension bound causes descriptive exception!'()
    {
        when : 'Some more complex slicing is being performed...'
            Tsr t = Tsr.of( [3, 3, 3, 3], 0 )
            t[1..2, 1..3, 1..1, 0..2] = Tsr.of( [2, 3, 1, 3], -4..2 )

        then : 'The slice range 1..3 causes and exception!'
            def exception = thrown(IllegalArgumentException)
            exception.message == "java.lang.IllegalArgumentException: " +
                    "Cannot create slice because ranges are out of the bounds of the targeted tensor.\n" +
                    "At index '1' : offset '1' + shape '3' = '4',\n" +
                    "which is larger than the target shape '3' at the same index!"

        and : 'The logger logs the exception message!'
            1 * Tsr._LOG.error(
                    "Cannot create slice because ranges are out of the bounds of the targeted tensor.\n" +
                    "At index '1' : offset '1' + shape '3' = '4',\n" +
                    "which is larger than the target shape '3' at the same index!",
                    _
            )
    }

    def 'Building a tensor with 0 shape arguments throws an exception.'() {

        when :
            var t = Tsr.ofInts().withShape().all(0)

        then :
            thrown(IllegalArgumentException)

    }

    def 'Building a tensor with "null" as shape argument throws an exception.'() {

        when :
            var t = Tsr.ofInts().withShape(null).all(0)

        then :
            thrown(IllegalArgumentException)

    }

}
