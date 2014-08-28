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

/** Base class for events. 
 * Provides a default implementation of <code>InterfaceEvent</code>.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
@SuppressWarnings("rawtypes")
public class Event implements Comparable {

	/** Scenario that contains this event */
	protected Scenario myScenario;

    protected edu.berkeley.path.beats.jaxb.Event jaxbEvent;
	
	/** Event type. */
    protected Event.Type myType;
	
	/** Activation time of the event, in number of simulation time steps. */
    protected int abs_time_step;
	
	/** List of targets for the event. */
    protected ArrayList<ScenarioElement> targets;
	
	/** Type of event. */
	public static enum Type	{   fundamental_diagram,
                                link_demand_knob,
                                link_lanes,
                                node_split_ratio,
                                control_toggle,
                                global_control_toggle,
                                global_demand_knob }
		   
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	protected Event(){}

	protected Event(Scenario myScenario,edu.berkeley.path.beats.jaxb.Event jaxbE,Event.Type myType){
		this.jaxbEvent = jaxbE;
		this.myScenario = myScenario;
		this.myType = myType;
		this.abs_time_step = BeatsMath.round(jaxbE.getTstamp()/myScenario.getSimdtinseconds());		// assume in seconds
		this.targets = new ArrayList<ScenarioElement>();
		if(jaxbE.getTargetElements()!=null)
			for(edu.berkeley.path.beats.jaxb.ScenarioElement s : jaxbE.getTargetElements().getScenarioElement() )
				this.targets.add(ObjectFactory.createScenarioElementFromJaxb(myScenario,s));
	}

	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	
	public long getId(){
		return this.jaxbEvent.getId();
	}

	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	
	public Scenario getMyScenario() {
		return myScenario;
	}

	public edu.berkeley.path.beats.jaxb.Event getJaxbEvent() {
		return jaxbEvent;
	}

	public Event.Type getMyType() {
		return myType;
	}

	public int getAbsTimeStep() {
		return abs_time_step;
	}

	public ArrayList<ScenarioElement> getTargets() {
		return targets;
	}

	/////////////////////////////////////////////////////////////////////
	// populate / validate / activate
	/////////////////////////////////////////////////////////////////////

	protected void populate(Object jaxbobject) {
	}

	protected void validate() {
		
		if(myType==null)
			BeatsErrorLog.addError("Event with ID=" + getId() + " has bad type.");
			
		// check that there are targets assigned to non-global events
		if(myType!=null)
			if(myType!=Event.Type.global_control_toggle && myType!=Event.Type.global_demand_knob)
				if(targets.isEmpty())
					BeatsErrorLog.addError("No targets assigned in event with ID=" + getId() + ".");
		
		// check each target is valid
		for(ScenarioElement s : targets)
			if(s.getReference()==null)
				BeatsErrorLog.addError("Invalid target ID=" + s.getId() + " in event ID=" + getId() + ".");

	}
	
	protected void activate() throws BeatsException {		
	}

	/////////////////////////////////////////////////////////////////////
	// Comparable
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public int compareTo(Object arg0) {
		
		if(arg0==null)
			return 1;
		
		int compare;
		Event that = ((Event) arg0);
		
		// first ordering by time stamp
		Integer thiststamp = this.abs_time_step;
		Integer thattstamp = that.abs_time_step;
		compare = thiststamp.compareTo(thattstamp);
		if(compare!=0)
			return compare;

		// second ordering by event type
		Event.Type thiseventtype = this.myType;
		Event.Type thateventtype = that.myType;
		compare = thiseventtype.compareTo(thateventtype);
		if(compare!=0)
			return compare;
		
		// third ordering by number of targets
		Integer thisnumtargets = this.targets.size();
		Integer thatnumtargets = that.targets.size();
		compare = thisnumtargets.compareTo(thatnumtargets);
		if(compare!=0)
			return compare;
		
		// fourth ordering by target type
		for(int i=0;i<thisnumtargets;i++){
			ScenarioElement.Type thistargettype = this.targets.get(i).getMyType();
			ScenarioElement.Type thattargettype = that.targets.get(i).getMyType();
			compare = thistargettype.compareTo(thattargettype);
			if(compare!=0)
				return compare;		
		}

		// fifth ordering by target ID
		for(int i=0;i<thisnumtargets;i++){
			Long thistargetId = this.targets.get(i).getId();
			Long thattargetId = that.targets.get(i).getId();
			compare = thistargetId.compareTo(thattargetId);
			if(compare!=0)
				return compare;
		}

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		return this.compareTo((Event) obj)==0;
	}
    
    protected void setGlobalDemandEventKnob(double knob){
    	myScenario.setGlobal_demand_knob(knob);
    }
	    
	/////////////////////////////////////////////////////////////////////
	// internal class
	/////////////////////////////////////////////////////////////////////	

//	protected static class SplitRatio {
//		private int input_index;
//		private int output_index;
//		private int vehicle_type_index;
//		private double value;
//
//		public SplitRatio(int input_index, int output_index, int vehicle_type_index, double value) {
//			this.input_index = input_index;
//			this.output_index = output_index;
//			this.vehicle_type_index = vehicle_type_index;
//			this.value = value;
//		}
//
//		public int getInputIndex() {
//			return input_index;
//		}
//		public int getOutputIndex() {
//			return output_index;
//		}
//		public int getVehicleTypeIndex() {
//			return vehicle_type_index;
//		}
//		public double getValue() {
//			return value;
//		}
//	}

}
