package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.actuator.StageSplit;

public abstract class ActuatorImplementation {

    protected Actuator myActuator;
    protected Object target;      // Link or Signal

    public void setActuator(Actuator myActuator){
        this.myActuator = myActuator;
    }

    public void deploy_metering_rate_in_veh(Double metering_rate_in_veh){};
	public void deploy_metering_rate_in_vph(Double metering_rate_in_vph){};
	public void deploy_stage_splits(StageSplit[] stage_splits){};
	public void deploy_cms_split(){};
	public void deploy_vsl_speed(){};
}
