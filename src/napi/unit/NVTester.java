package napi.unit;


import napi.main.core.NVertex;
import napi.main.core.imp.NVertex_Root;
import napi.unit.io.NVTesting_Activation;
import napi.unit.state.NVTesting_Format;
import napi.utility.NMessageFrame;

public class NVTester {

	NMessageFrame Console = new NMessageFrame("NV-Unit-Test:");
	
	NMessageFrame ResultConsole = new NMessageFrame("NV-Unit-Test-Result:");
	
	public NVTester(){}
	/*
		Function IDs:
		 case 0:  ReLu
		 case 1:  Sigmoid
		 case 2:  Tanh
		 case 3:  Quadratic
		 case 4:  Ligmoid (rectifier)
		 case 5:  Linear
		 case 6:  Gaussian
	*/
	public void Test()
	{
		InputFormattingTest();
	    BaseFunctionsActivationTest();
	    
	    testCoreTypes();
	    
	    

	    componentTest();
	    
	    networkTest();
	    advancedActivationTest();
	    rootActivationTest();
	    
	    
	    //NVertex source = new NVertex_Root(6, true, false, -1, 3);
	    //NVertex head = new NVertex_Root(4, false, false, 1, 2);
	    
	    //source.randomizeInput();
	    //head.connect(source);
	    //head.weightAll();
	    //head.randomizeBiasAndWeight();
	    
	    //System.out.println(head.toString());
	    
	    
	    
	    
	    //simpleRootTest();
	    testRootDeriviation();

		weightStateTest();
	}
	
	public void testRootDeriviation() {
		
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		NVertex parent = new NVertex_Root(1, false, false, 0, 3);//quadratic!
		NVertex child = new NVertex_Root(parent, true, false, -1, 4);//ligmoid

        NVertex src = new NVertex_Root(1,true, false, -10, 0);//relu

        child.connect(src);

		parent.connect(child);
		child.setAllInput(0, 2.2);//2.2 * 5 -> 10.5; 10.5*10.5 -> 105.25; -> 105.25...
		parent.setWeight(0, 0, 0, 5);


		NVertex[] structure = {parent, child};
		double[] expected = {105.25023142345};
		double[] relational = {1};
		tester.testGraph_RelationalDeriviation(structure, parent, expected, relational);
	}
	
	public void simpleRootTest() 
	{
		//Hidden:
		NVertex SuperRoot = new NVertex_Root(1, false, false, 0, 0);
		NVertex.State.turnIntoParentRoot(SuperRoot);
		NVertex basicSub = new NVertex_Root(1, true, false, 1, 0);
		NVertex.State.turnIntoBasicChild(basicSub, SuperRoot);
		
		NVertex src = new NVertex_Root(1, true, false, -1, 0);
		src.setAllInput(0, -3);
		src.asExecutable().cleanup();
		src.asExecutable().forward();
		
		basicSub.connect(src);
		basicSub.setWeight(0, 0, 0, -2);
		
		SuperRoot.connect(basicSub);
		SuperRoot.setWeight(0, 0, 0, 2);
		
		basicSub.addToAllInput(0, 4);
		System.out.println("\nStart:");
		
		SuperRoot.asExecutable().cleanup();
		basicSub.asExecutable().cleanup();
		
		SuperRoot.asExecutable().forward();
		basicSub.asExecutable().forward();
		
		Console.println("SuperRoot:");
		Console.println(SuperRoot.toString());
		Console.println("SubRoot:");
		Console.println(basicSub.toString());
	}
	
	public int[] neuralActivationTest() 
	{	
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		
		int TestCounter = 0;
		int SuccessCounter = 0;
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		NVertex head = new NVertex_Root(1, true, false, 0, 0);
		NVertex child = new NVertex_Root(1, false, false, 1, 0);
		
		head.connect(child);
		child.setAllInput(0, 2);
		NVertex[] net = {head, child};
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, head, 2, "Activating and being first layer:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		
		return null;
	}
	public int[] ActivationTest() 
	{	
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		
		int TestCounter = 0;
		int SuccessCounter = 0;
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		NVertex head = new NVertex_Root(1, true, false, 0, 0);
		NVertex child = new NVertex_Root(1, false, false, 1, 0);
		
		
		head.connect(child);
		child.setAllInput(0, 2);
		NVertex[] net = {head, child};
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, head, 2, "(Root!) Activating and being first layer:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		
		return null;
	}
	
