package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.simulator.Clock;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.nodeBeahavior.Node_FlowSolver;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.util.HashMap;

/**
 * Created by matt on 2/1/16.
 */
public class Controller_SR_Generator_HOV_GeneralNode extends Controller_SR_Generator_new {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_SR_Generator_HOV_GeneralNode(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(myScenario,c);
	}

	@Override
	protected NodeData createNodeData(Link link, String demandString, double knob, double dpdt, double start_time) {
		return new NodeData(this, link, demandString, knob, dpdt, start_time, myScenario);
	}

	class NodeData extends Controller_SR_Generator_new.NodeData {

		int gp_in, hov_in, offramp_out;

		public NodeData(Controller parent, Link profileLink, String demandStr, Double knob, Double dpdt, Double start_time, Scenario scenario) {
			super(parent, profileLink, demandStr, knob, dpdt, start_time, scenario);

			for (int i = 0; i < is_feed.size(); i++) {
				if (is_feed.get(i) && myNode.getInput_link()[i].isHov())
					hov_in = i;
				else if (is_feed.get(i) && myNode.getInput_link()[i].isFreeway())
					gp_in = i;
			}
			offramp_out = myNode.getOutputLinkIndex(profileLink.getId());
  		}

  		@Override
  		public void update(Clock clock) {
			int i,j;
			int e = 0;

			double bHigh, bLow, bTest;
			Node_FlowSolver.IOFlow flow;

			Double[][][] splitratio = myNode.getSplitRatio().clone();

			if(measured_flow_profile_veh.sample(false,clock))
				measured_flow_veh = measured_flow_profile_veh.getCurrentSample()*knob; // hat{f}_222^in

			if(BeatsMath.equals(measured_flow_veh, 0d)){
				beta = 0d;
				return;
			}

			// find demands from GP and HOV lanes
			double S[] = new double[myNode.getnIn()];
			for (int k = 0; k < myNode.getnIn(); k++) {
				S[k] = myNode.getInput_link()[k].get_total_out_demand_in_veh(e);
			}

			if( BeatsMath.sum(S) <= measured_flow_veh ) { // insufficient demand
				beta = 1d;
				return;
			}

			// keep track of split ratio proportions to non-offramp links


			bLow = 0;
			bHigh = measured_flow_veh / BeatsMath.sum(S);

			// begin bisection method
			bTest = bLow;
			boolean solved = false;
			while (!solved) {
				for (int c=0; c<myScenario.get.numVehicleTypes(); c++)
				{
					splitratio[hov_in][offramp_out][c] = bTest;
					splitratio[gp_in][offramp_out][c] = bTest;
				}
				flow = myNode.node_behavior.flow_solver.computeLinkFlows(splitratio, e);
			}
		}
	}

}