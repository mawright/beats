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

//package edu.berkeley.path.beats.control;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import edu.berkeley.path.beats.actuator.ActuatorSignal;
//import edu.berkeley.path.beats.actuator.SignalPhase;
//import edu.berkeley.path.beats.simulator.Scenario;
//
//public class Controller_SIG_Pretimed_IntersectionPlan {

    // up references
//    protected Controller_SIG_Pretimed_Plan plan;
//    protected ActuatorSignal signal;
//
//    // data
//    protected float offset;	// offset for the intersection
//    protected List<Controller_SIG_Stage> stages;
//
//    // command
//	protected ArrayList<ActuatorSignal.Command> command = new ArrayList<ActuatorSignal.Command>();
//    protected int nextcommand;
//    protected double lastcommandtime;

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
// 	public Controller_SIG_Pretimed_IntersectionPlan(Controller_SIG_Pretimed_Plan myPlan){
//		this.plan = myPlan;
//	}

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset  / update
	/////////////////////////////////////////////////////////////////////
 	
//	@SuppressWarnings("unchecked")
//	protected void populate(Scenario myScenario, Controller_SIG_Pretimed.Intersection intersection) {
								
//		if (null != intersection.getOffset())
//			this.offset = intersection.getOffset().floatValue();
//		else
//			this.offset = 0f;
//
//		numstages = intersection.getStage().size(); 	// number of stages (length of movA, movB and greentime)
//
//		if(numstages<=1)
//			return;
//
//		greentime = new double[numstages];
//		movA = new ActuatorSignal.NEMA[numstages];
//		movB = new ActuatorSignal.NEMA[numstages];
//
//		for(int i=0;i<numstages;i++){
//			Controller_SIG_Pretimed.Stage stage = intersection.getStage().get(i);
//			if (null != stage.getGreenTime())
//				greentime[i] = stage.getGreenTime().floatValue();
//			else
//				greentime[i] = Double.NaN;
//
//			movA[i] = ActuatorSignal.string_to_nema(stage.getMovA());
//			movB[i] = ActuatorSignal.string_to_nema(stage.getMovB());
//		}
//
//		signal = myScenario.getSignalWithNodeId(intersection.getNodeId());
//
//		// Set yellowtimes, redcleartimes, stagelength, totphaselength
//		int k;
//		SignalPhase pA;
//		SignalPhase pB;
//		double y,r,yA,yB,rA,rB;
//		float totphaselength = 0;
//		stagelength = new double[numstages];
//		for(k=0;k<numstages;k++){
//
//			pA = signal.getPhaseByNEMA(movA[k]);
//			pB = signal.getPhaseByNEMA(movB[k]);
//
//			if(pA==null && pB==null)
//				return;
//
//			yA = pA==null ? 0.0 : pA.getYellowtime();
//			rA = pA==null ? 0.0 : pA.getRedcleartime();
//			yB = pB==null ? 0.0 : pB.getYellowtime();
//			rB = pB==null ? 0.0 : pB.getRedcleartime();
//
//			y = Math.max(yA,yB);
//			r = Math.max(rA,rB);
//
//			if( InNextStage(pA,k) ){
//				y = yB;
//				r = rB;
//			}
//
//			if( InNextStage(pB,k) ){
//				y = yA;
//				r = rA;
//			}
//
//			if(pA!=null){
//				pA.setActualyellowtime(y);
//				pA.setActualredcleartime(r);
//			}
//
//			if(pB!=null){
//				pB.setActualyellowtime(y);
//				pB.setActualredcleartime(r);
//			}
//
//			stagelength[k] = greentime[k]+y+r;
//			totphaselength += greentime[k]+y+r;
//		}
//
//		// compute hold and forceoff points ............................................
//		double stime, etime;
//		int nextstage;
//		stime=0;
//		for(k=0;k<numstages;k++){
//
//			etime = stime + greentime[k];
//			stime = stime + stagelength[k];
//
//			if(k==numstages-1)
//				nextstage = 0;
//			else
//				nextstage = k+1;
//
//			if(stime>=totphaselength)
//				stime = 0;
//
//			// do something if the phase changes from this stage to the next
//			if(movA[k].compareTo(movA[nextstage])!=0){
//
//				// force off this stage
//				if(movA[k].compareTo(NEMA.NULL)!=0){
//					pA = signal.getPhaseByNEMA(movA[k]);
//					command.add(new Command(ActuatorSignal.CommandType.forceoff,movA[k],etime,pA.getActualyellowtime(),pA.getActualredcleartime()));
//				}
//
//				// hold next stage
//				if(movA[nextstage].compareTo(NEMA.NULL)!=0)
//					command.add(new Command(ActuatorSignal.CommandType.hold,movA[nextstage],stime));
//
//			}
//
//			// same for ring B
//			if(movB[k].compareTo(movB[nextstage])!=0){
//				if(movB[k].compareTo(NEMA.NULL)!=0){
//					pB = signal.getPhaseByNEMA(movB[k]);
//					command.add(new Command(ActuatorSignal.CommandType.forceoff,movB[k],etime,pB.getActualyellowtime(),pB.getActualredcleartime()));
//				}
//				if(movB[nextstage].compareTo(NEMA.NULL)!=0)
//					command.add(new Command(ActuatorSignal.CommandType.hold,movB[nextstage],stime));
//			}
//
//		}
//
//		// Correction: offset is with respect to end of first stage, instead of beginning
//		for(ActuatorSignal.Command c : command){
//			c.time -= greentime[0];
//			if(c.time<0)
//				c.time += plan._cyclelength;
//		}
//
//		// sort the commands
//		Collections.sort(command);
//
//		lastcommandtime = command.get(command.size()-1).time;

