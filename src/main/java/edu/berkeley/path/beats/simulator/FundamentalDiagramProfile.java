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
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileFD;

import java.io.Serializable;

public final class FundamentalDiagramProfile extends edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile implements Serializable {

    private static final long serialVersionUID = 1939374386939181119L;

    // does not change ....................................
    private Scenario myScenario;
    private Link myLink;
    private BeatsTimeProfileFD fdProfile;

    /////////////////////////////////////////////////////////////////////
    // protected interface
    /////////////////////////////////////////////////////////////////////

    // scale present and future fundamental diagrams to new lane value
    protected void set_Lanes(double newlanes) {
        if (newlanes <= 0 || fdProfile.isEmpty())
            return;
        fdProfile.set_lanes(newlanes);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / reset / validate / update
    /////////////////////////////////////////////////////////////////////

    protected void populate(Scenario myScenario) throws BeatsException {

        this.myScenario = myScenario;
        this.myLink = myScenario.get.linkWithId(getLinkId());

        if (myLink != null)
            myLink.setFundamentalDiagramProfile(this);

        fdProfile = new BeatsTimeProfileFD(
            myLink,
            getFundamentalDiagram(),
            getDt(),
            getStartTime(),
            myScenario.get.simdtinseconds()
        );
    }

    protected void validate() {

        if (myLink == null)
            BeatsErrorLog.addError("Bad link ID=" + getLinkId() + " in fundamental diagram.");

        fdProfile.validate();
    }

    protected void reset() throws BeatsException {
        fdProfile.reset();
    }

    public void update(boolean forcesample,Clock clock) throws BeatsException {
        if (myLink == null || fdProfile==null || fdProfile.isEmpty())
            return;

        if(fdProfile.sample(forcesample,clock))
            myLink.setFDFromProfile(fdProfile.getCurrentSample());
    }

    /////////////////////////////////////////////////////////////////////
    // public API
    /////////////////////////////////////////////////////////////////////

    public FundamentalDiagram getFDforTime(double time) {
        return fdProfile.sample_at_time(time);
    }

    @Override
    public String toString() {
        String str = "";
        edu.berkeley.path.beats.simulator.FundamentalDiagram fd = (FundamentalDiagram) getFundamentalDiagram().get(0);
        str += "vf=" + fd.getVfNormalized() + ",";
        str += "w=" + fd.getWNormalized() + ",";
        str += "C=" + fd._getCapacityInVeh() + ",";
        return str;
    }

}