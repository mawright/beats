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

final public class SplitRatioSet extends edu.berkeley.path.beats.jaxb.SplitRatioSet {

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {
		
		if(getSplitRatioProfile().isEmpty())
			return;
		
		for(edu.berkeley.path.beats.jaxb.SplitRatioProfile sr : getSplitRatioProfile())
			((SplitRatioProfile) sr).populate(myScenario);
	}
	
	protected void reset() {
		for(edu.berkeley.path.beats.jaxb.SplitRatioProfile sr : getSplitRatioProfile())
    		((SplitRatioProfile) sr).reset();
	}

	protected void validate() {

		if(getSplitRatioProfile()==null)
			return;
		
		if(getSplitRatioProfile().isEmpty())
			return;

		for(edu.berkeley.path.beats.jaxb.SplitRatioProfile sr : getSplitRatioProfile())
			((SplitRatioProfile)sr).validate();
	}

    public void update() {
    	for(edu.berkeley.path.beats.jaxb.SplitRatioProfile sr : getSplitRatioProfile())
    		((SplitRatioProfile) sr).update(false);
	}

    @Override
    public String toString() {
        String str = "";
        if(getSplitRatioProfile()!=null)
            for(edu.berkeley.path.beats.jaxb.SplitRatioProfile srp : getSplitRatioProfile())
                if(srp.getSplitratio()!=null)
                    for(Splitratio sr : srp.getSplitratio())
                        str += String.format("%d\t%d\t%d\t%d\t%s\n",srp.getNodeId(),sr.getLinkIn(),sr.getLinkOut(),sr.getVehicleTypeId(),sr.getContent());
        return str;
    }

    /////////////////////////////////////////////////////////////////////
    // public interface
    /////////////////////////////////////////////////////////////////////

    public SplitRatioProfile get_split_profile_for_node_id(Long id){
        for(edu.berkeley.path.beats.jaxb.SplitRatioProfile sr : getSplitRatioProfile())
            if(sr.getNodeId()==id)
                return (SplitRatioProfile) sr;
        return null;
    }

    public void set_split_profile_for_node_id(Long id,SplitRatioProfile srp){
        boolean foundit = false;
        for(int i=0;i<getSplitRatioProfile().size();i++)
            if(getSplitRatioProfile().get(i).getNodeId()==srp.getNodeId()) {
                getSplitRatioProfile().set(i, srp);
                foundit = true;
            }
        if(!foundit)
            getSplitRatioProfile().add(srp);
    }

}