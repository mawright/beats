package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorRampMeter;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 5/25/2016.
 */
public class ControllerRampMeteringAPI extends Controller {

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public ControllerRampMeteringAPI(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.RMAPI);
//        this.dtinseconds = Double.NaN;
//        this.samplesteps = Integer.MAX_VALUE;
    }

    @Override
    protected void initialize_actuators() {
        super.initialize_actuators();
        for(Actuator act : this.actuators)
            ((ActuatorRampMeter) act).setMeteringRateInVeh(Double.POSITIVE_INFINITY);
    }

    public void set_metering_rate_in_vph(int act_id,double rate_in_vph){
        ActuatorRampMeter ramp_meter = (ActuatorRampMeter)getActuatorWithId(act_id);
        if(ramp_meter==null)
            return;
        ramp_meter.setMeteringRateInVPH(rate_in_vph);
    }

}
