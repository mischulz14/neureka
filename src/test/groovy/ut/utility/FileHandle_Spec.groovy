package ut.utility

import neureka.Neureka
import neureka.Tsr
import neureka.devices.file.FileHandle
import neureka.devices.file.CSVHandle
import neureka.devices.file.IDXHandle
import neureka.dtype.DataType
import neureka.dtype.NumericType
import neureka.dtype.custom.F64
import neureka.dtype.custom.I16
import neureka.dtype.custom.UI8
import neureka.view.NDPrintSettings
import spock.lang.Specification

class FileHandle_Spec extends Specification
{
    def setupSpec()
    {
        reportHeader """
            This specification covers the expected functionality of
            various "FileHandle" implementations.
            Such implementations ought to be able to save tensors to
            a given directory in the file format that they represent.
            Functionalities like : "store", "restore" and "free" must
            behave as expected.
            (For more information take a look a the "FileHandle" & "Storage" interface)
        """
    }

    def setup() {
        Neureka.get().reset()
        Neureka.get().settings().view().ndArrays({ NDPrintSettings it -> it.setCellSize(15) })

        File dir = new File( "build/test-can" )
        if ( ! dir.exists() ) dir.mkdirs()
    }

    def 'Test writing IDX file format.'(
        Tsr<?> tensor, Class<NumericType<?,?,?,?>> type, String filename, String expected
    ) {
        given:
            Neureka.get().settings().view().ndArrays({ NDPrintSettings it ->
            it.setIsScientific( true )
            it.setIsMultiline( false )
            it.setHasGradient( true )
            it.setCellSize( 1 )
            it.setHasValue( true )
            it.setHasRecursiveGraph( false )
            it.setHasDerivatives( false )
            it.setHasShape( true )
            it.setIsCellBound( false )
            it.setPostfix(  "" )
            it.setPrefix(  ""  )
            it.setHasSlimNumbers(  false )
        })

        when : 'A new IDX file handle for the given filename is being instantiated.'
            IDXHandle idx = new IDXHandle(tensor, "build/test-can/"+filename)

        then : 'The file will then exist at the following path: '
            new File("build/test-can/"+filename).exists()

        when : 'The "load" method is being called in order to load the tensor into memory.'
            Tsr loaded = idx.load()

        then : 'The loaded tensor is as expected...'
            loaded != null
            loaded.toString() == expected
            loaded.getDataType().getRepresentativeType() == type

        where : 'The following paths and file names are being used for testing : '
            tensor                  | type      | filename          || expected
            Tsr.of([2, 4], -2d..4d) | F64.class | "test.idx3-ubyte" || "(2x4):[-2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, -2.0]"
            Tsr.of([2, 4], 2d)      | F64.class | "test2.idx"       || "(2x4):[2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0]"
    }


    def 'Test reading IDX file format.'(
        String filename, String expected
    ) {
        given : 'A variable for storing a hash :'
            def hash = ""

        when : 'The given idx file is being loaded by the "IDXHead" class into a new tensor...'
            IDXHandle idx = new IDXHandle( "build/resources/test/idx/" + filename )
            Tsr loaded = idx.load()
        and : '... this new tensor is then hashed ...'
            loaded.forEach( e -> hash = ( hash + e ).digest("md5") )
            /*
            // Use the following for an ASCII respresentation of the tensor :
            int i = 0
            loaded.forEach({ e ->
                def norm = (double)( (int) e& 0xff) / 255
                if ( norm < 0.1 ) print(" ")
                else if ( norm < 0.5 ) print( "." )
                else if ( norm < 0.7 ) print( "*" )
                else s.append( "#" )
                if ( ( i ) % 28==27 ) print( "\n" )
                i ++
            })
            */
        then : 'The hash is as expected.'
            hash == expected
        and : 'The loaded tensor has the expected data type.'
            loaded.dataType.getRepresentativeType() == I16.class
            loaded.dataType == DataType.of( I16.class )
        and : 'It contains the correct array type.'
            loaded.mut.data.ref instanceof short[]

        and : 'The "IDXHead" instance has the expected state :'
            idx.valueSize == 28 * 28
            idx.valueSize == 28 * 28 * 1 // 1 := ubyte
            idx.fileName == filename
            idx.location.endsWith( filename )
            idx.totalSize == 28 * 28 * 1 + 16
            idx.dataType != loaded.dataType
            idx.dataType == DataType.of( UI8.class ) // The underlying data is unsigned byte! (Not supported by JVM)

        where : 'The following files and the expected hashes of their data were used :'
            filename             || expected
            "MNIST-sample-1.idx" || "c74e87c7a93605e7a1660ec9e17dcf9f"
            "MNIST-sample-2.idx" || "4a57297981456a467a302c8738b3ac50"
            "MNIST-sample-3.idx" || "87eade8bb5659d324030f4e84f6745e7"
    }


