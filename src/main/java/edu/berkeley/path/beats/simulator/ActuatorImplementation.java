package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.actuator.ActuatorSignal;
import edu.berkeley.path.beats.actuator.NEMA;
import edu.berkeley.path.beats.jaxb.Splitratio;
import edu.berkeley.path.beats.jaxb.SwitchRatio;

import java.util.List;

public abstract class ActuatorImplementation {

    protected Actuator myActuator;
    protected Object target;      // Link or Node or HashMap<ActuatorSignal.NEMA,List<Link>>

    public Object get_target(){ return target; };
    
    public void setActuator(Actuator myActuator){
        this.myActuator = myActuator;
    }

    public void deploy_metering_rate_in_veh(Double metering_rate_in_veh){};
	public void deploy_metering_rate_in_vph(Double metering_rate_in_vph){};
//	public void deploy_stage_splits(StageSplit[] stage_splits){};
	public void deploy_cms_split(List<Splitratio> splits){};
	public void deploy_vsl_speed(){};
    public void deploy_bulb_color(NEMA.ID nema,ActuatorSignal.BulbColor color){};
    public void deploy_switch_ratio(List<SwitchRatio> switchRatios){};
}
