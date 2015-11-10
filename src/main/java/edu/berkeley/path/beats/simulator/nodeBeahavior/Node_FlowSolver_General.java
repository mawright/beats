package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matt on 7/1/15.
 * The general node model described in Section 3.5 of Wright, Gomes, Horowitz and Kurzhanskiy,
 * "A new model for multi-commodity macroscopic modeling of complex traffic networks"
 */
public class Node_FlowSolver_General extends Node_FlowSolver implements Serializable {

	private static final long serialVersionUID = 3225338640367388312L;
	// used in update()
	protected boolean [][] iscontributor;	// [nIn][nOut]

	protected double [][] demands; // [nIn][nVType]
	protected double [] supplies; // [nOut]
	protected double [][][] directed_demands; // [nIn][nOut][nVType]
	protected double [][][] original_directed_demands; // [nIn][nOut][nVType]
	protected double [] priorities; // [nIn]
	protected double [][] oriented_priorities; // [nIn][nOut]
	protected double [] reduction_factors; // [nOut]
	protected int min_reduction_index;
	protected ArrayList<Integer> freeflow_inlinks; // a list of ints that represent the inlink indeces that are in FF, if any
	protected HashMap< int[], Double > flows; // crude way to implement a tuple of a (i,j,c) index set and flow value [per-commodity, summed over commodity]
	protected IOFlow ioFlow;

	private RestrictionCoefficients restrictionCoefficients;

	private int c_max; // num of VTypes

	private boolean [] outlink_done; // [nOut], entry is true if outlink has all its inflows computed, false otherwise
	private boolean [] inlink_done; // [nIn], entry is true if inlink has all its outflows computed, false otherwise

	// constructor
	public Node_FlowSolver_General(Node myNode){
		super(myNode);
		restrictionCoefficients = new RestrictionCoefficients();
		restrictionCoefficients.populate(myNode, myNode.getRestrictionCoefficients());

		restrictionCoefficients.validate();
	}

	// implementation

	@Override
	public void reset() {
		iscontributor = new boolean[myNode.nIn][myNode.nOut];

		directed_demands = new double[myNode.nIn][myNode.nOut][myNode.getMyNetwork().getMyScenario().get.numVehicleTypes()];
		original_directed_demands = new double[myNode.nIn][myNode.nOut][myNode.getMyNetwork().getMyScenario().get.numVehicleTypes()];
		oriented_priorities = new double[myNode.nIn][myNode.nOut];
		reduction_factors = new double[myNode.nOut];
		c_max = myNode.getMyNetwork().getMyScenario().get.numVehicleTypes();
		freeflow_inlinks = new ArrayList<Integer>(myNode.nIn);
		flows = new HashMap<int[], Double>();
	}

	@Override
	public IOFlow computeLinkFlows(final Double [][][] sr, final int ensemble_index){

		int i,j,c; // input, output, commodity indices
		priorities = myNode.getInputLinkPriorities(ensemble_index);
		supplies = myNode.node_behavior.getAvailableSupply(ensemble_index);
		demands = myNode.node_behavior.getDemand(ensemble_index);

		// initialize directed demands
		for(i=0;i<sr.length;i++){
			for(j=0;j<sr[i].length;j++){
				for(c=0;c<sr[i][j].length;c++){
					directed_demands[i][j][c] = demands[i][c] * sr[i][j][c];
				}
				original_directed_demands[i][j] = directed_demands[i][j].clone();
			}
		}

		outlink_done = new boolean[myNode.getnOut()];
		inlink_done = new boolean[myNode.getnIn()];

		ioFlow = new IOFlow(myNode.getnIn(),myNode.getnOut(),c_max);

		determineUnsolvedMovements();

		while(!allFlowsSolved()){
			updatePriorities();
			computeOrientedPriorities();
			computeReductionFactors();
			determineFreeflowInlinks();
			pickFlows();
			setFlowsUpdateSuppliesDemands();
			determineUnsolvedMovements();
		}

		return ioFlow;
	}