    def 'We can load image files as tensors.'(
            String filename, List<Integer> shape, String expected
    ) {
        given :
            var hash = ""
            var type = filename.split("\\.")[1].toLowerCase()

        when :
            FileHandle handle = FileHandle.FACTORY.getLoader(type).load("build/resources/test/$type/" + filename, null)
            Tsr loaded = handle.load()
            loaded.forEach(e -> hash = ( hash + e ).digest('md5') )
        /*
            // Use the following code to get an ASCII representation of the image (from the loaded tensor):
            int i = 0
            float pixel = 0
            loaded.forEach({ e ->
                var channels = ( type == "png" ? 4 : 3 )
                var norm = (double)( (int) e& 0xff) / ( 255 * channels )
                pixel += norm

                if (  i % channels == ( channels - 1 ) ) {
                    if ( pixel < 0.1 ) print(" ")
                    else if ( pixel < 0.2 ) print("`")
                    else if ( pixel < 0.5 ) print(".")
                    else if ( pixel < 0.7 ) print("*")
                    else print("#")
                    if ((i) % (loaded.shape(1)*channels) == (loaded.shape(1)*channels-1)) print("\n")
                    pixel = 0
                }
                i ++
            })
        */

        then :
            loaded != null
            !loaded.isVirtual()
            loaded.size() == shape.inject( 1, {prod, value -> prod * value} )
            loaded.getDataType().getRepresentativeType() == I16.class // Auto convert! (stored as I16)
            hash == expected

        and :
            handle.shape == shape as int[]
            handle.valueSize == shape.inject( 1, {prod, value -> prod * value} )
            handle.totalSize == shape.inject( 1, {prod, value -> prod * value} ) //28 * 28 * 1 + 16
            handle.location.endsWith( filename )
            handle.dataType == DataType.of( UI8.class )
            loaded.dataType == DataType.of( I16.class )

        where : 'The following jpg files with their expected shape and hash were used.'
            filename           || shape          | expected
            "small.JPG"        || [260, 410, 3]  | "b0e336b03f2ead7297e56b8ca050f34d"
            "tiny.JPG"         || [10, 46, 3]    | "79bf5dd367b5ec05603e395c41dafaa7"
            "super-tiny.JPG"   || [3, 4, 3]      | "a834038d8ddc53f170fa426c76d45df2"
            "tiny.png"         || [90, 183, 4]   | "63bcd21a7580242a1b562bb49cb53e74"
    }

