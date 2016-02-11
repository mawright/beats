package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

public abstract class Node_FlowSolver implements Serializable {

	private static final long serialVersionUID = -1405763028140546852L;

	public Node myNode;

    public abstract IOFlow computeLinkFlows(final Double [][][] sr,final int ensemble_index);

    public abstract void reset();
    
	public Node_FlowSolver(Node myNode) {
		super();
		this.myNode = myNode;
	}

	public static class SupplyDemand implements Serializable{
		private static final long serialVersionUID = 6326575979927951801L;
		// input to node model, copied from link suppy/demand
		protected double [][] demand;    // [nIn][nTypes]
		protected double [] supply;		// [nOut]
		
		public SupplyDemand(int nIn,int nOut,int numVehicleTypes) {
			super();
	    	demand = new double[nIn][numVehicleTypes];
			supply = new double[nOut];
		}
		
		public void setDemand(int nI,double [] val){
			demand[nI] = val;
		}
		
		public void setSupply(int nO, double val){
			supply[nO]=val;
		}
		
		public double getDemand(int nI,int nK){
			return demand[nI][nK];
		}
		
		public double getSupply(int nO){
			return supply[nO];
		}

		public double [] getSupply(){
			return supply;
		}
	}
	
	public static class IOFlow implements Serializable {
		private static final long serialVersionUID = -5377400410894173778L;
		// input to node model, copied from link suppy/demand
		protected Double [][] in;		// [nIn][nTypes]
		protected Double [][] out;	// [ensemble][nOut][nTypes]
		
		public IOFlow(int nIn,int nOut,int numVehicleTypes) {
			super();
	    	in = BeatsMath.zeros(nIn, numVehicleTypes);
			out = BeatsMath.zeros(nOut,numVehicleTypes);
		}

		public void setIn(int nI,int nV,double val){
			in[nI][nV] = val;
		}
		
		public void setOut(int nO,int nV,double val){
			out[nO][nV]=val;
		}
		
		public Double [] getIn(int nI){
			return in[nI];
		}

		public Double getIn(int nI,int nV){
			return in[nI][nV];
		}
		
		public Double [] getOut(int nO){
			return out[nO];
		}
		
		public void addOut(int nO,int nV,double val){
			out[nO][nV] += val;
		}
		
	}
}