	private void determineUnsolvedMovements() { // determine membership of set V(k)
		int i,j;
		// input i contributes to output j .............................
		for(j=0;j<myNode.getnOut();j++)
			for (i = 0; i < myNode.getnIn(); i++)
				iscontributor[i][j] = BeatsMath.sum(directed_demands[i][j]) > 0;

		outerloop1:
		for(j=0;j<myNode.getnOut();j++){
			for(i=0;i<myNode.getnIn();i++) {
				if(iscontributor[i][j]){
					outlink_done[j] = false;
					continue outerloop1;
				}
			}
			outlink_done[j] = true;
		}
		outerloop2:
		for(i=0;i<myNode.getnIn();i++) {
			for(j=0;j<myNode.getnOut();j++) {
				if(iscontributor[i][j]){
					inlink_done[i] = false;
					continue outerloop2;
				}
			}
			inlink_done[i] = true;
		}
	}

	private void updatePriorities() { // equation (5.12)
		int i,j;
		for(j=0;j<myNode.getnOut();j++) {
			if(!outlink_done[j]) {
				for(i=0;i<myNode.getnIn();i++) {
					if(iscontributor[i][j] && !inlink_done[i] && priorities[i] > 0)
						return; // if there are any unprocessed in-links with nonzero priority, do not adjust
				}
			}
		}
		int numUnsetInlinks = BeatsMath.count(inlink_done);
		for(i=0;i<myNode.getnIn();i++) {
			if(!inlink_done[i])
				priorities[i] = 1/numUnsetInlinks; // otherwise
		}
	}

	private void computeOrientedPriorities() { // equation (5.12)
		int i,j;
		double[] sum_over_c = new double[myNode.getnOut()];
		double sum_over_c_and_j;
		for(i=0;i<directed_demands.length;i++){
			for(j=0;j<directed_demands[i].length;j++) {
				sum_over_c[j] = BeatsMath.sum(directed_demands[i][j]);
			}
			sum_over_c_and_j = BeatsMath.sum(sum_over_c);
			for(j=0;j<directed_demands[i].length;j++) {
				oriented_priorities[i][j] = priorities[i] * sum_over_c[j] / sum_over_c_and_j;
			}
		}
	}

	private void computeReductionFactors() { // compute factors a_j
		int i,j;
		double sum_over_i;
		min_reduction_index = 0;
		for(j=0;j<myNode.nOut;j++){
			sum_over_i = 0d;
			for(i=0;i<myNode.nIn;i++){
				if(iscontributor[i][j])
					sum_over_i += oriented_priorities[i][j];
			}
			reduction_factors[j] = supplies[j] / sum_over_i;
			if(reduction_factors[j] <= reduction_factors[min_reduction_index])
				min_reduction_index = j;
		}
	}

	private void determineFreeflowInlinks() { // find members of set U-tilde(k)
		int i;
		for(i=0;i<myNode.nIn;i++){
			if(iscontributor[i][min_reduction_index] &&
					BeatsMath.sum(demands[i]) <= priorities[i] * reduction_factors[min_reduction_index] )
				freeflow_inlinks.add(i);
		}
	}

