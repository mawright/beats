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

import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfile;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileDouble;

import java.io.Serializable;

public final class CapacityProfile extends edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacityProfile implements Serializable {

	private static final long serialVersionUID = -5500522551015498761L;
	// does not change ....................................
	private Scenario myScenario;
	private boolean isOrphan;
	private BeatsTimeProfileDouble capacity;	// [veh]

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {

		this.myScenario = myScenario;

		// required
		Link myLink = myScenario.get.linkWithId(getLinkId());

		isOrphan = myLink==null;
				
		if(isOrphan)
			return;
		
		// set to link
		myLink.myCapacityProfile = this;

		// sample demand distribution, convert to vehicle units
        // sample demand distribution, convert to vehicle units
        if(getContent()!=null){

            Double dt = getDt();
            Double startTime = getStartTime();
            double simdtinseconds = myScenario.get.simdtinseconds();

            capacity = new BeatsTimeProfileDouble(getContent(),",",dt,startTime,simdtinseconds);	// true=> reshape to vector along k, define length
            capacity.multiplyscalar(myScenario.get.simdtinseconds()*myLink.get_Lanes());
        }

    }
	
	protected void validate() {

		if(isOrphan){
			BeatsErrorLog.addWarning("Bad origin link ID=" + getLinkId() + " in capacity profile.");
			return;
		}
		
		if(capacity==null || capacity.isEmpty())
			return;

//		// check dtinseconds
//		if( dtinseconds<=0  && capacity.getNumTime()>1)
//			BeatsErrorLog.addError("Non-positive time step in capacity profile for link ID=" + getLinkId());
//
//		if(!BeatsMath.isintegermultipleof(dtinseconds,myScenario.get.simdtinseconds()) && capacity.getNumTime()>1)
//			BeatsErrorLog.addError("Time step for capacity profile of link ID=" + getLinkId() + " is not a multiple of simulation time step.");
//
//		// check non-negative
//		if(capacity.hasNaN())
//			BeatsErrorLog.addError("Capacity profile for link ID=" +getLinkId()+ " has illegal values.");

	}

	protected void reset() {
		if(isOrphan)
			return;
        capacity.reset();
	}
	
	protected void update(boolean forcesample) {

        if(isOrphan)
            return;

        if(capacity==null || capacity.isEmpty())
            return;

        // sample splits and update
        capacity.sample(forcesample,myScenario.clock);

	}

	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	
	public double getCurrentValue(){
		return capacity.getCurrentSample();
    }

}
