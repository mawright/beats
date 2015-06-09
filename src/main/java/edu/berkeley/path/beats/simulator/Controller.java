/**
 * Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 **/

package edu.berkeley.path.beats.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import edu.berkeley.path.beats.simulator.utils.Table;
import org.apache.log4j.Logger;

import edu.berkeley.path.beats.jaxb.FeedbackSensor;
import edu.berkeley.path.beats.jaxb.TargetActuator;

/** Base class for controllers. 
 * Provides a default implementation of <code>InterfaceController</code>.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public class Controller {

	protected Scenario myScenario;
	protected edu.berkeley.path.beats.jaxb.Controller jaxbController;
	protected Controller.Algorithm myType;
	protected ArrayList<Actuator> actuators;
	protected ArrayList<String> actuator_usage;
	protected ArrayList<Sensor> sensors;
	protected ArrayList<String> sensor_usage;
	protected double dtinseconds;
	protected int samplesteps;
	protected boolean ison;
//	protected ArrayList<ActivationTimes> activationTimes;
	protected java.util.Map<String, Table> tables;

	public static enum Algorithm {
        IRM_ALINEA,
        IRM_TOD,
        IRM_TOS,
        CRM_HERO,
        CRM_MPC,
        FRR_MPC,
        SIG_Pretimed,
        SIG_MaxPressure,
        SR_Generator,
		SR_Generator_Fw,
		SR_Generator_new,
		HOV_SR_Generator,
        Commodity_Swapper}

    public static enum ActuatorType {
		ramp_meter,
		cms,
		vsl,
		signal
	}
	
    public static final Map<Algorithm, ActuatorType> map_algorithm_actuator = new HashMap<Algorithm, ActuatorType>();
    static {
    	map_algorithm_actuator.put( Algorithm.IRM_ALINEA , ActuatorType.ramp_meter);
    	map_algorithm_actuator.put( Algorithm.IRM_TOD 		, ActuatorType.ramp_meter );
    	map_algorithm_actuator.put( Algorithm.IRM_TOS 		, ActuatorType.ramp_meter );
    	map_algorithm_actuator.put( Algorithm.CRM_HERO 		, ActuatorType.ramp_meter );
    	map_algorithm_actuator.put( Algorithm.CRM_MPC 		, ActuatorType.ramp_meter );
    	map_algorithm_actuator.put( Algorithm.SIG_Pretimed  , ActuatorType.signal );
    }

	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

    protected Controller(){}
      
	 protected Controller(Scenario myScenario,edu.berkeley.path.beats.jaxb.Controller jaxbC,Controller.Algorithm myType){
		 
			this.myScenario = myScenario;
			this.myType = myType;
			this.jaxbController = jaxbC;
			this.ison = false;
//			this.activationTimes=new ArrayList<ActivationTimes>();
			this.dtinseconds = jaxbC.getDt()>0 ? jaxbC.getDt() : myScenario.get.simdtinseconds();


			// Copy tables
			tables = new java.util.HashMap<String, Table>();
			for (edu.berkeley.path.beats.jaxb.Table table : jaxbC.getTable()) {
				if (tables.containsKey(table.getName()))
					BeatsErrorLog.addError("Table '" + table.getName() + "' already exists");
				tables.put(table.getName(), new Table(table));
			}
			
//			// Get activation times and sort
//			if (jaxbC.getActivationIntervals()!=null)
//				for (edu.berkeley.path.beats.jaxb.Interval tinterval : jaxbC.getActivationIntervals().getInterval())
//					if(tinterval!=null)
//						activationTimes.add(new ActivationTimes(tinterval.getStartTime(),tinterval.getEndTime()));
//			Collections.sort(activationTimes);

            // below this does not apply for scenario-less controllers  ..............................
            if(myScenario==null)
                return;

            samplesteps = BeatsMath.round(dtinseconds / myScenario.get.simdtinseconds());

            // read target actuators
			actuators = new ArrayList<Actuator>();
			actuator_usage = new ArrayList<String>();
			if(jaxbC.getTargetActuators()!=null && jaxbC.getTargetActuators().getTargetActuator()!=null){
				for(TargetActuator ta : jaxbC.getTargetActuators().getTargetActuator()){
                    Actuator act = myScenario.get.actuatorWithId(ta.getId());
                    actuators.add(act);
                    act.setMyController(this);
					actuator_usage.add(ta.getUsage()==null ? "" : ta.getUsage());
				}
			}

			// read feedback sensors
			sensors = new ArrayList<Sensor>();
			sensor_usage = new ArrayList<String>();
			if(jaxbC.getFeedbackSensors()!=null && jaxbC.getFeedbackSensors().getFeedbackSensor()!=null){
				for(FeedbackSensor fs : jaxbC.getFeedbackSensors().getFeedbackSensor()){
					sensors.add(getMyScenario().get.sensorWithId(fs.getId()));
					sensor_usage.add(fs.getUsage()==null ? "" : fs.getUsage());
				}
			}

	 }
	 
	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset  / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(Object jaxbobject) {
	}

    protected void initialize_actuators(){
    }

    /** Update the state of the component.
	 * 
	 * <p> Called by {@link Scenario#run} at each simulation time step.
	 * This function updates the internal state of the component.
	 * <p> Because events are state-less, the {@link Event} class provides a default 
	 * implementation of this method, so it need not be implemented by other event classes.
	 */
	protected void update() throws BeatsException {
	}


	protected void validate() {

		// check that type was read correctly
		if(myType==null)
			BeatsErrorLog.addError("Controller with ID=" + getId() + " has the wrong type.");

        // validations below this make sense only in the context of a scenario
        if(myScenario==null)
            return;

		// check that sample dt is an integer multiple of network dt
		if(!BeatsMath.isintegermultipleof(dtinseconds,myScenario.get.simdtinseconds()))
			BeatsErrorLog.addError("Time step for controller ID=" +getId() + " is not a multiple of the simulation time step.");

//		// check that activation times are valid.
//		for (int i=0; i<activationTimes.size(); i++ ){
//			activationTimes.get(i).validate();
//			if (i<activationTimes.size()-1)
//				activationTimes.get(i).validateWith(activationTimes.get(i+1));
//		}

	}

	/** Prepare the component for simulation.
	 * 
	 * <p> Called by {@link Scenario#run} each time a new simulation run is started.
	 * It is used to initialize the internal state of the component.
	 * <p> Because events are state-less, the {@link Event} class provides a default 
	 * implementation of this method, so it need not be implemented by other event classes.
	 */
	protected void reset() {
//		//switch on conroller if it is always on by default.
//		if (activationTimes==null || activationTimes.isEmpty())
			ison = true;
	}
		
	/////////////////////////////////////////////////////////////////////
	// registration / deregistration
	/////////////////////////////////////////////////////////////////////

	/** Register the controller with its targets. 
	 * 
	 * <p> All controllers must register with their targets in order to be allowed to
	 * manipulate them. This is to prevent clashes, in which two or 
	 * more controllers access the same variable.
	 * The return value of these methods indicates whether the registration was successful.
	 * 
	 * @return <code>true</code> if the controller successfully registered with all of its targets; 
	 * <code>false</code> otherwise.
	 */
	public boolean register() {
        for(Actuator act : actuators)
            if(!act.register())
                return false;
		return true;
	}

	/** Deregister the controller with its targets. 
	 * 
	 * <p> All controllers must deregister with their targets when they are no longer active
	 *  This is to prevent clashes, in which two or more controllers access the same variable at different simulation periods
	 * The return value of these methods indicates whether the deregistration was successful.
	 * 
	 * @return <code>true</code> if the controller successfully registered with all of its targets; 
	 * <code>false</code> otherwise.
	 */
	protected boolean deregister() {
		return false;
	}
	
	// Returns the start and end times of the controller.
	
