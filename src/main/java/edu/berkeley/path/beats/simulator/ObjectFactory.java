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

//import edu.berkeley.path.beats.control.splitgen.Controller_HOV_SR_Generator;

import edu.berkeley.path.beats.actuator.*;
import edu.berkeley.path.beats.control.*;
//import edu.berkeley.path.beats.control.splitgen.Controller_SR_Generator;
//import edu.berkeley.path.beats.control.splitgen.Controller_SR_Generator_Fw;
import edu.berkeley.path.beats.event.*;
import edu.berkeley.path.beats.sensor.*;

/** Factory methods for creating scenarios, controllers, events, sensors, and scenario elements. 
 * <p>
 * Use the static methods in this class to load a scenario and to programmatically generate events, controllers, sensors, and scenario elements.
 * 
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
@SuppressWarnings("restriction")
final public class ObjectFactory {

//	private static Logger logger = Logger.getLogger(ObjectFactory.class);
	
	/////////////////////////////////////////////////////////////////////
	// private default constructor
	/////////////////////////////////////////////////////////////////////

	private ObjectFactory(){}
							  
	/////////////////////////////////////////////////////////////////////
	// protected create from Jaxb
	/////////////////////////////////////////////////////////////////////
	
	protected static Controller createControllerFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Controller jaxbC,Controller.Algorithm myType) {
		if(myScenario==null)
			return null;
		Controller C;
		switch(myType){
			case IRM_ALINEA:
				C = new Controller_IRM_Alinea(myScenario, jaxbC);
				break;

			case IRM_TOD:
				C = new Controller_IRM_Time_of_Day(myScenario, jaxbC);
				break;

			case IRM_TOS:
				C = new Controller_IRM_Traffic_Responsive(myScenario, jaxbC);
				break;

			case CRM_HERO:
				C = new Controller_CRM_HERO(myScenario, jaxbC);
				break;

			case CRM_MPC:
				C = new Controller_CRM_MPC(myScenario, jaxbC);
				break;

            case FRR_MPC:
                C = new Controller_FRR_MPC(myScenario, jaxbC);
                break;

			case SIG_Pretimed:
				C = new Controller_SIG_Pretimed(myScenario, jaxbC);
				break;

			case SR_Generator:
			case SR_Generator_Fw:
			case SR_Generator_new:
				C = new Controller_SR_Generator_new(myScenario, jaxbC);
				break;
//
//            case SIG_MaxPressure:
//                C = new Controller_SIG_CycleMP(myScenario, jaxbC, myType);
//                break;

//            case SR_Generator:
//                C = new Controller_SR_Generator(myScenario, jaxbC);
//				break;
//
//			case SR_Generator_Fw:
//				C = new Controller_SR_Generator_Fw(myScenario, jaxbC);
//				break;
//
//			case HOV_SR_Generator:
//				C = new Controller_HOV_SR_Generator(myScenario, jaxbC);
//				break;

            case VehType_Swapper:
                C = new Controller_VehType_Swapper(myScenario,jaxbC);
                break;

			default:
				C = null;
				break;
		}
		C.populate(jaxbC);
		return C;
	}

	protected static Event createEventFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Event jaxbE,Event.Type myType) {
		if(myScenario==null)
			return null;
		Event E;
		switch(myType){
			case fundamental_diagram:
				E = new Event_Fundamental_Diagram(myScenario, jaxbE, myType);
				break;

			case link_demand_knob:
				E = new Event_Link_Demand_Knob(myScenario, jaxbE, myType);
				break;

			case link_lanes:
				E = new Event_Link_Lanes(myScenario, jaxbE, myType);
				break;

			case node_split_ratio:
				E = new Event_Node_Split_Ratio(myScenario, jaxbE, myType);
				break;

			case control_toggle:
				E = new Event_Control_Toggle(myScenario, jaxbE, myType);
				break;

			case global_control_toggle:
				E = new Event_Global_Control_Toggle(myScenario, jaxbE, myType);
				break;

			case global_demand_knob:
				E = new Event_Global_Demand_Knob(myScenario, jaxbE, myType);
				break;

			default:
				E = null;
				break;
		}
		E.populate(jaxbE);
		return E;
	}

	protected static Sensor createSensorFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Sensor jaxbS,Sensor.Type myType) {
		if(myScenario==null)
			return null;
		Sensor S;
		switch(myType){
			case loop:
				S = new SensorLoopStation(myScenario, jaxbS, myType);
				break;

			default:
				S = null;
				break;
		}
		S.populate(jaxbS);
		return S;
	}

	protected static Actuator createActuatorFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA,Actuator.Type myType) {
		if(myScenario==null)
			return null;
		Actuator A = null;
        ActuatorImplementation imp = new BeatsActuatorImplementation(jaxbA,myScenario);
        switch(myType){
			case ramp_meter:
				A = new ActuatorRampMeter(myScenario,jaxbA,imp);
				break;

			case vsl:
				//A = new ActuatorVSL(myScenario, jaxbA);
				break;

			case cms:
				A = new ActuatorCMS(myScenario,jaxbA,imp);
				break;

            case vehtype_changer:
                A = new ActuatorVehType(myScenario,jaxbA,imp);
                break;

			default:
				A = null;
				break;
		}
        imp.setActuator(A);
		A.populate(jaxbA,myScenario);
		return A;
	}

    protected static Actuator createActuatorSignalFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Signal jaxbS) {
        if(myScenario==null)
            return null;
        ActuatorImplementation imp = new BeatsActuatorImplementation(jaxbS,myScenario);
        Actuator A = new ActuatorSignal(jaxbS,imp);
        imp.setActuator(A);
        A.populate(jaxbS,myScenario);
        return A;
    }

	protected static ScenarioElement createScenarioElementFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.ScenarioElement jaxbS){
		if(myScenario==null)
			return null;
		ScenarioElement S = new ScenarioElement(myScenario, jaxbS);
		return S;
	}

	
}
