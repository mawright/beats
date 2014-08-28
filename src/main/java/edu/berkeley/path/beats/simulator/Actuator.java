package edu.berkeley.path.beats.simulator;

public class Actuator extends edu.berkeley.path.beats.jaxb.Actuator {

    protected Controller myController;
    public enum Implementation {beats,aimsun};
    protected ActuatorImplementation implementor;
    protected Actuator.Type myType;
    protected boolean isOn;

	public static enum Type	{ ramp_meter,signal,vsl,cms };

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

	public Actuator (){
	}
	
	public Actuator (Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA,ActuatorImplementation act_implementor){
        this.isOn = false;
        this.implementor = act_implementor;
        this.myType = Actuator.Type.valueOf(jaxbA.getActuatorType().getName());

        // copy jaxb data
        setId(jaxbA.getId());
        setScenarioElement(new ScenarioElement(myScenario,jaxbA.getScenarioElement()));
        setParameters(jaxbA.getParameters());
        setActuatorType(jaxbA.getActuatorType());
        setTable(jaxbA.getTable());
	}

    public Actuator (ActuatorImplementation act_implementor,Actuator.Type myType){
        this.isOn = false;
        this.implementor = act_implementor;
        this.myType = myType;
    }

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset / deploy
	/////////////////////////////////////////////////////////////////////

    protected boolean register(){
        return false;
    }

	protected void populate(Object jaxbobject,Scenario myScenario) {
		return;
	}

	protected void validate() {
//		if(implementor.getLink()==null)
//			BeatsErrorLog.addError("Bad link reference in actuator ID="+getId());
	}

	protected void reset() throws BeatsException {
        this.isOn = true;
        return;
	}

	protected void deploy(double current_time_in_seconds){
    };

    protected void deploy_off_signal(){
        return;
    };

    public ActuatorImplementation getImplementor(){
        return implementor;
    }

    public Actuator.Type get_type(){
        return myType;
    }

    public void setIsOn(boolean xison){
        this.isOn = xison;
        if(!isOn)
            deploy_off_signal();
    }
}
