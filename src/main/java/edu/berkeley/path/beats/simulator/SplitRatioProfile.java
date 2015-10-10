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

import edu.berkeley.path.beats.jaxb.Splitratio;
import edu.berkeley.path.beats.simulator.utils.*;

import java.util.ArrayList;

public final class SplitRatioProfile extends edu.berkeley.path.beats.jaxb.SplitRatioProfile {

	// does not change ...................................
	private Scenario myScenario;
	private Node myNode;
	private boolean isdeterministic;
    private BeatsTimeProfileDouble3D splitsProfile;
    private BeatsTimeProfileDouble3D concentrationProfile;

	// does change ........................................

    private Double [][][] splitsCurrent; 	// current split ratio matrix with dimension [inlink x outlink x vehicle type]
    private Double [][][] concentrationCurrent; // current CP matric with dimensions [inlink x outlink x vehtype]

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

    protected void populate(Scenario myScenario) {

        ArrayList<Integer> in_index,out_index,vt_index;
        ArrayList<String> stringArray;

        try {
            if(getSplitratio().isEmpty())
                return;

            if(myScenario==null)
                return;

            this.myScenario = myScenario;

            // required
            myNode = myScenario.get.nodeWithId(getNodeId());

            if(myNode==null)
                return;

            int nIn = myNode.getnIn();
            int nOut = myNode.getnOut();
            int nVt = myScenario.get.numVehicleTypes();

            // gather information to create the splits profile
            in_index = new ArrayList<Integer>();
            out_index = new ArrayList<Integer>();
            vt_index = new ArrayList<Integer>();
            stringArray = new ArrayList<String>();
            for(edu.berkeley.path.beats.jaxb.Splitratio sr : getSplitratio()) {
                in_index.add(myNode.getInputLinkIndex(sr.getLinkIn()));
                out_index.add(myNode.getOutputLinkIndex(sr.getLinkOut()));
                vt_index.add(myScenario.get.vehicleTypeIndexForId(sr.getVehicleTypeId()));
                stringArray.add(sr.getContent());
            }

            // create split profile
            if(!stringArray.isEmpty())
                splitsProfile = new BeatsTimeProfileDouble3D(
                        in_index,out_index, vt_index,stringArray,
                        nIn,nOut,nVt,
                        getDt(),getStartTime(),
                        myScenario.get.simdtinseconds() );

            // gather information to create the concentration profile
            in_index = new ArrayList<Integer>();
            out_index = new ArrayList<Integer>();
            vt_index = new ArrayList<Integer>();
            stringArray = new ArrayList<String>();
            for(edu.berkeley.path.beats.jaxb.ConcentrationParameters cp : getConcentrationParameters()) {
                in_index.add(myNode.getInputLinkIndex(cp.getLinkIn()));
                out_index.add(myNode.getOutputLinkIndex(cp.getLinkOut()));
                vt_index.add(myScenario.get.vehicleTypeIndexForId(cp.getVehicleTypeId()));
                stringArray.add(cp.getContent());
            }

            // create concentration profile
            if(!stringArray.isEmpty())
                concentrationProfile = new BeatsTimeProfileDouble3D(
                        in_index,out_index, vt_index,stringArray,
                        nIn,nOut,nVt,
                        getDt(),getStartTime(),
                        myScenario.get.simdtinseconds() );


            // optional uncertainty model
            isdeterministic = !hasConcentrationParameters() && super.getVariance()==null;

            // inform the node
            myNode.setMySplitRatioProfile(this);

//            splitsCurrent = new Double3DMatrix(nIn,nOut,nVt);
//            currentConcentrationParameters = new Double3DMatrix(nIn,nOut,nVt);

        } catch (BeatsException e) {
            e.printStackTrace();
        }

    }

    protected void reset() {
        if (myNode==null)
            return;
        splitsProfile.reset();

        int nIn = myNode.getnIn();
        int nOut = myNode.getnOut();
        int nVt = myScenario.get.numVehicleTypes();

        splitsCurrent = BeatsMath.nans(nIn,nOut,nVt);
        if(concentrationProfile!=null)
            concentrationProfile.reset();
        concentrationCurrent = BeatsMath.nans(nIn,nOut,nVt);
	}

	protected void validate() {
        if(getSplitratio().isEmpty()){
            BeatsErrorLog.addWarning("Split ratio ID=" + this.getId() + " has no data.");
            return;
        }

        if(myNode==null){
            BeatsErrorLog.addWarning("Unknown node with ID=" + getNodeId() + " in split ratio profile.");
            return; // this profile will be skipped but does not cause invalidation.
        }

        // check link ids
        int index;
        for(edu.berkeley.path.beats.jaxb.Splitratio sr : getSplitratio()){
            index = myNode.getInputLinkIndex(sr.getLinkIn());
            if(index<0)
                BeatsErrorLog.addError("Bad input link ID=" + sr.getLinkIn() + " in split ratio profile with node ID=" + getNodeId());

            index = myNode.getOutputLinkIndex(sr.getLinkOut());
            if(index<0)
                BeatsErrorLog.addError("Bad output link ID=" + sr.getLinkOut() + " in split ratio profile with node ID=" + getNodeId());

        }

        splitsProfile.validate(0d,1d);

        if(concentrationProfile!=null)
            concentrationProfile.validate(1d,Double.POSITIVE_INFINITY);
	}

