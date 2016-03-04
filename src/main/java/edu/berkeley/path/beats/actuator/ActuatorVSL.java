package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;

import java.io.Serializable;

public class ActuatorVSL extends Actuator  implements Serializable {
    private static final long serialVersionUID = -7757068960469048806L;

    /////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

//	public ActuatorVSL(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA){
//		super(myScenario,jaxbA);
//	}

    @Override
    public boolean register() {
        return ((Link)implementor.get_target()).register_speed_controller();
    }
}
