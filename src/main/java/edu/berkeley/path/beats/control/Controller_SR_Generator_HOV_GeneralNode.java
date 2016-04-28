package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.simulator.Clock;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.nodeBeahavior.Node_FlowSolver;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

/**
 * Created by matt on 2/1/16.
 */
public class Controller_SR_Generator_HOV_GeneralNode extends Controller_SR_Generator_new {

	protected boolean[] variable_vtype;

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_SR_Generator_HOV_GeneralNode(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(myScenario,c);
		variable_vtype = new boolean[myScenario.get.numVehicleTypes()];
		for(int v=0;v<myScenario.get.numVehicleTypes();v++) {
			if( myScenario.get.vehicleTypeNames()[v].compareToIgnoreCase("hov") == 0 ||
					myScenario.get.vehicleTypeNames()[v].compareToIgnoreCase("sov") == 0 ||
					myScenario.get.vehicleTypeNames()[v].compareToIgnoreCase("lov") == 0) {
				variable_vtype[v] = true;
			}
			else {
				variable_vtype[v] = false;
			}
		}
	}

	@Override
	protected NodeData createNodeData(Link link, String demandString, double knob, double dpdt, double start_time) {
		return new NodeData(this, link, demandString, knob, dpdt, start_time, myScenario);
	}

	class NodeData extends Controller_SR_Generator_new.NodeData {

		int offramp_out;
		private Double[][][] controller_split;

		public NodeData(Controller parent, Link profileLink, String demandStr, Double knob, Double dpdt, Double start_time, Scenario scenario) {
			super(parent, profileLink, demandStr, knob, dpdt, start_time, scenario);

			if(myNode.isHasManagedLaneBarrier()) {
				for (int i=0; i<myNode.nIn; i++) {
					if(myNode.getInput_link()[i].isManagedLane())
						is_feed.set(i,false);
				}
			}

			offramp_out = myNode.getOutputLinkIndex(profileLink.getId());
			controller_split = BeatsMath.nans(myNode.nIn, myNode.nOut, myScenario.get.numVehicleTypes());
  		}

  		@Override
  		public void update(Clock clock) {
			int i;
			int e = 0;

			double bHigh, bLow, bTest;
			double[][] initial_total_nonmeasured_split;
			Node_FlowSolver.IOFlow flow;

			// make node sample its split ratio
			myNode.sample_split_ratio_profile();
			Double[][][] splitratio = BeatsMath.copy(myNode.getSplitRatio());

			if(measured_flow_profile_veh.sample(false,clock))
				measured_flow_veh = measured_flow_profile_veh.getCurrentSample()*knob; // hat{f}_222^in

			if(BeatsMath.equals(measured_flow_veh, 0d)){
				beta = 0d;
				initial_total_nonmeasured_split = compute_initial_total_nonmeasured_split(splitratio);
				controller_split = complete_splitratio_matrix(splitratio, initial_total_nonmeasured_split, beta, e);
				return;
			}

			// find demands from GP and HOV lanes
			Double S[] = BeatsMath.zeros(myNode.nIn);
			for (i = 0; i < myNode.getnIn(); i++) {
				if (is_feed.get(i))
					S[i] = myNode.getInput_link()[i].get_total_out_demand_in_veh(e);
			}

			if( BeatsMath.sum(S) <= measured_flow_veh ) { // insufficient demand
				beta = 1d;
				initial_total_nonmeasured_split = compute_initial_total_nonmeasured_split(splitratio);
				controller_split = complete_splitratio_matrix(splitratio, initial_total_nonmeasured_split, beta, e);
				return;
			}

			bHigh = 1d;
			bLow = 0d;

			// begin bisection method
			Double[][][] splitratio_filled;

			initial_total_nonmeasured_split = compute_initial_total_nonmeasured_split(splitratio);

			bTest = (bLow + bHigh) / 2;
			boolean solved = false;
			do {
				splitratio_filled = complete_splitratio_matrix(splitratio, initial_total_nonmeasured_split, bTest, e);

				flow = myNode.node_behavior.flow_solver.computeLinkFlows(splitratio_filled, e);
				double difference = BeatsMath.sum(flow.getOut(offramp_out)) - measured_flow_veh;
				if (BeatsMath.equals( difference, 0d, measured_flow_veh*.01 )) {
					solved = true;
				}
				else if (BeatsMath.equals(bLow, bHigh, .001d)) {
					solved = true;
				}
				else if (difference < 0) {
					bLow = (bLow + bHigh) / 2;
					bTest = (bLow + bHigh) / 2;
				}
				else {
					bHigh = (bLow + bHigh) / 2;
					bTest = (bLow + bHigh) / 2;
				}
			} while (!solved);
			beta = bTest;
			controller_split = splitratio_filled;
		}

		private double[][] compute_initial_total_nonmeasured_split(Double[][][] splitratio) {
			int i,j,c;
			double[][] initial_total_nonmeasured_split = new double[myNode.nIn][myScenario.get.numVehicleTypes()];
			for (c = 0; c<myScenario.get.numVehicleTypes(); c++) {
				if (variable_vtype[c]) {
					for (i = 0; i < myNode.nIn; i++) {
						if( is_feed.get(i) ) {
							initial_total_nonmeasured_split[i][c] = 0d;
							for (j = 0; j < myNode.nOut; j++) {
								if ( !is_measured.get(j) ) {
									double temp = Double.isNaN(splitratio[i][j][c]) ? 0d : splitratio[i][j][c];
									initial_total_nonmeasured_split[i][c] += temp;
								}
							}
						}
					}
				}
			}
			return initial_total_nonmeasured_split;
		}

		private Double[][][] complete_splitratio_matrix(Double[][][] splitratio,
														double[][] initial_total_nonmeasured_split, double bTest,
														int ensemble_index) {
			int i,j,c;
			double newsplit;
			Double[][][] splitratio_filled = BeatsMath.copy(splitratio);

			double total_non_measured_split = 1d - bTest;
			for (c=0; c<myScenario.get.numVehicleTypes(); c++) {
				if (variable_vtype[c]) {
					for (i = 0; i < myNode.nIn; i++) {
						if (is_feed.get(i)) {
							for (j = 0; j < myNode.nOut; j++) {
								if (is_measured.get(j)) {
									splitratio_filled[i][j][c] = bTest; // beta we are searching for
								} else {
									newsplit = BeatsMath.equals(initial_total_nonmeasured_split[i][c], 0d) ? Double.NaN :
											(myNode.getSplitRatio(i, j, c) * total_non_measured_split)
													/ initial_total_nonmeasured_split[i][c];
									splitratio_filled[i][j][c] = newsplit;
								}
							}
						}
					}
				}
			}

			// invoke the SRsolver to fill in any undefined parts
			splitratio_filled = myNode.node_behavior.sr_solver.computeAppliedSplitRatio(splitratio_filled, ensemble_index);
			return splitratio_filled;

		}

		@Override
		public void deploy(double current_time_in_seconds){
			int i,j,c;

			for(i=0;i<myNode.nIn;i++) {
				Link inlink = myNode.input_link[i];
				if (is_feed.get(i)) {
					for (c = 0; c < myScenario.get.numVehicleTypes(); c++) {
						if (variable_vtype[c]) {
							for (j = 0; j < myNode.nOut; j++) {
								Link outlink = myNode.output_link[j];

								cms.set_split(inlink.getId(), outlink.getId(), myScenario.get.vehicleTypeIdForIndex(c),
										controller_split[i][j][c]); // split ratios differ across commodities
								}
							}
						}
					}
			}
			cms.deploy(current_time_in_seconds);
		}
	}

}