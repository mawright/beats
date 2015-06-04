package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.ActuatorImplementation;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 6/4/2015.
 */
public class ActuatorCommodity extends Actuator {

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public ActuatorCommodity(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA,ActuatorImplementation act_implementor){
        super(myScenario,jaxbA,act_implementor);
    }

    @Override
    public boolean register() {
        return ((Link)implementor.get_target()).register_density_controller();
    }

}
