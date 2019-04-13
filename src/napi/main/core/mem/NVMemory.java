package napi.main.core.mem;

import napi.main.core.comp.NVData;

import java.math.BigInteger;
import java.util.ArrayList;

public class NVMemory {

	//Histories:
	private ArrayList<NVData> DataHistory;
	private ArrayList<double[][]> dInputHistory;
	private ArrayList<double[][]> activationHistory;//Linked lists better!!!
	private ArrayList<byte[]>   activityHistory;
	private int TimeTunnel;//WIP
	/*How it works:
	 * 
	 * fetching memorized information will be modified by -TimeTunnel
	 * -> So if the network tries to fetch an activation from 15 steps ago and the Tunneling is set to 5
	 * the recieved values will be taken from index 10.
	 * -> 
	 * 
	 * */
	//========================================================================================
	public NVMemory(int tunneling)
	{
		TimeTunnel = tunneling;
		//dInputHistory     = new ArrayList<double[][]>();
		//activationHistory = new ArrayList<double[][]>();
		activityHistory   = new ArrayList<byte[]>();

		DataHistory = new ArrayList<NVData>();
	}
	
	public void removeHistoryOlderThan(BigInteger Hi) 
	{
		int activeTime = activeTimeDeltaWithin(Hi);
		//removeDeriviationsOlderThan(activeTime);
		//removeActivationsOlderThan(activeTime);
		removeActivityOlderThan(Hi);
	}

	//public void removeDeriviationsOlderThan(int Hi)
	//{
	//	ArrayList<double[][]> newMemorized = new ArrayList<double[][]>();
	//	for(int i=dInputHistory.size()-Hi; i<dInputHistory.size(); i++) {newMemorized.add(dInputHistory.get(i));}
	//	dInputHistory = newMemorized;
	//}
	//
	//public void removeActivationsOlderThan(int Hi)
	//{
	//	ArrayList<double[][]> newMemorized = new ArrayList<double[][]>();
	//	for(int i=activationHistory.size()-Hi; i<activationHistory.size(); i++) {newMemorized.add(activationHistory.get(i));}
	//	activationHistory = newMemorized;
	//}
	
	public void removeActivityOlderThan(BigInteger Hi)
	{
		if(activityHistory==null) {return;}
		if(activityHistory.size()==0) {return;}
		
		Hi = Hi.add(BigInteger.ONE);
		
		int historyCounter=0;
		while(true && historyCounter<activityHistory.size()) 
			{
				BigInteger big = new BigInteger(activityHistory.get(activityHistory.size()-1-historyCounter));
				if(big.signum()==-1) 
				{
					big = big.abs();
				}
				else 
				{
					big = big.add(BigInteger.ONE);
				}
				if(Hi.compareTo(big)==1) {Hi=Hi.subtract(big);historyCounter++;}
				else 
				{
					ArrayList<byte[]> newMemorized = new ArrayList<byte[]>();
					for(int i=activityHistory.size()-historyCounter-1; i<activityHistory.size(); i++) {newMemorized.add(activityHistory.get(i));}
					activityHistory = newMemorized;
					return;
				}
			}
		return;
	}

	public void removeDataOlderThan(int Hi)
	{
		ArrayList<NVData> newMemorized = new ArrayList<NVData>();
		for(int i=DataHistory.size()-Hi; i<DataHistory.size(); i++) {newMemorized.add(DataHistory.get(i));}
		DataHistory = newMemorized;
	}
	//========================================================================================
	//public boolean hasDeriviationMemory() {if(dInputHistory     != null) {return true;}return false;}
	//public boolean hasActivationMemory()  {if(activationHistory != null) {return true;}return false;}
	public boolean hasActivityMemory()    {if(activityHistory   != null) {return true;}return false;}
	public boolean hasDataMemory(){if(DataHistory!=null){return true;}return false;}
	//========================================================================================
	//public void memorizeDerivatives() {if(dInputHistory     == null) {dInputHistory = new ArrayList<double[][]>();}}
	//public void memorizeActivation()  {if(activationHistory == null) {activationHistory = new ArrayList<double[][]>();}}
	public void memorizeActivity()    {if(activityHistory   == null) {activityHistory = new ArrayList<byte[]>();}}
	public void memorizeData(){if(DataHistory==null){DataHistory = new ArrayList<NVData>();}}
	//========================================================================================
	//public void forgetDerivatives() {dInputHistory     = null;}
	//public void forgetActivation()  {activationHistory = null;}
	public void forgetActivity()    {activityHistory   = null;}
	public void forgetData(){DataHistory=null;}
	//========================================================================================
	public void resetMemory()
	{
		//if(hasDeriviationMemory()) {forgetDerivatives(); memorizeDerivatives();}
		//if(hasActivationMemory())  {forgetActivation();  memorizeActivation();}
		if(hasActivityMemory())    {forgetActivity();    memorizeActivity();}
		if(hasDataMemory()){forgetData(); memorizeData();}
	}
	//========================================================================================
	
