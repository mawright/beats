package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.BeatsException;

public class ActuatorRampMeter extends Actuator {

	public void setRampMeteringRate(Double rate){
		this.command = rate;
	}

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset / update
	/////////////////////////////////////////////////////////////////////

	@Override
	protected void populate(Object jaxbobject) {
		return;
	}

	@Override
	protected void validate() {
	}

	@Override
	protected void reset() throws BeatsException {
		return;
	}

	@Override
	protected void update() throws BeatsException {
		return;
	}

	/////////////////////////////////////////////////////////////////////
	// deploy
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void delpoy(Object command) {
		this.implementor.deploy_metering_rate((Double) command);
	}

}