//	}
//
//	protected void validate(double controldt){
//
//		// at least two stages
//		if(numstages<=1)
//			BeatsErrorLog.addError("ActuatorSignal ID=" + signal.getId() + " has less than two stages.");
//
//		// check offset
//		if(offset<0 || offset>=plan._cyclelength)
//			BeatsErrorLog.addError("Offset for signal ID=" + signal.getId() + " is not between zero and the cycle length.");
//
//		//  greentime, movA, movB
//		for(int k=0;k<numstages;k++){
//			if(Double.isNaN(greentime[k]) || greentime[k]<=0)
//				BeatsErrorLog.addError("Invalid green time in stage for signal ID=" + signal.getId());
//			if(movA[k]==null && movB[k]==null)
//				BeatsErrorLog.addError("Invalid phase in stage for signal ID=" + signal.getId());
//		}
//
//		// values are integer multiples of controller dt
//		for(int k=0;k<numstages;k++){
//			if(!BeatsMath.isintegermultipleof((double) greentime[k],controldt))
//				BeatsErrorLog.addError("Green time not a multiple of control time step in signal ID=" + signal.getId());
//			if(stagelength[k]!=greentime[k])
//				if(!BeatsMath.isintegermultipleof((double) stagelength[k]-greentime[k],controldt))
//					BeatsErrorLog.addError("Lost time not a multiple of control time step in signal ID=" + signal.getId());
//		}
//
//		// check cycles are long enough .....................................
//		float totphaselength=0;
//		for(int k=0;k<numstages;k++)
//			totphaselength += stagelength[k];
//		if(!BeatsMath.equals(plan._cyclelength,totphaselength))
//			BeatsErrorLog.addError("Stages do not add up to cycle time in signal ID=" + signal.getId());
//
//		// first two commands have zero timestamp
//		if(command.get(0).time!=0.0)
//			BeatsErrorLog.addError("Initial stages for pretimed plan of signal ID=" + signal.getId() + " must have time stamp equal zero.");
//	}

//	protected void reset(){
//		nextcommand = 0;
//	}
//
//	/////////////////////////////////////////////////////////////////////
//	// protected methods
//	/////////////////////////////////////////////////////////////////////
//
//	protected boolean InNextStage(SignalPhase thisphase,int stageindex)
//	{
//		int nextstage;
//
//		if(stageindex<0 || stageindex>=numstages || thisphase==null)
//			return false;
//
//		nextstage = stageindex+1;
//
//		if(nextstage==numstages)
//			nextstage=0;
//
//		return thisphase.getNEMA().compareTo(movA[nextstage])==0 || thisphase.getNEMA().compareTo(movB[nextstage])==0;
//        return true;
//	}
//
//	protected void getCommandForTime(double itime,ArrayList<SignalCommand> commandlist){
//
//		double reltime = itime - offset;
//		if(reltime<0)
//			reltime += plan._cyclelength;
//
//		if(reltime>lastcommandtime)
//			return;
//
//		double nexttime = command.get(nextcommand).time;
//
//		if(nexttime<=reltime){
//			while(nexttime<=reltime){
//				commandlist.add(command.get(nextcommand));
//				nextcommand += 1;
//				if(nextcommand==command.size()){
//					nextcommand = 0;
//					break;
//				}
//				nexttime = command.get(nextcommand).time;
//			}
//		}
//
//	}
//
//}
