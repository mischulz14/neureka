package ngui;

import java.awt.Graphics2D;
import java.util.LinkedList;

public abstract class NMapAPI{

	static public int MAX = 8;
	
	public abstract NMapAPI addAll(LinkedList<NPanelObject> elements);
	
	public abstract NMapAPI addAndUpdate(NPanelObject node);
	
	public abstract NMapAPI removeAndUpdate(NPanelObject node);
	
	public abstract int getCount();
	
	public abstract NPanelObject get(int i);
	
	public abstract LinkedList<NPanelObject> getAll();
	public abstract LinkedList<NPanelObject> getAllWithin(double[] frame);
	public abstract LinkedList<NPanelObject> getAllClosestTo(double x, double y);
	
	public abstract LinkedList<NPanelObject> findAllAt(double x, double y, MapAction Actor);
	public abstract LinkedList<NPanelObject> findAllWithin(double[] frame, MapAction Actor);
	
	public abstract Object applyAndGet(AdvancedMapAction Actor);
	public abstract Object applyAndGet(Object Data, AdvancedMapAction Actor);
	public abstract void   apply	  (Object Data, AdvancedMapAction Actor);
		
	public abstract boolean applyToAll(MapAction Actor);
	public abstract boolean applyToAllWithin(double[] frame, MapAction Actor);
	
	public abstract void paintStructure(Graphics2D brush);

	interface MapAction         {abstract boolean act(NPanelObject thing);}	
	interface AdvancedMapAction {abstract Object  act(Object first, NPanelObject thing); }	



}
