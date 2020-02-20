package util;
import neureka.Tsr;
import util.gui.MessageFrame;
import org.junit.Assert;

import java.util.List;

public class NTester extends Assert
{
    //private MessageFrame _verbose_frame;

    private static MessageFrame RESULT_FRAME;
    private static MessageFrame ERROR_FRAME;

    private static volatile int _global_tests;
    static {
        _global_tests = 0;
        if(System.getProperty("os.name").toLowerCase().contains("windows")){
            RESULT_FRAME = new MessageFrame("[NEUREKA UNIT TEST]: results");
            ERROR_FRAME =  new MessageFrame("[NEUREKA UNIT TEST]: fails");
        }

    }
    protected static String BAR = "[|]";
    protected static String LINE = "--------------------------------------------------------------------------------------------";
    private int _positive_assertions = 0;
    private int _assertion_count = 0;

    private int _success = 0;
    private int _tests = 0;

    private String _session = "";

    public NTester(String name) {
        if(System.getProperty("os.name").toLowerCase().contains("windows")){
            //_verbose_frame = new MessageFrame("[NEUREKA UNIT TEST]:("+name+"): verbose results");
            printlnResult("\nT[ "+name+" ]:");
        }
        _global_tests++;
    }

    public NTester(String name, boolean liveLog){
        if(liveLog && System.getProperty("os.name").toLowerCase().contains("windows")){
            //_verbose_frame = new MessageFrame(name+" - TEST PROCESS");

            printResult("\nT["+name+"]: ");
        }
        _global_tests++;
    }

