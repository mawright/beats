package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.simulator.Clock;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.util.HashMap;

/**
 * Created by matt on 2/1/16.
 */
public class Controller_SR_Generator_GeneralNode extends Controller_SR_Generator_new {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_SR_Generator_GeneralNode(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(myScenario,c);
	}

	@Override
	protected NodeData createNodeData(Link link, String demandString, double knob, double dpdt, double start_time) {
		return new NodeData(this, link, demandString, knob, dpdt, start_time, myScenario);
	}

  class NodeData extends Controller_SR_Generator_new.NodeData {

	  public NodeData(Controller parent, Link profileLink, String demandStr, Double knob, Double dpdt, Double start_time, Scenario scenario) {
		  super(parent, profileLink, demandStr, knob, dpdt, start_time, scenario);
	  }

	  @Override
	  public void update(Clock clock) {
		  int i,j;
		  int e = 0;

		  if(measured_flow_profile_veh.sample(false,clock))
			  measured_flow_veh = measured_flow_profile_veh.getCurrentSample()*knob;

		  if(BeatsMath.equals(measured_flow_veh, 0d)){
			  beta = 0d;
		  }
	  }
  }

}