    def 'The FileDevice component "CSVHead" can read CSV file formats and load them as tensors.'(
            String filename, Map<String, Object> params, int byteSize, List<Integer> shape, String expected
    ) {
        when :
            CSVHandle csv = new CSVHandle( "build/resources/test/csv/" + filename, params )
            Tsr loaded = csv.load()
            var hash = loaded.toString().digest('md5')//.forEach( e -> hash = ( hash + e ).digest('md5') )
            //println(loaded)
        then :
            loaded != null
            !loaded.isVirtual()
            loaded.size() == shape.inject( 1, {prod, value -> prod * value} )
            loaded.getDataType().getItemTypeClass() == String.class // Auto convert! (stored as String)
            hash == expected

        and :
            csv.shape == shape as int[]
            csv.valueSize == shape.inject( 1, {prod, value -> prod * value} )
            csv.totalSize == byteSize
            csv.dataSize == byteSize
            csv.location.endsWith( filename )
            csv.dataType == DataType.of( String.class )
            loaded.dataType == DataType.of( String.class )

        where : 'The following jpg files with their expected shape and hash were used.'
            filename      | params                                        || byteSize | shape    | expected
            "biostats.csv"| [:]                                           || 753      | [19, 5]  | "a3dc4ede7814b5d35d20a8c9310cd63c"
            "biostats.csv"| [firstRowIsLabels:true]                       || 702      | [18, 5]  | "baac406a366a51cb6d69e97a90711050"
            "biostats.csv"| [firstColIsIndex:true]                        || 639      | [19, 4]  | "90c5d3a4b1ea87901879993bb79e9bc1"
            "biostats.csv"| [firstColIsIndex:true,firstRowIsLabels:true]  || 594      | [18, 4]  | "61d75f3dccc8d6987e686d151a423310"
    }


    def 'Fully labeled tenors will be stored with their labels included when saving them as CSV.'()
    {
        given:
            Tsr t = Tsr.of(DataType.of(String.class), [2,3], [
                            '1', 'hi', ':)',
                            '2', 'hey', ';)'
                        ])
                        .mut.labelAxes([
                            ['r1', 'r2'],
                            ['A', 'B', 'C']
                        ])

        expect:
            t.toString() == "(2x3):[\n" +
                            "   (        A       )(       B       )(       C        )\n" +
                            "   [        1       ,        hi      ,        :)       ]:( r1 ),\n" +
                            "   [        2       ,       hey      ,        ;)       ]:( r2 )\n" +
                            "]"
            !new File("build/resources/test/csv/test.csv").exists()

        when:
            def csvHead = new CSVHandle( t, "build/resources/test/csv/test.csv" )
            Tsr loaded = csvHead.load()
        then:
            loaded.toString() == "(2x3):[\n" +
                                 "   (        A       )(       B       )(       C        ):( test )\n" +
                                 "   [        1       ,        hi      ,        :)       ]:( r1 ),\n" +
                                 "   [        2       ,       hey      ,        ;)       ]:( r2 )\n" +
                                 "]"
            new File("build/resources/test/csv/test.csv").exists()
            new File("build/resources/test/csv/test.csv").text == ",A,B,C\nr1,1,hi,:)\nr2,2,hey,;)\n"

        when:
            csvHead.free()

        then:
            !new File("build/resources/test/csv/test.csv").exists()
    }


    def 'Partially labeled tenors will be stored with their labels included when saving them as CSV.'()
    {
        given:
            Tsr t = Tsr.of(DataType.of(String.class), [2,3], [
                                '1', 'hi', ':)',
                                '2', 'hey', ';)'
                            ])
                            .mut.labelAxes([
                                'ROW':null,
                                'COL':['A', 'B', 'C']
                            ])

        expect:
            t.toString() == "(2x3):[\n" +
                    "   (        A       )(       B       )(       C        )\n" +
                    "   [        1       ,        hi      ,        :)       ]:( 0 ),\n" +
                    "   [        2       ,       hey      ,        ;)       ]:( 1 )\n" +
                    "]"
            !new File("build/resources/test/csv/test.csv").exists()

        when:
            def csvHead = new CSVHandle( t, "build/resources/test/csv/test.csv" )
            Tsr loaded = csvHead.load()
        then:
            loaded.toString() == "(2x3):[\n" +
                                 "   (        A       )(       B       )(       C        ):( test )\n" +
                                 "   [        1       ,        hi      ,        :)       ]:( 0 ),\n" +
                                 "   [        2       ,       hey      ,        ;)       ]:( 1 )\n" +
                                 "]"
            new File("build/resources/test/csv/test.csv").exists()
            new File("build/resources/test/csv/test.csv").text == ",A,B,C\n0,1,hi,:)\n1,2,hey,;)\n"

        when:
            csvHead.free()

        then:
            !new File("build/resources/test/csv/test.csv").exists()
    }


}
