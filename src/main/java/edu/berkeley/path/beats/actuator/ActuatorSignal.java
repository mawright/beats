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

package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.control.SignalCommand;
import edu.berkeley.path.beats.jaxb.Phase;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import edu.berkeley.path.beats.simulator.utils.DebugFlags;

import java.io.Serializable;
import java.util.*;

/** ActuatorSignal class.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public final class ActuatorSignal extends Actuator  implements Serializable {

    private static final long serialVersionUID = -1132579882403725363L;

    public static enum BulbColor {GREEN,YELLOW,RED,DARK};
	private HashMap<NEMA.ID,SignalPhase> nema2phase;
	private Node myNode;
	private ArrayList<SignalPhase> phases;
    private PerformanceCalculator.SignalLogger signal_logger;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public ActuatorSignal(edu.berkeley.path.beats.jaxb.Signal jaxbS,ActuatorImplementation act_implementor){

        super(act_implementor,Type.signal);

        // set ID for this acuator
        setId(jaxbS.getId());
    }

    /////////////////////////////////////////////////////////////////////
    // actuation command
    /////////////////////////////////////////////////////////////////////

    public void set_command(ArrayList<SignalCommand> command){

        for(SignalCommand c : command){
            SignalPhase p = nema2phase.get(c.nema);
            if(p==null)
                continue;

            switch(c.type){
                case forceoff:
                    p.forceoff_requested = true;
                    p.actualyellowtime = c.yellowtime>=0 ? c.yellowtime : p.yellowtime;
                    p.actualredcleartime = c.redcleartime>=0 ? c.redcleartime : p.redcleartime;
                    break;

                case hold:
                    p.hold_requested = true;
                    break;
            }
        }
    }

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / deploy
	/////////////////////////////////////////////////////////////////////

    @Override
    public void populate(Object jaxbobject,Scenario myScenario) {

        edu.berkeley.path.beats.jaxb.Signal jaxbSignal = (edu.berkeley.path.beats.jaxb.Signal)jaxbobject;

		myNode = myScenario.get.nodeWithId(jaxbSignal.getNodeId());
		
		if(myNode==null)
			return;

        // make list of phases and map
		phases = new ArrayList<SignalPhase>();
		nema2phase = new HashMap<NEMA.ID,SignalPhase>();
        //HashMap<NEMA.ID,List<Link>> nema_to_linklist = (HashMap<NEMA.ID,List<Link>>) implementor.get_target();
        for(Phase jphase : jaxbSignal.getPhase() ){
            NEMA.ID nema = NEMA.int_to_nema(jphase.getNema().intValue());
            //List<Link> link_list = nema_to_linklist.get( nema );

            SignalPhase sp = new SignalPhase(myNode,this,myScenario.get.simdtinseconds());
            sp.populateFromJaxb(myScenario,jphase);
            phases.add(sp);
            nema2phase.put(nema,sp);
        }

//        // get reference to opposing phase
//        for(SignalPhase phase : phases)
//            phase.opposingPhase = get_phase_with_nema(NEMA.get_opposing(phase.myNEMA));
	}

    @Override
    public void reset() throws BeatsException {
        super.reset();
		if(myNode==null)
			return;
		for(SignalPhase p : phases)
			p.reset();
	}

    @Override
    public void validate() {
		
		if(myNode==null){
			BeatsErrorLog.addWarning("Unknow node ID in signal ID=" + getId());
			return; // this signal will be ignored
		}
		
		if(phases==null)
			BeatsErrorLog.addError("ActuatorSignal ID=" + getId() + " contains no valid phases.");
        else
			for(SignalPhase p : phases)
				p.validate();
	}

    @Override
    public void deploy(double current_time_in_seconds) {

		if(myNode==null)
			return;

		// 0) Advance all phase timers ...........................................
		for(SignalPhase phase : phases)
            phase.bulbtimer.advance();

		// Update detector stations ............................................
//		for(SignalPhase phase : phases)
//			phase.UpdateDetectorStations();

//		// Update stopline calls
//		for(SignalPhase phase : phases)
//		    phase.hasstoplinecall = phase.Recall() ? true : phase.StoplineStation()!=null && phase.StoplineStation().GotCall();
//
//		// Update approach calls
//		for(SignalPhase phase : phases)
//			phase.hasapproachcall = phase.ApproachStation()!=null && phase.ApproachStation().GotCall();
//
//		// Update conflicting calls
//        for(SignalPhase phase : phases)
//            phase.currentconflictcall = CheckForConflictingCall(i);
//        for(SignalPhase phase : phases){
//			if( !phase.hasconflictingcall && phase.currentconflictcall )
//                phase.conflictingcalltime = (float)(myNode.getMyNetwork().getSimTime()*3600f);
//            phase.hasconflictingcall = phase.currentconflictcall;
//        }

//		for(SignalPhase phase:phases)
//			phase.updatePermitOpposingHold();
//
//		// 3) Update permitted holds ............................................
//		for(SignalPhase phaseA:phases){
//            phaseA.permithold = true;
//			for(SignalPhase phaseB:phases)
//				if(!NEMA.is_compatible(phaseA, phaseB) && !phaseB.permitopposinghold )
//                    phaseA.permithold = false;
//		}
//
//		// 4) Update signal commands ...................................................
//
//		// Throw away conflicting hold pairs
//		// (This is purposely drastic to create an error)
//		for(SignalPhase phaseA:phases)
//			if(phaseA.hold_requested)
//				for(SignalPhase phaseB:phases)
//					if( phaseB.hold_requested && !NEMA.is_compatible(phaseA, phaseB) ){
//                        phaseA.hold_requested = false;
//                        phaseB.hold_requested = false;
//					}
//
//		// Deal with simultaneous hold and forceoff (RHODES needs this)
//		for(SignalPhase phase: phases)
//			if( phase.hold_requested && phase.forceoff_requested )
//                phase.forceoff_requested = false;

		// Make local relaying copy
        for(SignalPhase phase : phases){
            phase.hold_approved = phase.hold_requested;
            phase.forceoff_approved = phase.forceoff_requested;
		}

//		// No transition if no permission
//        for(SignalPhase phase : phases)
//			if( !phase.permithold )
//                phase.hold_approved = false;

//		// No transition if green time < mingreen
//        for(SignalPhase phase : phases)
//            if( phase.bulbcolor==BulbColor.GREEN  && BeatsMath.lessthan(phase.bulbtimer.getT(),phase.mingreen) )
//				phase.forceoff_approved = false;

		// collect updated bulb indications
		for(SignalPhase phase : phases){
            ActuatorSignal.BulbColor new_bulb_color = phase.get_new_bulb_color(phase.hold_approved,phase.forceoff_approved);

            // set phase color if changed
            if(new_bulb_color!=phase.bulbcolor){

                phase.bulbcolor = new_bulb_color;

                if(DebugFlags.signal_events)
                    System.out.println(
                            myNode.getMyNetwork().getMyScenario().get.currentTimeInSeconds() + "\t" +
                            "signal=" + getId() + "\t" +
                            "phase=" + phase.myNEMA + "\t" +
                            "color=" + phase.bulbcolor );

                // log
                if(signal_logger!=null)
                    signal_logger.send_event(getId(),phase.myNEMA,phase.bulbcolor);

                // deploy to dynamics
                implementor.deploy_bulb_color(phase.myNEMA,phase.bulbcolor);
            }
        }

        // Remove serviced commands
		for(SignalPhase phase: phases){
			if(phase.is_green())
                phase.hold_requested = false;
			if(phase.is_yellow() || phase.is_red() )
                phase.forceoff_requested = false;
		}

//		// Set permissive opposing left turn to yellow
//		// opposing is yellow if I am green or yellow, and I am through, and opposing is permissive
//		// opposing is red if I am red and it is not protected
//		for(SignalPhase phase : phases){
//			SignalPhase ophase = phase.getOpposingPhase();
//			if(ophase==null)
//				continue;
//			switch(phase.getBulbColor()){
//				case GREEN:
//				case YELLOW:
//					if(phase.isIsthrough() && ophase.isPermissive())
//                        ophase.setPhaseColor(ActuatorSignal.BulbColor.YELLOW);
//					break;
//				case RED:
//					if(!ophase.isProtected())
//						ophase.setPhaseColor(ActuatorSignal.BulbColor.RED);
//					break;
//			case DARK:
//				break;
//			default:
//				break;
//			}
//		}


	}

    @Override
    protected void deploy_off_signal(){
        System.err.print("Cannot turn signals off");
    };

    @Override
    public boolean register() {
        HashMap<NEMA.ID,List<Link>> phase_link_map = (HashMap<NEMA.ID,List<Link>>) implementor.get_target();
        boolean success = true;
        for(List<Link> link_list : phase_link_map.values())
            for(Link link : link_list)
                success &= link.register_flow_controller();
        return success;
    }

	/////////////////////////////////////////////////////////////////////
	// public methods
	/////////////////////////////////////////////////////////////////////

    // hack for call to deploy by Controller.initialize_actuators
    public void deploy(double current_time_in_seconds,Controller caller){
        if(caller==getMyController())
            deploy(current_time_in_seconds);
    }

	public SignalPhase get_phase_with_nema(NEMA.ID nema){
		if(nema==null)
			return null;
		return nema2phase.get(nema);
	}

    public void set_phase_state(NEMA.ID nema,BulbColor bulb_color){
        SignalPhase phase = nema2phase.get(nema);
        if(phase==null)
            return;
        phase.set_bulb_color(bulb_color);
    }

    public Long get_node_id(){
        return myNode==null ? null : myNode.getId();
    }

    public void register_event_logger(PerformanceCalculator.SignalLogger logger){
        if(signal_logger==null)
            signal_logger = logger;
    }

    public static int color_to_int(BulbColor x){
        switch(x){
            case GREEN:
                return 1;
            case YELLOW:
                return 2;
            case RED:
                return 3;
            case DARK:
                return 4;
            default:
                return 0;
        }
    }
    /////////////////////////////////////////////////////////////////////
    // SignalPhase class
    /////////////////////////////////////////////////////////////////////

    public class SignalPhase implements Serializable{

        private static final long serialVersionUID = -808386477650736702L;
        // references ....................................................
        protected ActuatorSignal mySignal;

        // properties ....................................................
        protected boolean protectd	    = false;
        protected boolean isthrough	    = false;
//        protected boolean recall		= false;
        protected boolean permissive	= false;
//        protected boolean lag 		    = false;

        // dual ring structure
//        protected int myRingGroup = -1;
//        protected SignalPhase opposingPhase;
        protected NEMA.ID myNEMA = NEMA.ID.NULL;

        // Basic timing parameters
        protected double mingreen;
        protected double yellowtime;
        protected double redcleartime;
        protected double actualyellowtime;
        protected double actualredcleartime;

        // timers
        protected Clock bulbtimer;

        // State
        protected ActuatorSignal.BulbColor bulbcolor;

        //private int [] myControlIndex;

        // Detectors
        //private DetectorStation ApproachStation = null;
        //private DetectorStation StoplineStation = null;
        //private Vector<Integer> ApproachStationIds;
        //private Vector<Integer> StoplineStationIds;

        // Detector memory
//        protected boolean hasstoplinecall		= false;
//        protected boolean hasapproachcall		= false;
//        protected boolean hasconflictingcall	= false;
//        protected boolean currentconflictcall   = false;
//        protected float conflictingcalltime	= 0f;

        // Safety
//        protected boolean permitopposinghold 	= true;
//        protected boolean permithold			= true;

        // Controller command
        protected boolean hold_requested 		= false;
        protected boolean forceoff_requested	= false;
        protected boolean hold_approved 		= false;
        protected boolean forceoff_approved 	= false;

        /////////////////////////////////////////////////////////////////////
        // construction
        /////////////////////////////////////////////////////////////////////

        public SignalPhase(Node myNode,ActuatorSignal mySignal,double dt){
            this.mySignal = mySignal;
            this.bulbtimer = new Clock(0d,Double.POSITIVE_INFINITY,dt);
        }

        /////////////////////////////////////////////////////////////////////
        // populate / rese / validate
        /////////////////////////////////////////////////////////////////////

        protected final void populateFromJaxb(Scenario myScenario,edu.berkeley.path.beats.jaxb.Phase jaxbPhase){
            myNEMA = NEMA.string_to_nema(jaxbPhase.getNema().toString());
            mingreen = jaxbPhase.getMinGreenTime();
            redcleartime = jaxbPhase.getRedClearTime();
            yellowtime = jaxbPhase.getYellowTime();
//            lag = jaxbPhase.isLag();
            permissive = jaxbPhase.isPermissive();
            protectd = jaxbPhase.isProtected();
//            recall = jaxbPhase.isRecall();
            actualyellowtime   = yellowtime;
            actualredcleartime = redcleartime;
            isthrough = NEMA.is_through(myNEMA);
//            myRingGroup = NEMA.get_ring_group(myNEMA);
        }

        protected void reset() {
//            hasstoplinecall		= false;
//            hasapproachcall		= false;
//            hasconflictingcall	= false;
//            conflictingcalltime	= 0f;
            hold_requested 		= false;
            forceoff_requested	= false;
//            permithold			= true;
//            permitopposinghold  = false;
            bulbcolor = ActuatorSignal.BulbColor.DARK;
            bulbtimer.reset();
        }

        protected void validate() {

//		// check that there are links attached
//		if(targetlinks==null || targetlinks.length==0)
//			BeatsErrorLog.addError("No valid target link for phase NEMA=" + getNEMA() + " in signal ID=" + signal.getId());
//
//		// target links are valid
//		if(targetlinks!=null)
//			for(int i=0;i<targetlinks.length;i++)
//				if(targetlinks[i]==null)
//					BeatsErrorLog.addError("Unknown link reference in phase NEMA=" + getNEMA() + " in signal ID=" + signal.getId());

            // myNEMA is valid
            if(myNEMA==NEMA.ID.NULL)
                BeatsErrorLog.addError("Invalid NEMA code in phase NEMA=" + myNEMA + " in signal ID=" + mySignal.getId());

            // numbers are positive
            if( mingreen<0 )
                BeatsErrorLog.addError("Negative mingreen=" + mingreen + " in signal ID=" + mySignal.getId());

            if( yellowtime<0 )
                BeatsErrorLog.addError("Negative yellowtime=" + yellowtime + " in signal ID=" + mySignal.getId());

            if( redcleartime<0 )
                BeatsErrorLog.addError("Negative redcleartime=" + redcleartime + " in signal ID=" + mySignal.getId());
        }

        /////////////////////////////////////////////////////////////////////
        // protected
        /////////////////////////////////////////////////////////////////////

        protected void set_bulb_color(BulbColor b){
            this.bulbcolor = b;
        }

//        protected void updatePermitOpposingHold(){
//            switch(bulbcolor){
//                case GREEN:
//                    // iff I am about to go off and there is no transition time
//                    permitopposinghold = forceoff_requested && actualyellowtime==0 && redcleartime==0;
//                    break;
//                case YELLOW:
//                    // iff near end yellow time and there is no red clear time
//                    permitopposinghold =  BeatsMath.greaterorequalthan(bulbtimer.getT(),actualyellowtime-bulbtimer.getDt()) && redcleartime==0;
//                    break;
//                case RED:
//                    // iff near end of red clear time and not starting again.
//                    permitopposinghold =  BeatsMath.greaterorequalthan(bulbtimer.getT(),redcleartime-bulbtimer.getDt()) && !hold_requested;
//                    break;
//                case DARK:
//                    break;
//            }
//        }

        protected ActuatorSignal.BulbColor get_new_bulb_color(boolean hold_approved,boolean forceoff_approved){

            ActuatorSignal.BulbColor next_color = bulbcolor;
            double bulbt = bulbtimer.getT();

            if(!protectd)
                return permissive ? ActuatorSignal.BulbColor.DARK : ActuatorSignal.BulbColor.RED;

            // execute this state machine until "done". May be more than once if
            // some state has zero holding time (eg yellowtime=0)
            boolean done=false;


            while(!done){

                switch(next_color){

                    // .............................................................................................
                    case GREEN:

//				permitopposinghold = false;

                        // Force off
                        if( forceoff_approved ){
                            next_color = ActuatorSignal.BulbColor.YELLOW;
//					signal.getCompletedPhases().add(signal.new PhaseData(myNEMA, signal.getMyScenario().getClock().getT() - bulbtimer.getT(), bulbtimer.getT()));
                            bulbtimer.reset();
                            //FlushAllStationCallsAndConflicts();
                            done = actualyellowtime>0;
                        }
                        else
                            done = true;

                        break;

                    // .............................................................................................
                    case YELLOW:

                        // set permitopposinghold one step ahead of time so that other phases update correctly next time.
//				permitopposinghold = false;

//				if( BeatsMath.greaterorequalthan(bulbt,actualyellowtime-bulbtimer.dt) && redcleartime==0)
//					permitopposinghold = true;

                        // yellow time over, go immediately to red if redcleartime==0
                        if( BeatsMath.greaterorequalthan(bulbt, actualyellowtime) ){
                            next_color = ActuatorSignal.BulbColor.RED;
                            bulbtimer.reset();
                            done = redcleartime>0;
                        }
                        else
                            done = true;
                        break;

                    // .............................................................................................
                    case RED:

                        //if( BeatsMath.greaterorequalthan(bulbt,redcleartime-myNode.getMyNetwork().getTP()*3600f  && !goG )
//				if( BeatsMath.greaterorequalthan(bulbt,redcleartime-bulbtimer.dt) && !hold_approved )
//					permitopposinghold = true;
//				else
//					permitopposinghold = false;

                        // if hold, set to green, go to green, etc.
                        if( hold_approved ){
                            next_color = ActuatorSignal.BulbColor.GREEN;
                            bulbtimer.reset();

                            // Unregister calls (for reading conflicting calls)
                            //FlushAllStationCallsAndConflicts(); // GCG ?????

                            done = !forceoff_approved;
                        }
                        else
                            done = true;

                        break;
                    case DARK:
                        next_color = ActuatorSignal.BulbColor.RED;
                        done = true;
                        break;
                    default:
                        break;
                }

            }
            return next_color;
        }

        /////////////////////////////////////////////////////////////////////
        // public interface
        /////////////////////////////////////////////////////////////////////

        public boolean is_green(){
            return bulbcolor==BulbColor.GREEN;
        }

        public boolean is_yellow(){
            return bulbcolor==BulbColor.YELLOW;
        }

        public boolean is_red(){
            return bulbcolor==BulbColor.RED;
        }

        public BulbColor get_bulb_color(){
            return bulbcolor;
        }

        public double getYellowtime() {
            return yellowtime;
        }

        public double getRedcleartime() {
            return redcleartime;
        }

        public double getMingreen() {
            return mingreen;
        }

        public NEMA.ID getNEMA() {
            return myNEMA;
        }

        public double getActualyellowtime() {
            return actualyellowtime;
        }

        public double getActualredcleartime() {
            return actualredcleartime;
        }

    }

}



