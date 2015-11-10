package edu.berkeley.path.beats.simulator.nodeBeahavior;

import java.io.Serializable;
import java.util.Set;

import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.util.ArraySet;
import org.apache.log4j.Logger;

/**
 * The node model proposed in
 * C.M.J. Tampere et al.
 * A generic class of first order node models
 * for dynamic macroscopic simulation of traffic flows.
 * Transportation Research Part B 45 (2011) 289-309
 *
 * The algorithm is improved to handle zero priorities of incoming links
 * if for each outgoing link there is at most one zero-priority incoming link
 * with strictly positive directed demand.
 */
public class Node_FlowSolver_Symmetric extends Node_FlowSolver implements Serializable {

	private static final long serialVersionUID = -4490898681235593337L;
	/** directed demands */
	private double [][] directed_demand; // [nIn][nOut] S_{ij}
	/** incoming link priorities */
	double [] priority_i; // [nIn] C_i
	/** directed flows */
	private double [][] flow; // [nIn][nOut] q_{ij}

	NodeModel model;

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

	public Node_FlowSolver_Symmetric(Node myNode) {
		super(myNode);
	}
	
	/////////////////////////////////////////////////////////////////////
	// implementation
	/////////////////////////////////////////////////////////////////////
	
	@Override
    public void reset() {
		int nIn = myNode.nIn;
		int nOut = myNode.nOut;
		directed_demand = new double[nIn][nOut];
		priority_i = new double[nIn];
		flow = new double[nIn][nOut];
		model = new NodeModel(nIn, nOut);
	}

	@Override
    public IOFlow computeLinkFlows(final Double [][][] splitratio,final int ensemble_index){

		int nIn = myNode.nIn;
		int nOut = myNode.nOut; 
    	int numVehicleTypes = myNode.getMyNetwork().getMyScenario().get.numVehicleTypes();
		IOFlow ioflow = new IOFlow(nIn,nOut,numVehicleTypes);

        double [][] demand = myNode.node_behavior.getDemand(ensemble_index);
        double [] supply = myNode.node_behavior.getAvailableSupply(ensemble_index);

        // priority_i and demand
        for (int i = 0; i < nIn; ++i) {
            priority_i[i] = myNode.input_link[i].getPriority(ensemble_index);
            for (int j = 0; j < nOut; ++j) {
                directed_demand[i][j] = 0;
                for (int vt = 0; vt < numVehicleTypes; ++vt) {
                    if (1 < nOut) {
                        // S_{ij} = \sum_{vt} S_i^{vt} * sr_{ij}^{vt}
                        double sr = splitratio[i][j][vt];
                        if (!Double.isNaN(sr))
                            directed_demand[i][j] += demand[i][vt] * sr;
                    } else
                        directed_demand[i][j] += demand[i][vt];
                }
            }
        }

        // compute flow from demand, supply and priorities
        model.solve(directed_demand, supply, priority_i, flow);

        // set outFlow to 0
        for (int j = 0; j < nOut; ++j)
            for (int vt = 0; vt < numVehicleTypes; ++vt)
                ioflow.setOut(j,vt,0d);

        for (int i = 0; i < nIn; ++i) {
            // S_i = \sum_j S_{ij}
            double demand_i = 0;
            for (int j = 0; j < nOut; ++j)
                demand_i += directed_demand[i][j];

            if (0 >= demand_i) {
                for (int vt = 0; vt <numVehicleTypes; ++vt)
                    ioflow.setIn(i,vt,0d);
            } else {
                // q_i = \sum_j q_{ij}
                double flow_i = 0;
                for (int j = 0; j < nOut; ++j)
                    flow_i += flow[i][j];
                final double reduction = flow_i / demand_i;
                for (int vt = 0; vt < numVehicleTypes; ++vt) {
                    ioflow.setIn(i,vt,demand[i][vt]*reduction  );
                    for (int j = 0; j < nOut; ++j){
                        ioflow.addOut(j,vt, ioflow.getIn(i,vt) * splitratio[i][j][vt] );
                    }
                }
            }
        }
		return ioflow;
	}

	public static class NodeModel implements Serializable {
		private static final long serialVersionUID = 3659546083729372428L;
		private int nIn;
		private int nOut;

		double [][] priority; // [nIn][nOut] C_{ij} directed priorities
		double [] demand_i; // [nIn] S_i incoming link demands
		private double [] supply_residual; // [nOut] \tilde R_j(k)  outgoing link supply residuals
		private Set<Integer> j_set; // [nOut] J(k)
		private Set<Integer> [] uj_set; // [nOut][nIn] U_j(k)
		private double [] a_coef; // [nOut] a_j(k)

		public NodeModel(int nIn, int nOut) {
			this.nIn = nIn;
			this.nOut = nOut;

			priority = new double[nIn][nOut];
			demand_i = new double[nIn];
			supply_residual = new double[nOut];
			j_set = new ArraySet(nOut);
			uj_set = new ArraySet[nOut];
			for (int oind = 0; oind < nOut; ++oind) uj_set[oind] = new ArraySet(nIn);
			a_coef = new double[nOut];
		}

        private static Logger logger = Logger.getLogger(NodeModel.class);

