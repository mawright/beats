package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Parameters;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

public class ActuatorRampMeter extends Actuator  implements Serializable {

    private static final long serialVersionUID = 2228283060869908816L;

    public enum QueueOverrideStrategy {none,max_rate,proportional,proportional_integral};
    private double metering_rate_in_veh;
    private Link myLink;
	private Double max_rate_in_veh = Double.POSITIVE_INFINITY;
	private Double min_rate_in_veh = 0d;
    private QueueOverride queue_override;

    private double cycle_increment;   // [sec]

    /////////////////////////////////////////////////////////////////////
    // actuation command
    /////////////////////////////////////////////////////////////////////

    public void setMeteringRateInVeh(Double rate_in_veh){
        metering_rate_in_veh = rate_in_veh;
        // round to the nearest time increment
        if(!Double.isNaN(cycle_increment) & cycle_increment>0){
            double dt = getMyController().getMyScenario().get.simdtinseconds();
            double sec_per_veh = dt/metering_rate_in_veh;
            double sec_per_veh_round = Math.round(sec_per_veh/cycle_increment)*cycle_increment;
            metering_rate_in_veh = dt/sec_per_veh_round;
        }
        metering_rate_in_veh = Math.max(metering_rate_in_veh,min_rate_in_veh);
        metering_rate_in_veh = Math.min(metering_rate_in_veh,max_rate_in_veh);
    }

	public void setMeteringRateInVPH(Double rate_in_vph){
        setMeteringRateInVeh(rate_in_vph*getMyController().getMyScenario().get.simdtinseconds()/3600d);
	}
	
	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

    public ActuatorRampMeter(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA,ActuatorImplementation act_implementor){
        super(myScenario,jaxbA,act_implementor);
    }

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset / deploy
	/////////////////////////////////////////////////////////////////////

	@Override
    public void populate(Object jaxb,Scenario myScenario) {

        edu.berkeley.path.beats.jaxb.Actuator jaxbA = (edu.berkeley.path.beats.jaxb.Actuator) jaxb;
		myLink = myScenario.get.linkWithId(jaxbA.getScenarioElement().getId());
        double dt_in_hours = myScenario.get.simdtinseconds()/3600d;

        Parameters params = (Parameters) jaxbA.getParameters();

		if(myLink!=null && params!=null){
			double lanes = myLink.get_Lanes();
            max_rate_in_veh = params.has("max_rate_in_vphpl") ?
                    Double.parseDouble(params.get("max_rate_in_vphpl"))*lanes*dt_in_hours :
                    Double.POSITIVE_INFINITY;
            min_rate_in_veh = params.has("min_rate_in_vphpl") ?
                    Double.parseDouble(params.get("min_rate_in_vphpl"))*lanes*dt_in_hours :
                    0d;
		}

        cycle_increment = Double.NaN;
        if(params!=null && params.has("cycle_increment_sec"))
            cycle_increment =  Double.parseDouble(params.get("cycle_increment_sec"));

        // queue override
        if(jaxbA.getQueueOverride()!=null)
            queue_override = this.new QueueOverride(jaxbA.getQueueOverride());

    }

	@Override
    public void validate() {
		if(myLink==null)
			BeatsErrorLog.addError("Bad link ID in ramp metering actuator ID=" + getId());
		if(max_rate_in_veh<0)
			BeatsErrorLog.addError("Negative max rate in ramp metering actuator ID="+getId());
		if(min_rate_in_veh<0)
			BeatsErrorLog.addError("Negative min rate in ramp metering actuator ID="+getId());
		if(max_rate_in_veh<min_rate_in_veh)
			BeatsErrorLog.addError("max rate less than min rate in actuator ID="+getId());
	}

	@Override
	public void deploy(double current_time_in_seconds) {
        if(queue_override!=null)
            metering_rate_in_veh = Math.max(metering_rate_in_veh,queue_override.compute_rate_in_veh());
        implementor.deploy_metering_rate_in_veh(metering_rate_in_veh);
	}

    @Override
    protected void deploy_off_signal(){
        implementor.deploy_metering_rate_in_veh(null);
    };

    @Override
    public boolean register() {
        return ((Link)implementor.get_target()).register_flow_controller();
    }
	
	public Link getLink(){
		return myLink;
	}

    private class QueueOverride implements Serializable{
        private static final long serialVersionUID = 5230456626105848852L;
        public QueueOverrideStrategy strategy;
        public double max_vehicles;

        public QueueOverride(edu.berkeley.path.beats.jaxb.QueueOverride jaxbQ){
           strategy = QueueOverrideStrategy.valueOf(jaxbQ.getStrategy());
            if(getParameters()!=null)  {
                Parameters param = (Parameters) getParameters();
                max_vehicles = param.has("max_queue_vehicles") ? Double.parseDouble(param.get("max_queue_vehicles")) : Double.POSITIVE_INFINITY;
            }
        }

        public double compute_rate_in_veh(){

            double current_veh = myLink.getTotalDensityInVeh(0);

            switch(strategy){
                case none:
                    return Double.NEGATIVE_INFINITY;
                case max_rate:
                    if(BeatsMath.greaterorequalthan(current_veh, max_vehicles))
                        return max_rate_in_veh;
                    else
                        return Double.NEGATIVE_INFINITY;
                case proportional:
                case proportional_integral:
                    System.err.println("Not implemented.");
                    return Double.NaN;
                default:
                    return Double.NaN;
            }
        }

    }

}
