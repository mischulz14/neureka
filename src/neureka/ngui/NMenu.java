package neureka.ngui;

import java.awt.Graphics2D;
import java.util.ArrayList;

public class NMenu implements NPanelObject
{

	//Icon painter (lambda!)
	
	private double X;
	private double Y;
	
	
	private double outerX;
	private double outerY;
	
	
	
	
	@Override
	public boolean killable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasGripAt(double x, double y, NPanel_I HostPanel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<NPanelRepaintSpace> moveCircular(double[] data, NPanel_I Surface) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<NPanelRepaintSpace> moveDirectional(double[] data, NPanel_I Surface) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<NPanelRepaintSpace> moveTo(double[] data, NPanel_I Surface) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<NPanelRepaintSpace> updateOn(NPanel_I HostPanel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void movementAt(double x, double y, NPanel_I HostPanel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean clickedAt(double x, double y, NPanel_I HostPanel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean doubleClickedAt(double x, double y, NPanel_I HostPanel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getY() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRadius() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLeftPeripheral() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTopPeripheral() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRightPeripheral() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getBottomPeripheral() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NPanelRepaintSpace getRepaintSpace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean needsRepaintOnLayer(int layerID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void repaintLayer(int layerID, Graphics2D brush, NPanel_I HostSurface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getLayerID() {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
