package edu.berkeley.path.beats.simulator;

public abstract class Node_FlowSolver {

	protected Node myNode;
	
    protected abstract IOFlow computeLinkFlows(final Double3DMatrix sr,final SupplyDemand demand_supply,final int ensemble_index);

    protected abstract void reset();
    
	public Node_FlowSolver(Node myNode) {
		super();
		this.myNode = myNode;
	}

	protected static class SupplyDemand {
		// input to node model, copied from link suppy/demand
		protected double [][] demand;		// [nIn][nTypes]
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
	
	protected static class IOFlow {
		// input to node model, copied from link suppy/demand
		protected double [][] in;		// [nIn][nTypes]
		protected double [][] out;	// [ensemble][nOut][nTypes]
		
		public IOFlow(int nIn,int nOut,int numVehicleTypes) {
			super();
	    	in = new double[nIn][numVehicleTypes];
			out = new double[nOut][numVehicleTypes];
		}

		public void setIn(int nI,int nV,double val){
			in[nI][nV] = val;
		}
		
		public void setOut(int nO,int nV,double val){
			out[nO][nV]=val;
		}
		
		public double [] getIn(int nI){
			return in[nI];
		}

		public double getIn(int nI,int nV){
			return in[nI][nV];
		}
		
		public double [] getOut(int nO){
			return out[nO];
		}
		
		public void addOut(int nO,int nV,double val){
			out[nO][nV] += val;
		}
		
	}
}