	//Storage management:	
	//========================================================================================	
	synchronized public void storeCurrentActivity(boolean activity) 
	{
		if(!hasActivityMemory()) {return;}
		if(activityHistory.size()==0)
		{
			if(activity==true) {activityHistory.add(BigInteger.ZERO.toByteArray());}
			if(activity==false){activityHistory.add(new BigInteger("-1").toByteArray());}
			return;
		}
		BigInteger big = new BigInteger(activityHistory.get(activityHistory.size()-1));
		if(activity) 
		{
			if(big.signum()==-1 ) 
			{
				activityHistory.add(BigInteger.ZERO.toByteArray());
			}
			else
			{
				big = big.add(BigInteger.ONE);
				activityHistory.remove(activityHistory.size()-1);
				activityHistory.add(big.toByteArray());
			}
		}	
		else 
		{
			if(big.signum()==-1 ) 
			{
				big = big.subtract(BigInteger.ONE);
				activityHistory.remove(activityHistory.size()-1);
			    activityHistory.add(big.toByteArray());
			}
			else
			{
				activityHistory.add(new BigInteger("-1").toByteArray());
			}
		}
    }
	//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	synchronized public boolean getLatestActivity(){return getActivityAt(BigInteger.ZERO);}
	synchronized public boolean getActivityAt(BigInteger Hi)
	{
		if(activityHistory==null) {return false;}
		if(activityHistory.size()==0) {return false;}
		Hi=Hi.add(BigInteger.ONE);
	    boolean currentSign = true;
		int historyCounter=0;
		while(true && historyCounter<activityHistory.size()) 
		{
			BigInteger big = new BigInteger(activityHistory.get(activityHistory.size()-1-historyCounter));
			if(big.signum()==-1) 
			{
				currentSign=false;
				big = big.abs();
			}
			else 
			{
				currentSign=true; 
				big = big.add(BigInteger.ONE);
			}
			if(Hi.compareTo(big)==1) 
			{
				Hi=Hi.subtract(big);
				historyCounter++;
			}
			else 
			{
				return currentSign;
			}
		}
		return currentSign;
	}
	//================================================================================================================================================================================

	//========================================================================================
	synchronized public void storeCurrentInputDerivative(double[][] derivatives) //checking if sizes match up!
	{
		dInputHistory.add(derivatives);
	 	//System.out.println("Added InputDeriviation: "+derivatives[0]+" to storage!");
	}
	//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	synchronized public double[][] getdInputAt(int Hi) 
	{
		System.out.println("memory: getdInputAt("+Hi+");");
		if(Hi==0 || dInputHistory.size()==0) 
		{
			return null;
		} 
		return dInputHistory.get(dInputHistory.size()-Hi);
	}
	synchronized public double  getdInputAt(BigInteger Hi, int Vi, int Ii) 
	{
		int activeTime = activeTimeDeltaWithin(Hi);
		if(dInputHistory.size()==0 || activeTime==0) 
		{
			return 0;
		}
		else
		{
			double[][] derivatives = dInputHistory.get(dInputHistory.size()-activeTime); 
			return derivatives[Vi][Ii];
		}
	}
	//================================================================================================================================================================================	
	
	//========================================================================================
	synchronized public void storeCurrentActivation(double[][] activation) 
	{activationHistory.add(activation);}
	///::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	synchronized public double[][] getLatestActivation()   {return getActivationAt(0);}
	synchronized public double[][] getActivationAt(int Hi) 
	{
		System.out.println("memory: getActivationAt("+Hi+");");
		if(activationHistory.size()==0 || Hi==0) 
		{
			System.out.println("activation history size: "+activationHistory.size());
			return null;
		} 
	  	return activationHistory.get(activationHistory.size()-Hi);
	}
	//================================================================================================================================================================================