    protected void update(boolean forcesample) {
        if(splitsProfile==null)
            return;
        if(myNode==null)
            return;

        // sample splits and update
        if( splitsProfile.sample(forcesample,myScenario.clock) ) {
            splitsCurrent = BeatsMath.normalize(splitsProfile.getCurrentSample());
            if(splitsCurrent==null)
                splitsCurrent = BeatsMath.nans(myNode.nIn,myNode.nOut,myScenario.numVehicleTypes);
        }

        // sample concentration parameters and update
        if( concentrationProfile!=null && concentrationProfile.sample(forcesample,myScenario.clock) ) {
            concentrationCurrent = concentrationProfile.getCurrentSample();
            if(concentrationCurrent==null)
                concentrationCurrent = BeatsMath.nans(myNode.nIn,myNode.nOut,myScenario.numVehicleTypes);
        }

    }

	/////////////////////////////////////////////////////////////////////
	// protected getter
	/////////////////////////////////////////////////////////////////////

    protected Double [][][] getCurrentSplitRatio() {
        return splitsCurrent;
    }

    protected Double getSplit(int i,int j,int k) {
        return splitsCurrent[i][j][k];
    }

    protected Double [][][] getCurrentConcentration() {
        return concentrationCurrent;
    }

    public Double getCurrentSplitRatio(int i,int j,int k) {
        return splitsCurrent[i][j][k];
    }

//	protected boolean isCurrentConcentrationParametersValid() {
//		int i,j,v;
//		for(i=0;i<myNode.getnIn();i++){
//			for(j=0;j<myNode.getnOut();j++){
//				for(v=0;v<myScenario.get.numVehicleTypes();v++){
//					if(Double.isNaN(currentConcentrationParameters.get(i,j,v)))
//						return false;
//				}
//			}
//		}
//		return true;
//	}


    /////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
//	// for time sample k, returns a 3D matrix with dimensions inlink x outlink x vehicle type
//	private Double3DMatrix sampleAtTimeStep(int k){
//		if(myNode==null)
//			return null;
//		Double3DMatrix X = new Double3DMatrix(myNode.getnIn(),myNode.getnOut(),
//				myScenario.get.numVehicleTypes(),Double.NaN);	// initialize all unknown
//
//		// get vehicle type order from SplitRatioProfileSet
////		Integer [] vehicletypeindex = null;
////		if(myScenario.getSplitRatioSet()!=null)
////			vehicletypeindex = ((SplitRatioSet)myScenario.getSplitRatioSet()).getVehicletypeindex();
//
//		int i,j,v,lastk;
//		for(i=0;i<myNode.getnIn();i++){
//			for(j=0;j<myNode.getnOut();j++){
//				for(v=0;v<myScenario.get.numVehicleTypes();v++){
//					if(profile[i][j][v]==null)						// nan if not defined
//						continue;
//					if(profile[i][j][v].isEmpty())					// nan if no data
//						continue;
//					lastk = Math.min(k,profile[i][j][v].getNumTime()-1);	// hold last value
//					X.set(i,j,v,profile[i][j][v].get(lastk));
//				}
//			}
//		}
//		return X;
//	}
//
//	private Double3DMatrix sampleConcentrationParametersAtTimeStep(int k){
//		if(myNode==null)
//			return null;
//		if (!hasConcentrationParameters())
//			return null;
//		Double3DMatrix X = new Double3DMatrix(myNode.getnIn(),myNode.getnOut(),
//				myScenario.get.numVehicleTypes(),Double.NaN);	// initialize all unknown
//
//		int i,j,v,lastk;
//		for(i=0;i<myNode.getnIn();i++){
//			for(j=0;j<myNode.getnOut();j++){
//				for(v=0;v<myScenario.get.numVehicleTypes();v++){
//					if(concentrationParamsProfile[i][j][v]==null)						// nan if not defined
//						continue;
//					if(concentrationParamsProfile[i][j][v].isEmpty())					// nan if no data
//						continue;
//					lastk = Math.min(k,concentrationParamsProfile[i][j][v].getNumTime()-1);	// hold last value
//					X.set(i,j,v,concentrationParamsProfile[i][j][v].get(lastk));
//				}
//			}
//		}
//		return X;
//	}

    /////////////////////////////////////////////////////////////////////
    // public API
    /////////////////////////////////////////////////////////////////////

//    public Double [] predict(long inlink_id,long outlink_id,int vt_index,Double start_time,double time_step,int num_steps){
//
//        int in_index = myNode.getInputLinkIndex(inlink_id);
//        int out_index = myNode.getOutputLinkIndex(outlink_id);
//
//        Double [] val = BeatsMath.zeros(num_steps);
//        for(int i=0;i<num_steps;i++){
//
//            double time = start_time + i*time_step + 0.5*time_step;
//            double rel_time = time - splitsProfile.start_time;
//            int step = BeatsMath.floor(rel_time/splitsProfile.getDtinseconds());
//            Double [][][] sample = splitsProfile.sample_step(step);
//            val[i] = sample[in_index][out_index][vt_index];
//        }
//        return val;
//    }

    public boolean isdeterministic() {
        return isdeterministic;
    }

    public boolean hasConcentrationParameters() {
        return !getConcentrationParameters().isEmpty();
    }

    public boolean isConstant(){
        return splitsProfile.getNumTime()==1;
    }

    @Override
    public String toString() {
        String str = String.format("node_id = %d\n",nodeId);
        for(Splitratio sr : this.getSplitratio())
            str += String.format("\tlink_in=%d, link_out=%d, profile=%s\n",sr.getLinkIn(),sr.getLinkOut(),sr.getContent());
        return str;
    }

}