        /**
		 * Solve the node model
		 * @param demand directed demand
		 * @param supply outgoing links' supply
		 * @param priority_i incoming links' priorities
		 * @param flow an array to store the resulting flow
		 */
		public void solve(double [][] demand, double [] supply, double [] priority_i, double [][] flow) {
			
			for (int i = 0; i < nIn; ++i) {
				for (int j = 0; j < nOut; ++j)
					flow[i][j] = 0;

				// S_i = \sum_j S_{ij}
				demand_i[i] = 0;
				for (int j = 0; j < nOut; ++j)
					demand_i[i] += demand[i][j];

				if (nOut == 1) 
					priority[i][0] = priority_i[i];
				else
					for (int j = 0; j < nOut; ++j)
						// C_{ij} = C_i * (S_{ij} / S_i)
						priority[i][j] = 0 >= demand_i[i] ? priority_i[i] / nOut :
							priority_i[i] * demand[i][j] / demand_i[i];
			}

            // initialization
            j_set.clear();
            for (int j = 0; j < nOut; ++j) {
                uj_set[j].clear();
                // \tilde R_j(0) = R_j
                supply_residual[j] = supply[j];
                double demand_j = 0; // S_j
                int zero_priority_positive_demand_count = 0;
                for (int i = 0; i < nIn; ++i)
                    if (0 < priority_i[i]) {
                        // S_j = \sum_{i: p_i > 0} S_{ij}
                        demand_j += demand[i][j];
                        // U_j(0) = {i: p_i > 0, S_{ij} > 0}
                        if (demand[i][j] > 0) uj_set[j].add(i);
                    } else if (0 < demand[i][j])
                        ++zero_priority_positive_demand_count;
                if (1 < zero_priority_positive_demand_count)
                    logger.warn("Outgoing link #" + (j + 1) + " has " + //
                            zero_priority_positive_demand_count + " incoming links with zero priority and positive directed demand. " + //
                            "The flows for those incoming links may be incorrect");
                // J(0) = {j: S_j > 0}
                if (demand_j > 0) j_set.add(j);
            }
			
			// main loop
			while (!j_set.isEmpty()) {
				int min_a_ind = -1; // \hat j
				double min_a_val = Double.MAX_VALUE; // a_{\hat j}(k)
				for (int j = 0; j < nOut; ++j) // j \in J(k)
					if (j_set.contains(j)) {
						double sum_priority = 0; // \sum_{i \in U_j(k)} C_{ij}
						for (int i = 0; i < nIn; ++i)
							if (uj_set[j].contains(i))
								sum_priority += priority[i][j];
						// a_j(k) = \tilde R_j(k) / \sum_{i \in U_j(k)} C_{ij}
						a_coef[j] = supply_residual[j] / sum_priority;
						if (a_coef[j] < min_a_val) {
							min_a_val = a_coef[j];
							min_a_ind = j;
						}
					}
				
				if (-1 == min_a_ind)
					// TODO revise the exception type
					throw new RuntimeException("Internal node model error: min a_j is undefined");
				
				boolean demand_constrained = false;
				for (int i = 0; i < nIn; ++i)
					// i \in U_{\hat j}(k)
					// S_i <= a_{\hat k}(k) C_i
					if (uj_set[min_a_ind].contains(i) &&
							demand_i[i] <= min_a_val * priority_i[i]) {
						demand_constrained = true;
						for (int j = 0; j < nOut; ++j) {
							// q_{ij} = S_{ij} for all j
							flow[i][j] = demand[i][j];
							// for all j \in J(k)
							if (j_set.contains(j)) {
								// \tilde R_j(k + 1) = \tilde R_j(k) - S_{ij}
								supply_residual[j] -= demand[i][j];
								// U_j(k + 1) = U_j(k) \ {i}
								uj_set[j].remove(i);
							}
						}
					}
				
				if (demand_constrained) {
					for (int j = 0; j < nOut; ++j)
						if (j_set.contains(j) && uj_set[j].isEmpty())
							j_set.remove(j);
				} else {
					for (int i = 0; i < nIn; ++i)
						// for all i \in U_{\hat j}(k)
						if (uj_set[min_a_ind].contains(i)) {
							for (int j = 0; j < nOut; ++j) {
								// q_{ij} = a_{\hat j}(k) C_{ij} for all j
								flow[i][j] = min_a_val * priority[i][j];
								// for all j \in J(k)
								if (j_set.contains(j)) {
									// \tilde R_j(k + 1) = \tilde R_j(k) - a_{\hat j}(k) C_{ok}
									supply_residual[j] -= flow[i][j];
									// if j != \hat j(k)
									if (j != min_a_ind)
										// U_j(k + 1) = U_j(k) \ U_{\hat j}(k)
										uj_set[j].remove(i);
								}
							}
						}
					
					for (int j = 0; j < nOut; ++j)
						if (j != min_a_ind && j_set.contains(j) && uj_set[j].isEmpty())
							j_set.remove(j);
					
					j_set.remove(min_a_ind);
				}
            }

            // compute flows for zero-priority incoming links with positive demand
            for (int i = 0; i < nIn; ++i)
                if (0 == priority_i[i] && 0 < demand_i[i]) {
                    // diverge model
                    double flow_i = demand_i[i];
                    for (int j = 0; j < nOut; ++j)
                        if (0 < demand[i][j]) {
                            double split_ratio_ij = demand[i][j] / demand_i[i];
                            flow_i = Math.min(flow_i, supply_residual[j] / split_ratio_ij);
                        }
                    for (int j = 0; j < nOut; ++j)
                        if (0 < demand[i][j]) {
                            flow[i][j] = flow_i * demand[i][j] / demand_i[i];
                            supply_residual[j] -= flow[i][j];
                        }
                }


		}
	}

}