    public void close(){
        //if(_verbose_frame !=null){
        //    _verbose_frame.close();
        //    _verbose_frame=null;
        //}
        if(RESULT_FRAME !=null && _global_tests>3){
            try {
                Thread.sleep((int)Math.pow(_global_tests-3, 2)*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //RESULT_FRAME.close();
            //RESULT_FRAME=null;
        }


    }

    public int testContains(String result, List<String> expected, String description){
        printSessionStart(description);
        println(BAR + "  Tensor: "+result);
        println(BAR + "-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        for(String element : expected){
            this.assertStringContains("result", result, element);
        }
        return (printSessionEnd()>0)?1:0;
    }

    public int testContains(String result, String[] expected, String description){

        printSessionStart(description);
        println(BAR + "  Tensor: "+result);
        println(BAR + "-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        for(String element : expected){
            this.assertStringContains("result", result, element);
        }
        return (printSessionEnd()>0)?1:0;
    }

    protected void printSessionStart(String message){
        _session = "";
        _tests++;
        _positive_assertions = 0;
        _assertion_count = 0;
        println("");
        println(BAR +"  "+message);
        println("[O][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=][=]=>");
        println(BAR + LINE);
    }
    protected int printSessionEnd(){
        _success += (_positive_assertions == _assertion_count)?1:0;
        println(BAR +"  "+((_positive_assertions >0)?"test successful!"+" "+ _positive_assertions :"test failed!"+" "+(_assertion_count + _positive_assertions))+"/"+ _assertion_count);
        println("[O][=][=][=][=][=][=][=][=][=][=][=]|> "+ _success +"/"+ _tests);
        printResult((_positive_assertions == _assertion_count)? "." :"E");
        bottom();
        if(_positive_assertions != _assertion_count) failSession();
        //assert _positive_assertions == _assertion_count;
        return _positive_assertions;
    }

    protected boolean assertIsEqual(double result, double expected){
        _assertion_count++;
        if(result==expected) {
            println(BAR +"  [result]:("+result+") == [expected]:("+expected+") -> test successful.");
            _positive_assertions = (_positive_assertions <0)? _positive_assertions : _positive_assertions +1;
            println(BAR + LINE);
            return true;
        } else {
            println(BAR +"  [result]:("+result+") =|= [expected]:("+expected+") -> test failed!");
            _positive_assertions = (_positive_assertions <0)? _positive_assertions -1:-1;
            println(BAR + LINE);
            return false;
        }
    }

    protected boolean assertStringContains(String name, String result, String expected){
        _assertion_count++;
        if(result.contains(expected)) {
            println(BAR +"  ["+name+"]:("+result+") "+((result.length()>22)?"\n"+ BAR +"    contains   -> test successful!"+"\n"+ BAR +" ":"contains")+" [expected]:("+expected+")"+((result.length()>22)?"":" -> test successful!"));
            _positive_assertions = (_positive_assertions <0)? _positive_assertions : _positive_assertions +1;
            println(BAR + LINE);
            assertEquals(true, true);
            return true;
        } else {
            println(BAR +"  ["+name+"]:("+result+") "
                +((result.length()>22)
                    ?"\n"+ BAR +"    not contains   -> test failed!"+"\n"+ BAR +" "
                    :"not contains")+" [expected]:("+expected+")"+((result.length()>22)?"":" -> test failed!"));
            _positive_assertions = (_positive_assertions <0)? _positive_assertions -1:-1;
            println(BAR + LINE);
            //failSession();
            return false;
        }
    }

    protected boolean assertIsEqual(String name, String result, String expected){
        _assertion_count++;
        if(result.equals(expected)) {
            println(BAR +"  ["+name+"]:("+result+") "+((result.length()>22)
                    ?"\n"+ BAR +"    ==   -> test successful!"+"\n"+ BAR +" ":"==")+" [expected]:("+expected+")"+((result.length()>22)?""
                    :" -> test successful!"));
            _positive_assertions = (_positive_assertions <0)? _positive_assertions : _positive_assertions +1;
            println(BAR + LINE);
            assertEquals(true, true);
            return true;
        } else {
            println(BAR +"  ["+name+"]:("+result+") "
                    +((result.length()>22)
                    ?"\n"+ BAR +"    =|=   -> test failed!"+"\n"+ BAR +" "
                    :"=|=")+" [expected]:("+expected+")"+((result.length()>22)?"":" -> test failed!"));
            _positive_assertions = (_positive_assertions <0)? _positive_assertions -1:-1;
            println(BAR + LINE);
            //failSession();
            return false;
        }
    }

    protected boolean assertIsEqual(String result, String expected){
        _assertion_count++;
        if(result.equals(expected)) {
            println(BAR +"  [result]:("+result+") "+((result.length()>22)
                    ?"\n"+ BAR +"    ==  "+"\n"+ BAR +" "
                    :"==")+" [expected]:("+expected+") -> test successful.");
            _positive_assertions = (_positive_assertions <0)? _positive_assertions : _positive_assertions +1;
            println(BAR + LINE);
            assertTrue(true);
            return true;
        } else {
            println(BAR +"  [result]:("+result+") "+((result.length()>22)
                    ?"\n"+ BAR +"    =|=  "+"\n"+ BAR +" "
                    :"=|=")+" [expected]:("+expected+") -> test failed!");
            _positive_assertions = (_positive_assertions <0)? _positive_assertions -1:-1;
            println(BAR + LINE);
            //failSession();
            return false;
        }
    }

    protected void failSession(){
        ERROR_FRAME.print(_session);
        fail(_session);
    }

    protected String stringified(int[] a){
        String result = "";
        for(int ai : a) {
            result += ai+", ";
        }
        return result;
    }
    protected String stringified(short[] a){
        String result = "";
        for(short ai : a) {
            result += ai+", ";
        }
        return result;
    }
    public String stringified(double[] a){
        if(a==null){
            return "null";
        }
        String result = "";
        for(double ai : a) {
            result += Tsr.Utility.Stringify.formatFP(ai)+", ";
        }
        return result;
    }
    protected String stringified(double[][] a){
        String result = "";
        for(double[] ai : a) {
            result+="(";
            for(double aii : ai){
                result += aii+", ";
            }
            result+="), ";
        }
        return result;
    }
    protected String stringified(float[] a){
        String result = "";
        for(float ai : a) {
            result += ai+", ";
        }
        return result;
    }

    protected void print(String message){
        _session +=message;
        //if(_verbose_frame !=null){
        //    _verbose_frame.print(message);
        //}
        //if(ERROR_FRAME !=null){
        //    ERROR_FRAME.print(message);
        //}
    }
    protected void println(String message){
        _session +=message+"\n";
        //if(_verbose_frame !=null){
        //    _verbose_frame.println(message);
        //}
        //if(ERROR_FRAME !=null){
        //    ERROR_FRAME.println(message);
        //}
    }


    protected void printResult(String message){
        if(RESULT_FRAME !=null){
            RESULT_FRAME.print(message);
        }
    }
    protected void printlnResult(String message){
        if(RESULT_FRAME !=null){
            RESULT_FRAME.println(message);
        }
    }

    protected void bottom(){
        if(RESULT_FRAME !=null){//_verbose_frame !=null &&
                //_verbose_frame.bottom();
            RESULT_FRAME.bottom();
        }
    }

}