//   	/** Returns the first start time of the controller. This is the minimum of the start
//   	 * times of all activation periods of the controller.
//   	 * @return A double with the start time for the controller.
//   	 */
//	protected double getFirstStartTime(){
//        // this should not be used if no scenario is defined
//        if(myScenario==null)
//            return Double.NaN;
//		double starttime=myScenario.getTimeStart();
//		for (int ActTimesIndex = 0; ActTimesIndex < activationTimes.size(); ActTimesIndex++ )
//			if (ActTimesIndex == 0)
//				starttime=activationTimes.get(ActTimesIndex).getBegintime();
//			else
//				starttime=Math.min(starttime,activationTimes.get(ActTimesIndex).getBegintime());
//		return starttime;
//	}
	
//   	/** Returns the last end time of the controller. This is the maximum of the end
//   	 * times of all activation periods of the controller.
//   	 * @return A double with the end time for the controller.
//   	 */
//	protected double getlastEndTime(){
//        if(myScenario==null)
//            return Double.NaN;
//		double endtime=myScenario.getTimeEnd();
//		for (int ActTimesIndex = 0; ActTimesIndex < activationTimes.size(); ActTimesIndex++ )
//			if (ActTimesIndex == 0)
//				endtime=activationTimes.get(ActTimesIndex).getEndtime();
//			else
//				endtime=Math.max(endtime,activationTimes.get(ActTimesIndex).getEndtime());
//		return endtime;
//	}

	private static Logger logger = Logger.getLogger(Controller.class);

	/**
	 * Retrieves a table with the given name
	 * @param jaxb_controller a controller to get a table from
	 * @param name the table name
	 * @return null, if a table with the given name was not found
	 */
	protected static Table findTable(edu.berkeley.path.beats.jaxb.Controller jaxb_controller, String name) {
		Table table = null;
		for (edu.berkeley.path.beats.jaxb.Table jaxb_table : jaxb_controller.getTable())
			if (name.equals(jaxb_table.getName())) {
				if (null == table)
					table = new Table(jaxb_table);
				else
					logger.error("Controller " + jaxb_controller.getId() + ": duplicate table '" + name + "'");
			}
		return table;
	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

   	public Scenario getMyScenario() {
		return myScenario;
	}

	public int getSamplesteps() {
		return samplesteps;
	}

//	public ArrayList<ActivationTimes> getActivationTimes() {
//		return activationTimes;
//	}

	public java.util.Map<String, Table> getTables() {
		return tables;
	}	
	
   	/** Get the ID of the controller  */
	public long getId() {
		return this.jaxbController.getId();
	}	

	/** Get the type of the controller.  */
	public Controller.Algorithm getMyType() {
		return myType;
	}

	public int getNumActuators() {
		return actuators==null ? 0 : actuators.size();
	}

	public int getNumSensors() {
		return sensors==null ? 0 : sensors.size();
	}

   	/** Get the controller update period in [seconds]  */
	public double getDtinseconds() {
		return dtinseconds;
	}

   	/** Get the on/off value of the controller 
   	 * @return <code>true</code> if the controller is currently on, <code>off</code> otherwise. 
   	 */
	public boolean isIson() {
		return ison;
	}

	public ArrayList<Actuator> getActuatorByUsage(String usage){
		ArrayList<Actuator> list = new ArrayList<Actuator>();
		for(int i=0;i<actuators.size();i++)
			if(actuator_usage.get(i).compareTo(usage)==0)
				list.add(actuators.get(i));
		return list;
	}

	public ArrayList<Sensor> getSensorByUsage(String usage){
		ArrayList<Sensor> list = new ArrayList<Sensor>();
		for(int i=0;i<sensors.size();i++)
			if(sensor_usage.get(i).compareTo(usage)==0)
				list.add(sensors.get(i));
		return list;
	}

	
	/////////////////////////////////////////////////////////////////////
	// protected getters and setters
	/////////////////////////////////////////////////////////////////////

	protected edu.berkeley.path.beats.jaxb.Controller getJaxbController() {
		return jaxbController;
	}

	public void setIson(boolean ison) {
		this.ison = ison;
        for(Actuator act : this.actuators)
            act.setIsOn(ison);
	}
	
	/////////////////////////////////////////////////////////////////////
	// internal classes
	/////////////////////////////////////////////////////////////////////
	
	/** Creates a new class that stores begin and end times for each period of controller activation */
	protected class ActivationTimes implements Comparable<ActivationTimes>{
		
		/** Start time for each activation interval */
		protected double begintime; 
		
		/** End time for each activation interval */
		protected double endtime;
		
		protected ActivationTimes(double begintime, double endtime) {
			super();
			this.begintime = begintime;
			this.endtime = endtime;
		}
		
		public double getBegintime() {
			return begintime;
		}
		
		protected void setBegintime(double begintime) {
			this.begintime = begintime;
		}
		
		public double getEndtime() {
			return endtime;
		}
		
		protected void setEndtime(double endtime) {
			this.endtime = endtime;
		}		
		
		protected void validate(){			
			if (begintime-endtime>=0)
				BeatsErrorLog.addError("Begin time must be larger than end time.");		  
		}
		
		protected void validateWith(ActivationTimes that){			
			if (Math.max(this.begintime-that.getEndtime(), that.getBegintime()-this.endtime)<0)  // Assumption - activation times is sorted before this is invoked, should remove this assumption later.
				BeatsErrorLog.addError("Activation Periods of the controllers must not overlap.");
		}
		
		/////////////////////////////////////////////////////////////////////
		// Comparable
		/////////////////////////////////////////////////////////////////////		

		@Override
		public int compareTo(ActivationTimes that) {
			if(that==null)
				return 1;
			
			// Order first by begintimes.
			int compare = ((Double) this.getBegintime()).compareTo((Double) that.getBegintime());
		
			if (compare!=0)
				return compare;
				
		    // Order next by endtimes.
			compare = ((Double) this.getEndtime()).compareTo((Double) that.getEndtime());
				
			return compare;
		}
		
	}

}