	private void pickFlows() { // set to IOFlow flows found in this iteration
		if(!freeflow_inlinks.isEmpty()){
			for(int i : freeflow_inlinks){
				for(int j=0;j<myNode.nOut;j++){
					for(int c=0;c<c_max;c++){
						int[] indexTriple = new int[3];
						indexTriple[0] = i; indexTriple[1] = j; indexTriple[2] = c;
						double flow = directed_demands[i][j][c];
						flows.put( indexTriple, flow );
						directed_demands[i][j][c] = 0;
					}
				}
			}
		}
		else { // no links whose demands whose can be met by their claimed supply
			for(int i=0;i<myNode.nIn;i++) {
				if(iscontributor[i][min_reduction_index]) { // for each i in U_j*(k)

					// solve for flows to j* first (they are needed for demand reduction in 3.41)
					double demand_sum_over_c = BeatsMath.sum(directed_demands[i][min_reduction_index]);
					double flow_i_jstar_sum_over_c =0d;
					double original_demand_i_jstar_sum_over_c =
							BeatsMath.sum(original_directed_demands[i][min_reduction_index]);

					for(int c=0;c<c_max;c++) {
						double flow = directed_demands[i][min_reduction_index][c]
								* oriented_priorities[i][min_reduction_index] * reduction_factors[min_reduction_index]
								/ demand_sum_over_c;
						int[] indexTriple = new int[3];
						indexTriple[0] = i;
						indexTriple[1] = min_reduction_index;
						indexTriple[2] = c;
						flows.put(indexTriple,flow);
						flow_i_jstar_sum_over_c += flow;
						directed_demands[i][min_reduction_index][c] = 0d;
					}

					for(int j=0;j<myNode.nOut;j++) {
						if (j==min_reduction_index)
							continue; // we already did j* above

						double restrictCoef = restrictionCoefficients.getCoefficient(myNode.getInput_link()[i],
								myNode.getOutput_link()[min_reduction_index], myNode.getOutput_link()[j]);
						demand_sum_over_c = BeatsMath.sum(directed_demands[i][j]);
						double original_demand_sum_over_c = BeatsMath.sum(original_directed_demands[i][j]);
						for(int c=0;c<c_max;c++) {
							if(BeatsMath.equals(restrictCoef,1d)) { // strict FIFO
								int[] indexTriple = new int[3];
								indexTriple[0] = i;
								indexTriple[1] = j;
								indexTriple[2] = c;

								double flow;
								if(demand_sum_over_c<=0d)
									flow = 0;
								else
									flow = directed_demands[i][j][c] * oriented_priorities[i][j]
											* reduction_factors[min_reduction_index] / demand_sum_over_c; // equation (3.39)

								flows.put(indexTriple, flow);
								directed_demands[i][j][c] = 0;
							} else { // relaxed FIFO - degrade other directed demands
								double degraded_demand_sum_over_c = Math.min(demand_sum_over_c,
										(1 - restrictCoef) * original_demand_sum_over_c
										+ restrictCoef * flow_i_jstar_sum_over_c / original_demand_i_jstar_sum_over_c
										* original_demand_sum_over_c); // equation (3.41)

								directed_demands[i][j][c] = directed_demands[i][j][c]
										* degraded_demand_sum_over_c / demand_sum_over_c; // equation (3.40)
							}
						}
					}
				}
			}
		}
	}

	private void setFlowsUpdateSuppliesDemands() { // update IOFlow, demand, supplies
		int i, j, c;
		for(Map.Entry<int[],Double> pair : flows.entrySet() ){
			int[] indexTriple = pair.getKey();
			// set flows to the IOFlow object
			i = indexTriple[0];
			j = indexTriple[1];
			c = indexTriple[2];
			ioFlow.setOut(j, c, ioFlow.getOut(j)[c] + pair.getValue());
			ioFlow.setIn(i, c, ioFlow.getIn(i)[c] + pair.getValue());

			// update supplies
			supplies[j] -= pair.getValue();
		}

		// update (undirected) demands
		for (i = 0; i < myNode.getnIn(); i++) {
			for (c = 0; c < c_max; c++) {
				demands[i][c] = 0;
				for (j = 0; j < myNode.getnOut(); j++) {
					demands[i][c] += directed_demands[i][j][c];
				}
			}
		}

		// clear sets
		flows.clear();
		freeflow_inlinks.clear();
	}

	private boolean allFlowsSolved() {
		for (boolean outlink_computed : outlink_done){
			if(!outlink_computed)
				return false;
		}
		return true;
	}

	public RestrictionCoefficients getRestrictionCoefficients() {
		return restrictionCoefficients;
	}
}