	public int[] rootActivationTest() 
	{
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		
		int TestCounter = 0;
		int SuccessCounter = 0;
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		NVertex mother = new NVertex_Root(1, true, false, 0, 0);
		NVertex child = new NVertex_Root(1, false, false, 1, 0);
		
		NVertex.State.turnIntoChildOfParent(child, mother);
		mother.connect(child);
		child.setAllInput(0, 2);
		NVertex[] net = {mother, child};
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 2, "Activating and being first layer:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		mother = new NVertex_Root(1,false, false, 0, 0);
		child = new NVertex_Root(mother.asNode(),false, false, 1, 0);
		
		//NVertex.State.turnIntoChildOfMother(child, mother);
		mother.connect(child);
		child.setAllInput(0, 2);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 0, "Activating without being first layer, last layer or being connected to first layer:");
		
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		mother = new NVertex_Root(1,false, false, 0, 0);
		child = new NVertex_Root(mother.asNode(),false, false, 1, 0);
		
		//NVertex.State.turnIntoChildOfMother(child, mother);
		mother.connect(child);
		child.setAllInput(0, -21);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 0, "Activating without being first layer, last layer or being connected to first layer:");
		
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		mother = new NVertex_Root(1,false, false, 0, 0);
		child = new NVertex_Root(1 ,false, false, 1, 0);
				
		NVertex.State.turnIntoChildOfParent(child, mother);
		mother.connect(child);
		child.setAllInput(0, 3);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 0, "Activating without being first layer or being connected to first layer: (using NVertex.State.turnIntoChildOfMother(child, mother);)");
		
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
		
		mother.connect(child);
		NVertex.State.turnIntoChildOfParent(child, mother);

		child.setAllInput(0, 2);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 0,"Activating on root whose components were connected as both mothers:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		Console.println("Checking if weight is set of network whose components were connected as both mothers:");
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
		
		mother.connect(child);
		NVertex.State.turnIntoChildOfParent(child, mother);
		
		double[] w = mother.getWeight(0, 0);
		if(w!=null) {SuccessCounter++;}
		TestCounter++;
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");

		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
				
		NVertex.State.turnIntoChildOfParent(child, mother);
		mother.connect(child);
		child.setAllInput(0, -2);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, -0.0002, "Activating on root with two cascading relu nodes and negative input (-2->-0.02->-0.0002):");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
				
		mother.connect(child);
		NVertex.State.turnIntoChildOfParent(child, mother);
		mother.setWeight(0, 0, 0, -1.0);
		child.setAllInput(0, -2);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, +0.02, "Activating on root with two cascading relu nodes and negative input and negative weight(-2->-0.02->+0.02):");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		NVertex source = new NVertex_Root(1,true, false, -1, 0);
		
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
		
		source.setAllInput(0, 4);
		source.asExecutable().cleanup();
		source.asExecutable().forward();
		source.asExecutable().cleanup();
		
		NVertex.State.turnIntoChildOfParent(child, mother);	
		mother.connect(child);
		child.connect(source);
		child.setWeight(0, 0, 0, 1.0);
		//child.setInput(0, -2);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, +4, "Activating on root connected to first layer neuron (setting weight to 1):");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		Console.println("Checking if an input root will retain weight upon connecting to neuron:");
		source = new NVertex_Root(1,true, false, -1, 0);
		
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
		
		source.setAllInput(0, 4);
		source.asExecutable().cleanup();
		source.asExecutable().forward();
		source.asExecutable().cleanup();
		
		NVertex.State.turnIntoChildOfParent(child, mother);	
		mother.connect(child);
		child.connect(source);
		//mother.setWeight(-1, 0, 0);
		child.setWeight(0, 0, 0, 2.5);
		w = child.getWeight(0, 0); 
		
		//net = new NVertex[2]; net[0]=mother; net[1]=child;
		TestCounter++;
		//SuccessCounter+=this.testRootActivation(net, mother, +8);
		if(w!=null) {if(w[0]==2.5) {SuccessCounter++;}}
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		source = new NVertex_Root(1,true, false, -1, 0);
		mother = new NVertex_Root(1,true, false, 1, 0);
		child = new NVertex_Root(1,false, false, 0, 0);
		
		source.setAllInput(0, 3);
		source.asExecutable().cleanup();
		source.asExecutable().forward();
		source.asExecutable().cleanup();
		
		NVertex.State.turnIntoChildOfParent(child, mother);	
		mother.connect(child);
		child.connect(source);
		//mother.setWeight(-1, 0, 0);
		child.setWeight(0, 0, 0, 2.0);
		net = new NVertex[2]; net[0]=mother; net[1]=child;
		//net = new NVertex[2]; net[0]=mother; net[1]=child;
		
		SuccessCounter+=tester.testGraph_ScalarActivation(net, mother, 6, "Checking if an input root will retain weight upon connecting to neuron:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
	    System.out.println(source.getActivation(0)+" ...\n"+child.toString());
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		Console.println("Checking root derivatrives:");
		
		source = new NVertex_Root(1,true, false, -1, 0);
		
		mother = new NVertex_Root(1,true, false, 0, 0);
		child = new NVertex_Root(1,false, false, 1, 0);
		NVertex otherChild = new NVertex_Root(1,false, false, 2, 0);

		NVertex.State.turnIntoChildOfParent(child, mother);	
		NVertex.State.turnIntoChildOfParent(otherChild, mother);
		mother.connect(child);
		mother.connect(otherChild);
		child.setAllInput(0, 2.5);
		otherChild.setAllInput(0, 2);

		net = new NVertex[3]; net[0]=mother; net[1]=child; net[2]=otherChild;
		TestCounter++; double[] derivatives = {1,1,1};
		SuccessCounter+=tester.testGraph_InputDeriviation(net, mother, derivatives);
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		//-----------------------------------------------------------
		System.out.println("\nTEST : "+TestCounter+"\n======================================================================================");
		Console.println("Checking root derivatrives:");
		
		source = new NVertex_Root(1,true, false, -1, 0);
		
		mother     = new NVertex_Root(1,true, false, 0, 0);
		child      = new NVertex_Root(mother.asNode(),false, false, 1, 0);
		otherChild = new NVertex_Root(mother.asNode(),false, false, 2, 0);

		NVertex.State.turnIntoChildOfParent(child, mother);	
		NVertex.State.turnIntoChildOfParent(otherChild, mother);
		mother.connect(child);
		mother.connect(otherChild);
		child.setAllInput(0, 2.5);
		otherChild.setAllInput(0, 2);

		double[] weight = {2, 3};
		mother.setWeight(0,0,weight);
		
		net = new NVertex[3]; 
		net[0]=mother; net[1]=child; net[2]=otherChild;
		TestCounter++; 
		derivatives[0]=1;
		derivatives[0]=2;
		derivatives[0]=3;
		SuccessCounter+=tester.testGraph_InputDeriviation(net, mother, derivatives);
		Console.println("###########################");
		Console.println("|| ROOT STRUCTURE ACTIVATION RESULT:");
		Console.println("||==>> "+SuccessCounter+"/"+TestCounter+"\n");
		Console.println("###########################");
		//-----------------------------------------------------------	
		ResultConsole.println("###########################");
		ResultConsole.println("|| ROOT STRUCTURE ACTIVATION RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
		
		int[] result = {};
		return result;
	}
	
	public void networkTest() 
	{
		NVNetworkTester nvtester = new NVNetworkTester(Console, ResultConsole);
		int TestCounter = 0;
		int SuccessCounter = 0;
		TestCounter++;
		SuccessCounter+=nvtester.testScalarBackprop();
	}
	public void componentTest() 
	{
		int TestCounter =0;
		int SuccessCounter=0;
		NVComponentTester tester  = new NVComponentTester(Console, ResultConsole);
		TestCounter++;
		SuccessCounter+=tester.testFunctionComponent();
		
		TestCounter++;
		SuccessCounter+=tester.testPropertyHub();
	}
	
	public void InputFormattingTest() 
	{
		NVTesting_Format tester = new NVTesting_Format(Console, ResultConsole);	
		int TestCounter = 0;
		int SuccessCounter = 0;
		NVertex n1 = new NVertex_Root(1,true, false, 0, 0, 1);
		Console.println("TESTING -> NVertex_Root(size: 1):");
		TestCounter++;
		SuccessCounter+=tester.inputSizeTest(n1, 1);
		
		n1 = new NVertex_Root(1,true, false, 0, 0, 1);
		Console.println("TESTING -> NVertex_Root (size 1):");
		TestCounter++;
		SuccessCounter+=tester.inputSizeTest(n1, 1);
		
		int vertexSize = 3;
		n1 = new NVertex_Root(vertexSize,true, false, 0, 0, 1);
		Console.println("TESTING -> NVertex_Root(size: "+vertexSize+"):");
		TestCounter++;
		SuccessCounter+=tester.inputSizeTest(n1, 1);
		
		n1 = new NVertex_Root(vertexSize,true, false, 0, 0, 1);
		Console.println("TESTING -> NVertex_Root (size "+vertexSize+"):");
		TestCounter++;
		SuccessCounter+=tester.inputSizeTest(n1, 1);
		
		Console.println("###########################");
		Console.println("|| INPUT FORMAT TEST RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################");
		ResultConsole.println("###########################");
		ResultConsole.println("|| INPUT FORMAT TEST RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
	}
	
	public void weightStateTest() 
	{
		NVTesting_Format tester = new NVTesting_Format(Console, ResultConsole);	
		int TestCounter = 0;
		int SuccessCounter = 0;
		//-----------------------------------------------------------
		NVertex head = new NVertex_Root(3, false, false, 0, 0, 1);
					
		double[][][] expected = {{null},{null},{null}};
		
		//head.setWeight(W);
		SuccessCounter 
		+= tester.testWeightState(head, expected, "testing on NVertex_Neuron");
		TestCounter++;
		//-----------------------------------------------------------
		head = new NVertex_Root(3, false, false, 0, 0, 1);
		
		double[][][] W = {{{1,-8,3}},{{-2,6,-1}},{{-6,4,-1}}};
		
		head.setWeight(W);
		SuccessCounter 
		+= tester.testWeightState(head, W, "testing on NVertex_Neuron");
		TestCounter++;
		//-----------------------------------------------------------
		head = new NVertex_Root(3, false, false, 0, 0, 1);
		NVertex src = new NVertex_Root(3, false, false, 1, 0, 1);
		head.connect(src); 
			
		head.setWeight(W);
		expected = W;
		SuccessCounter 
		+= tester.testWeightState(head, expected, "testing on NVertex_Neuron");
		TestCounter++;
		//-----------------------------------------------------------
			
		Console.println("###########################");
		Console.println("|| WEIGHT STATE TEST END RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################");
		
		ResultConsole.println("###########################");
		ResultConsole.println("|| WEIGHT STATE TEST END RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
	}
	
	public int[] advancedActivationTest()
	{
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		int TestCounter = 0;
		int SuccessCounter = 0;
		//-----------------------------------------------------------
		NVertex head = new NVertex_Root(3,true, false, 0, 0, 1);
		//Input size 1:
		double[][] I = {{4},{4},{4}};	
		double[]   expected = {4, 4, 4};
		SuccessCounter += tester.testNode_VectorActivation(head, I, expected, "{{4},{4},{4}} to {4,4,4} activation on single vertex");	
		TestCounter++;
		//-----------------------------------------------------------
		head = new NVertex_Root(3, true, false, 0, 0, 1);
		I[0][0] = -4; I[1][0] = -4; I[2][0] = -4;
		expected[0] = -0.04; expected[1] = -0.04; expected[2] = -0.04; 
		SuccessCounter += tester.testNode_VectorActivation(head, I, expected, "{{-4},{-4},{-4}} to {-0.04,-0.04,-0.04} activation on single vertex");	
		TestCounter++;
		
		//-----------------------------------------------------------
		head = new NVertex_Root(3, false, false, 0, 0, 1);
		NVertex src = new NVertex_Root(3, true, false, -1, 0, 1);
		
		double[][][] W = {{{1,-8,3}},{{-2,6,-1}},{{-6,4,-1}}};
		
		NVertex[] structure = {head, src};
		
		I[0][0] = -19; I[1][0] = 6; I[2][0] = 2;
		src.setInput(I);
		src.asExecutable().cleanup();
		src.asExecutable().forward();
		
		head.connect(src);
		head.setWeight(W);
		expected[0] = -0.4219; expected[1] = 34.38; expected[2] = 23.14; 

		SuccessCounter 
		+= tester.testGraph_VectorActivation(structure, head, expected, "");
		TestCounter++;
		//-----------------------------------------------------------
		head = new NVertex_Root(3, false, false, 0, 0, 1);
		src = new NVertex_Root(3, true, false, -1, 0, 1);
		
		//double[][][] W = {{{1,-8,3}},{{-2,6,-1}},{{-6,4,-1}}};
		
		structure[0] = head; 
		structure[1] = src;
		
		I[0][0] = -19; I[1][0] = 6; I[2][0] = 2;
		src.setInput(I);
		src.asExecutable().cleanup();
		src.asExecutable().forward();
		
		head.connect(src);
		head.setWeight(W);
		expected[0] = -0.4219; expected[1] = 34.38; expected[2] = 23.14; 

		SuccessCounter 
		+= tester.testGraph_VectorActivation(structure, head, expected, "");
		TestCounter++;
		//-----------------------------------------------------------
		
		//String fail = null; fail.hashCode();
		// To do:   vertex -convection-> vertex2
		
		
		Console.println("###########################");
		Console.println("|| ADVANCED ACTIVATION RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################");
		
		ResultConsole.println("###########################");
		ResultConsole.println("|| ADVANCED ACTIVATION RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
		
		int[] result = {SuccessCounter, TestCounter};
		return result;	
	}
	
	public int[] BaseFunctionsActivationTest()
	{
		NVTesting_Activation tester = new NVTesting_Activation(Console, ResultConsole);
		int TestCounter = 0;
		int SuccessCounter = 0;
		
		NVertex n1 = new NVertex_Root(1,true, false, 0, 0, 1);
		
		//Input size 1:
		double[] I = {4};	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 4);	
		TestCounter++;
		
		I[0] = -2;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -0.02);
		TestCounter++;
		
		I[0] = -0.02;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -0.0002);
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		
		I[0] = 3;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 3);
		TestCounter++;
		
		I[0] = -3;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -3);
		TestCounter++;
		
		//Input size 2:
		//=====================================
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=2; I[1]=3;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 6);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=4; I[1]=3;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 12);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=4; I[1]=3;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 12);//test
		TestCounter++;
		
		//Input size 3:
		//====================================
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 24);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 3, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 576);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 4, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 26.054189745157966);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 4, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=-3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 0.4152428423818953);//test
		TestCounter++;
		
		//================================
		I = new double[1];
		for(int Fi=0; Fi<8; Fi++) 
		{
			Console.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			n1 = new NVertex_Root(1,true, false, 0, Fi, 1);
			
			double a = 0;
			switch(Fi) 
			{
				case 0: a=3;break;
				case 1: a=0.9525741268224331;break;
				case 2: a=0.9486832980505138;break;
				case 3: a=9;break;
				case 4: a=3.048587351573742;break;
				case 5: a=3;break;
				case 6: a=1.2340980408667962E-4;break;
				case 7: a=0.9486832980505138; break;//Default function: tanh
				default: a=0;
			}
			I[0] = 3;	
			SuccessCounter += tester.testNode_ScalarActivation(n1, I, a);
			TestCounter++;
			Console.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
		}
		
		Console.println("###########################");
		Console.println("|| NVertex_Neuron RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################\n");
		
		ResultConsole.println("###########################");
		ResultConsole.println("|| NVertex_Neuron RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################\n");
		
		Console.println("NVertex_Root Activation tests:");
		
		n1 = new NVertex_Root(1,true, false, 0, 0, 1);
		
		//Input size 1:
		I[0] = 4;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 4);	
		TestCounter++;
		
		I[0] = -2;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -0.02);
		TestCounter++;
		
		I[0] = -0.02;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -0.0002);
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		
		I[0] = 3;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 3);
		TestCounter++;
		
		I[0] = -3;	
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, -3);
		TestCounter++;
		
		//Input size 2:
		//=====================================
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=2; I[1]=3;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 6);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=4; I[1]=3;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 12);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		I = new double[2]; I[0]=4; I[1]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 16);//test
		TestCounter++;
		
		//Input size 3:
		//====================================
		n1 = new NVertex_Root(1,true, false, 0, 5, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 24);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 3, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 576);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 4, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 26.054189745157966);//test
		TestCounter++;
		
		n1 = new NVertex_Root(1,true, false, 0, 4, 1);
		n1.addInput(0);
		n1.addInput(0);
		I = new double[3]; I[0]=2; I[1]=-3; I[2]=4;
		SuccessCounter += tester.testNode_ScalarActivation(n1, I, 0.4152428423818953);//test
		TestCounter++;
		
		//================================
		I = new double[1];
		for(int Fi=0; Fi<8; Fi++) {
			Console.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			n1 = new NVertex_Root(1,true, false, 0, Fi, 1);
			
			double a = 0;
			switch(Fi) 
			{
				case 0: a=3;break;
				case 1: a=0.9525741268224331;break;
				case 2: a=0.9486832980505138;break;
				case 3: a=9;break;
				case 4: a=3.048587351573742;break;
				case 5: a=3;break;
				case 6: a=1.2340980408667962E-4;break;
				case 7: a=0.9486832980505138; break;//Default function: tanh
				default: a=0;
			}
			I[0] = 3;	
			SuccessCounter += tester.testNode_ScalarActivation(n1, I, a);
			TestCounter++;
			Console.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
		}
		
		Console.println("###########################");
		Console.println("|| ACTIVATION END RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################");
		
		ResultConsole.println("###########################");
		ResultConsole.println("|| ACTIVATION END RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
		
		int[] result = {SuccessCounter, TestCounter};
		return result;	
	}
	

	//TODO: Put implementation innto format tester!...
	public void testCoreTypes() 
	{
		int SuccessCounter = 0;
		int TestCounter = 0;
		
		NVertex n 		= new NVertex_Root(1,false, false, 0, 0, 1);
		NVertex superUnit = new NVertex_Root(1,false, false, 0, 0, 1);
		
		printPropertiesOf(n);
		
		Console.println("Turn into basic core node:");

		NVertex.State.turnIntoBasicChild(n, superUnit);
		
		printPropertiesOf(n);
		TestCounter++;
		if(n.is(NVertex.BasicRoot)) 
		{
			Console.println("-> (unit.is(NVertex.BasicRoot)==true) -> test successful");
			SuccessCounter++;
		}
		else
		{
			Console.println("-> (unit.is(NVertex.BasicRoot)==False)! :( test failed");
			
		}
		TestCounter++;
		if(n.is(NVertex.Child)) 
		{
			Console.println("-> (unit.is(NVertex.Child)==true) -> test successful");
			SuccessCounter++;
		}
		else
		{
			Console.println("-> (unit.is(NVertex.Child)==false)! :( test failed");
			
		}
		
		//n.turnIntoSuperRootNode();
		NVertex.State.turnIntoParentRoot(n);
		printPropertiesOf(n);
		TestCounter++;
		if(n.is(NVertex.MotherRoot)) 
		{
			Console.println("-> (unit.is(NVertex.MotherRoot)==true) -> test successful");
			SuccessCounter++;
		}
		else
		{
			Console.println("-> (unit.is(NVertex.MotherRoot)==false)! :( test failed");
		}
	    Console.println("###########################");
	    Console.println("|| TYPE CONFIG TEST RESULT:");
		Console.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		Console.println("###########################");
		ResultConsole.println("###########################");
		ResultConsole.println("|| TYPE CONFIG TEST RESULT:");
		ResultConsole.println("|| ============>> "+SuccessCounter+"/"+TestCounter);
		ResultConsole.println("###########################");
	}
	
	
	public void printPropertiesOf(NVertex node) 
	{
		Console.println("\nNeuron diagnosis:");
		Console.println("==================");
		Console.println(node.toString());
		Console.println("==================");
	}
	
	
	
	
	
	
	
	
}