	//========================================================================================
	synchronized public void storeCurrentData(NVData data)
	{DataHistory.add(data);}
	///::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	synchronized public NVData getLatestData()   {return getDataAt(0);}
	synchronized public NVData getDataAt(int Hi)
	{
		System.out.println("memory: getDataAt("+Hi+");");
		if(DataHistory.size()==0 || Hi==0)
		{
			System.out.println("data history size: "+DataHistory.size());
			return null;
		}
		return DataHistory.get(DataHistory.size()-Hi);
	}
	//================================================================================================================================================================================

	//================================================================================================================================================================================
	synchronized public void setTimeTunnel(int tunnelingTime) {TimeTunnel = tunnelingTime;}
	synchronized public int  getTimeTunnel()          {return TimeTunnel;}
	//================================================================================================================================================================================
	public int activeTimeDeltaWithin(BigInteger Hi)
	{	
		if(activityHistory==null) 
		{
			return 0;
		}
		Hi = Hi.add(BigInteger.ONE);//why is that again?!?!?!?! ... whatever.... it works!
		BigInteger activeTime = BigInteger.ZERO;//Active time stays zero if the History index is larger than the stored history. (->returns 0)
		boolean condition = true;
		boolean currentSign = true;
		int historyCounter=0;
		while(condition && historyCounter < activityHistory.size()) 
		{
			BigInteger big = new BigInteger(activityHistory.get(activityHistory.size()-1-historyCounter));
			System.out.println("big:"+big.intValue());
			if(big.signum()==-1) 
			   {currentSign=false;}
			else 
			   {currentSign=true; big = big.add(BigInteger.ONE); System.out.println("wtf:"+big.intValue());activeTime.add(big);}
			
			if(currentSign==true) 
			   {
			       if(Hi.compareTo(big)==1) {Hi=Hi.subtract(big);}//THIS IS NOT RIGHT!!!
			       else 					{activeTime=activeTime.add(Hi);condition = false;}
			   }
			historyCounter++;
		}
		return activeTime.intValue();
		// |Past|------###O----####--|Present| => Result: 4 (has been inactive for 4 steps!)
	}
	//---------------------------------------------------------------------------------------------------------------------------------------------
	public int localInactiveTimeDeltaWithin(BigInteger Hi) 
	{
		if(activityHistory==null) {return 0;}
		if(activityHistory.size()==0) {return 0;}
		Hi = Hi.add(BigInteger.ONE);//why is that again?!?!?!?! ... whatever.... it works!
		BigInteger inactiveTime = BigInteger.ZERO;//Active time stays zero if the History index is larger than the stored history. (->returns 0)
		boolean condition = true;
		int historyCounter=0;
		while(condition && historyCounter < Hi.intValue() && (Hi.intValue()-1-historyCounter)<activityHistory.size()) 
		{
			BigInteger big = new BigInteger(activityHistory.get(Hi.intValue()-1-historyCounter));
			System.out.println("big:"+big.intValue());
			if(big.signum()==-1) 
			   {System.out.println("inactive counting:"+big.intValue()); inactiveTime.add(big.abs());}
			else 
			   {return inactiveTime.intValue();}

			historyCounter++;
		}
		return inactiveTime.intValue();
	}
	//---------------------------------------------------------------------------------------------------------------------------------------------
	public long currentActivityPhase() 
	{
		if(activityHistory==null) {return 0;}
		if(activityHistory.size()==0) {return 0;}
		BigInteger big = new BigInteger(activityHistory.get(activityHistory.size()-1));
		System.out.println("latest:"+big.intValue());
		if(big.signum()==-1) 
		{
			return big.intValue();
		}
		else 
		{
			return big.intValue()+1;
		}
	}
	//---------------------------------------------------------------------------------------------------------------------------------------------
	public long activityPhaseAt(BigInteger Hi)
	{
		if(activityHistory==null) {return 0;}
		if(activityHistory.size()==0) {return 0;}
		Hi = Hi.abs();
		for(int i=0; i<activityHistory.size(); i++){
			BigInteger phase = new BigInteger(activityHistory.get(i));
			BigInteger delta;
			if(phase.signum()==-1){
				delta = phase.abs().subtract(Hi);
				if(delta.signum()>=0)
				{
					return delta.negate().longValue();
				}
			}
			else
			{
				phase.add(BigInteger.ONE);
				delta = phase.subtract(Hi);
				if(delta.signum()>=0)
				{
					return delta.longValue();
				}
			}
			Hi.subtract(delta);
		}
		return 0;
	}
}
