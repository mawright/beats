package edu.berkeley.path.beats.simulator;

import java.util.ArrayList;

import edu.berkeley.path.beats.jaxb.ActuatorType;
import edu.berkeley.path.beats.jaxb.Signal;

public class ActuatorSet extends edu.berkeley.path.beats.jaxb.ActuatorSet {

    Scenario myScenario;
	private ArrayList<Actuator> actuators = new ArrayList<Actuator>();

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	protected ArrayList<Actuator> getActuators() {
		return actuators;
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {

        this.myScenario = myScenario;

		// replace jaxb.Actuator with simulator.Actuator
		if(myScenario.getActuatorSet()!=null){
			for(edu.berkeley.path.beats.jaxb.Actuator jaxba : myScenario.getActuatorSet().getActuator()) {

				// assign type
				Actuator.Type myType = null;
		    	try {
		    		ActuatorType atype = jaxba.getActuatorType();
		    		if(atype!=null)
		    			myType = Actuator.Type.valueOf(atype.getName());
				} catch (IllegalArgumentException e) {
					continue;
				}
                // generate actuator
				if(myType!=null){
                    Actuator A = ObjectFactory.createActuatorFromJaxb(myScenario,jaxba,myType);
					if(A!=null)
						actuators.add(A);
				}		    	
			}
		}

        // construct actuators from signals
        if(myScenario.getSignalSet()!=null){
            for(Signal jaxba : myScenario.getSignalSet().getSignal()){
                Actuator A = ObjectFactory.createActuatorSignalFromJaxb(myScenario,jaxba);
                if(A!=null)
                    actuators.add(A);
            }
        }

	}

	protected void validate() {
		for(Actuator actuator : actuators)
			actuator.validate();
	}
	
	protected void reset() throws BeatsException {
		for(Actuator actuator : actuators)
			actuator.reset();
	}

    protected void deploy(double current_time_in_seconds) throws BeatsException {
        for(Actuator actuator : actuators){
            if(actuator.myController==null)
                continue;
            if(!actuator.isOn)
                continue;
//            if(myScenario.getClock().is_time_to_sample_abs(actuator.myController.getSamplesteps(),0))
                actuator.deploy(current_time_in_seconds);
        }
	}
}
