package edu.berkeley.path.beats.actuator;

import java.util.List;

import edu.berkeley.path.beats.simulator.Actuator;

public class ActuatorSignal extends Actuator {

    private List<Double> green_times;

	public void setGreenTimes(List<Double> green_times){
		this.green_times = green_times;
	}

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
	
//	public ActuatorSignal(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA){
//		super(myScenario,jaxbA);
//	}

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset / deploy
	/////////////////////////////////////////////////////////////////////

//	@Override
//	protected void populate(Object jaxbobject) {
//		return;
//	}
//
//	@Override
//	protected void validate() {
//	}
//
//	@Override
//	protected void reset() throws BeatsException {
//		return;
//	}
//
//	@Override
//	public void deploy() {
//		this.implementor.deploy_green_times(green_times);
//	}

}
