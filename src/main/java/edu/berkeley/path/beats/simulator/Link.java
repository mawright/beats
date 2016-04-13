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

import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorCTM;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** Link class.
 * 
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public class Link extends edu.berkeley.path.beats.jaxb.Link implements Serializable {

    private static final long serialVersionUID = 6752301095198199827L;

    public enum Type {onramp,offramp,source,freeway,hov,intersection_approach,street,other,undefined}

    protected Scenario myScenario;
    protected Network myNetwork;

	// does not change ....................................
    protected Node begin_node;
    protected Node end_node;
    public Type link_type;

    // in/out flows (from node model or demand profiles)
    public Double [][] inflow;    					// [veh]	numEnsemble x numVehTypes
    public Double [][] outflow;    				    // [veh]	numEnsemble x numVehTypes

    // link geometry
    public double _lanes;							// [-]

	// source/sink indicators
    public boolean issource; 						// [boolean]
    public boolean issink;     					    // [boolean]

    public FundamentalDiagramProfile myFDprofile;	// fundamental diagram profile (used to rescale future FDs upon lane change event)
    public CapacityProfile myCapacityProfile; 		// capacity profile
    public DemandProfile myDemandProfile;  		    // demand profiles
	
	// Actuation
    protected boolean has_flow_controller;
    protected boolean has_speed_controller;
    protected boolean has_density_controller;
    protected boolean has_vehtype_controller;
    protected double external_max_flow;
    protected double external_max_speed;
	
	// does change ........................................
	
	// link geometry
    public double _length;							// [meters]

	// FDs
    protected FundamentalDiagram [] FDfromProfile;	    // profile fundamental diagram
    protected FundamentalDiagram FDfromEvent;			// event fundamental diagram
	
	// Events
    protected boolean activeFDevent;					// true if an FD event is active on this link,

	// link state
    protected Double [] initial_density;			    // [veh]  	numVehTypes

    // link behavior
    public LinkBehaviorCTM link_behavior;

    // vehtype switching control
    protected boolean has_vehtype_switchratio;
    protected List<SwitchRatio> controllerSwitchRatios;

	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	protected Link(){}

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////    

	protected void populate(Network myNetwork) {

        this.myNetwork = myNetwork;
        this.myScenario = myNetwork.getMyScenario();

		// make network connections
		begin_node = myNetwork.getNodeWithId(getBegin().getNodeId());
		end_node = myNetwork.getNodeWithId(getEnd().getNodeId());

		// nodes must populate before links
		if(begin_node!=null)
			issource = begin_node.isTerminal();
		if(end_node!=null)
			issink = end_node.isTerminal();

		// lanes and length
		_lanes = getLanes();
		_length = getLength();

        has_flow_controller = false;
        has_speed_controller = false;
        has_density_controller = false;
        has_vehtype_controller = false;

        // link type
        if(getLinkType()==null)
            link_type = Type.undefined;
        else{
            String name = getLinkType().getName();
            if(name.compareToIgnoreCase("Intersection Approach")==0)
                link_type = Type.intersection_approach;
            else if (name.compareToIgnoreCase("Street")==0)
                link_type = Type.street;
            else if (name.compareToIgnoreCase("On-Ramp")==0)
                link_type = Type.onramp;
            else if (name.compareToIgnoreCase("Off-Ramp")==0)
                link_type = Type.offramp;
            else if (name.compareToIgnoreCase("Freeway")==0)
                link_type = Type.freeway;
            else if (name.compareToIgnoreCase("Source")==0)
                link_type = Type.source;
            else if (name.compareToIgnoreCase("HOV")==0)
                link_type = Type.hov;
            else
                link_type = Type.other;
        }

	}

	protected void validate() {

		if(!issource && begin_node==null)
			BeatsErrorLog.addError("Incorrect begin node ID=" + getBegin().getNodeId() + " in link ID=" + getId() + ".");

		if(!issink && end_node==null)
			BeatsErrorLog.addError("Incorrect node ID=" + getEnd().getNodeId() + " in link ID=" + getId() + ".");

		if(_length<=0)
			BeatsErrorLog.addError("Non-positive length in link ID=" + getId() + ".");

		if(_lanes<=0)
			BeatsErrorLog.addError("Non-positive number of lanes in link ID=" + getId() + ".");
	}

	protected void reset(){
		resetLanes();		
		resetFD();
        link_behavior.reset(initial_density);

        int n1 = myScenario.get.numEnsemble();
        int n2 = myScenario.get.numVehicleTypes();
        inflow = BeatsMath.zeros(n1, n2);
        outflow = BeatsMath.zeros(n1,n2);

		this.external_max_flow = Double.POSITIVE_INFINITY;
		this.external_max_speed = Double.POSITIVE_INFINITY;
    }

	private void resetLanes(){
		_lanes = getLanes();
	}

	private void resetFD(){
		FDfromProfile = new FundamentalDiagram [myScenario.get.numEnsemble()];
		for(int i=0;i<FDfromProfile.length;i++){
			FDfromProfile[i] = new FundamentalDiagram(this);
			FDfromProfile[i].settoDefault();
		}
		FDfromEvent = null;
		activeFDevent = false;
	}

	public void update_densities() {

        // behavior for all sink links
        if(issink)
            outflow = link_behavior.flow_demand;

        // behavior for all source links
        if(issource && myDemandProfile!=null)
            for(int e=0;e<myScenario.get.numEnsemble();e++)
                inflow[e] = myDemandProfile.getCurrentValue(e);

        // apply vehtype switching on just-entering vehicles if applicable
        if(has_vehtype_switchratio) {
            Double[] flowDifference;
            for(int e=0; e<inflow.length; e++ ) { // inflow is ensembleSize long
                flowDifference = BeatsMath.zeros(inflow[e].length);

                // convert all switching ratios to vehicles
                for( SwitchRatio sr : controllerSwitchRatios) {
                    int vehTypeInIndex = myScenario.get.vehicleTypeIndexForId(sr.getVehicleTypeIn());
                    int vehTypeOutIndex = myScenario.get.vehicleTypeIndexForId(sr.getVehicleTypeOut());
                    double value = Double.parseDouble(sr.getContent());
                    flowDifference[vehTypeInIndex] -= inflow[e][vehTypeInIndex] * value;
                    flowDifference[vehTypeOutIndex] += inflow[e][vehTypeInIndex] * value;
                }
                for (int i = 0; i < inflow[e].length; i++) {
                    inflow[e][i] += flowDifference[i];
                }
            }
        }

        link_behavior.update_state(inflow,outflow);
	}

    public void updateOutflowDemand(){
        link_behavior.update_outflow_demand(external_max_speed, external_max_flow);
    }

    /////////////////////////////////////////////////////////////////////
    // protected interface
    /////////////////////////////////////////////////////////////////////

    // FD profile ......................................................

    // called by FundamentalDiagramProfile.populate,
    protected void setFundamentalDiagramProfile(FundamentalDiagramProfile fdp){
        if(fdp==null)
            return;
        myFDprofile = fdp;
    }

    // used by FundamentalDiagramProfile to set the FD
    protected void setFDFromProfile(FundamentalDiagram fd) throws BeatsException {
        if(fd==null)
            return;

        // sample the fundamental digram
        for(int e=0;e<myScenario.get.numEnsemble();e++)
            FDfromProfile[e] = fd.perturb();
    }

    // initial condition ..................................................
    protected void copy_state_to_initial_state(){
        initial_density = getDensityInVeh(0).clone();
    }

    protected void set_initial_state(Double [] d){
        initial_density  = d==null ? BeatsMath.zeros(myScenario.get.numVehicleTypes()) : d.clone();
    }

    public void setInflow(int ensemble,Double[] inflow) {
        this.inflow[ensemble] = inflow;
    }

    public void setOutflow(int ensemble,Double[] outflow) {
        this.outflow[ensemble] = outflow;
    }

    // getter for the currently active fundamental diagram
    public FundamentalDiagram currentFD(int ensemble){
        if(activeFDevent)
            return FDfromEvent;
        return FDfromProfile==null ? null : FDfromProfile[ensemble];
    }

    /////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	
	// Events ..........................................................

	// used by Event.setLinkFundamentalDiagram to activate an FD event
	public void activateFDEvent(edu.berkeley.path.beats.jaxb.FundamentalDiagram fd) throws BeatsException {
		if(fd==null)
			return;

		FDfromEvent = new FundamentalDiagram(this,currentFD(0));		// copy current FD 
		// note: we are copying from the zeroth FD for simplicity. The alternative is to 
		// carry numEnsemble event FDs.
		FDfromEvent.copyfrom(fd);			// replace values with those defined in the event

		BeatsErrorLog.clearErrorMessage();
		FDfromEvent.validate();
		if(BeatsErrorLog.haserror())
			throw new BeatsException("Fundamental diagram event could not be validated.");

		activeFDevent = true;
	}

	// used by Event.revertLinkFundamentalDiagram
	public void revertFundamentalDiagramEvent() throws BeatsException{
		if(!activeFDevent)
			return;
		activeFDevent = false;
	}

	// used by Event.setLinkLanes
	public void set_Lanes(double newlanes) throws BeatsException{
		for(int e=0;e<myScenario.get.numEnsemble();e++)
			if(getDensityJamInVeh(e)*newlanes/get_Lanes() < getTotalDensityInVeh(e))
				throw new BeatsException("ERROR: Lanes could not be set.");

		if(myFDprofile!=null)
			myFDprofile.set_Lanes(newlanes);	// adjust present and future fd's
		for(int e=0;e<myScenario.get.numEnsemble();e++)
			FDfromProfile[e].setLanes(newlanes);
		_lanes = newlanes;					// adjust local copy of lane count
	}

	// controller registration .........................................

    public boolean register_flow_controller(){
        if(has_flow_controller)
            return false;
        has_flow_controller = true;
        return true;
    }

    public boolean register_speed_controller(){
        if(has_speed_controller)
            return false;
        has_speed_controller = true;
        return true;
    }

    public boolean register_density_controller(){
        if(has_density_controller)
            return false;
        has_density_controller = true;
        return true;
    }

    public boolean register_vehtype_controller(){
        if(has_vehtype_controller)
            return false;
        has_vehtype_controller = true;
        return true;
    }

    public void set_external_max_flow_in_veh(double value_in_veh){
        if(has_flow_controller)
            external_max_flow = value_in_veh;
    }

	public void set_external_max_flow_in_vph(double value_in_vph){
        if(has_flow_controller)
            external_max_flow = value_in_vph*myScenario.get.simdtinseconds()/3600d;
	}
	
    public void set_external_max_speed(double value){
        if(has_speed_controller)
            external_max_speed = value;
    }

    public void set_controller_switch_ratios(List<SwitchRatio> switchRatios){
        if(!has_vehtype_controller)
            return;

        this.controllerSwitchRatios = switchRatios;
        has_vehtype_switchratio = true;
    }

    public void deactivate_vehtype_switching() {
        if(!has_vehtype_controller)
            return;

        this.controllerSwitchRatios = null;
        has_vehtype_switchratio = false;
    }

    public Network getMyNetwork(){
        return myNetwork;
    }

    public Scenario getMyScenario(){
        return myScenario;
    }

	public static boolean haveSameType(Link linkA,Link linkB){
        return linkA.link_type==linkB.link_type;
	}

	public boolean isOnramp(){
		return link_type==Type.onramp;
	}

    public boolean isOfframp(){
        return link_type==Type.offramp;
    }

	public boolean isFreeway(){
        return link_type==Type.freeway;
	}

    public boolean isHov(){
        return link_type==Type.hov;
    }

    public double computeTotalDelayInVeh(int e){
        double val=0d;
        for(int v=0;v<myScenario.get.numVehicleTypes();v++)
            val += link_behavior.compute_delay_in_veh(e, v);
        return val;
    }

    // generic method to check permissibility of entrance for vType
    // to be expanded for more cases as needed (including querying controllers)
    public boolean canVTypeEnter(String vType){
//        if(isHov() && vType.compareToIgnoreCase("hov")!=0)
//            return false;
        return true;
    }

	public boolean canVTypeEnter(int vTypeIndex){
		return canVTypeEnter(myScenario.get.vehicleTypeNames()[vTypeIndex]);
	}

    // Link geometry ....................

	public Node getBegin_node() {
		return begin_node;
	}

	public Node getEnd_node() {
		return end_node;
	}

	public double getLengthInMeters() {
		return _length;
	}

	public double get_Lanes() {
		return _lanes;
	}

	public boolean isSource() {
		return issource;
	}

	public boolean isSink() {
		return issink;
	}

	// Link weaving and merging behavior
	
	public double getPriority(int ensemble) {
		BigDecimal priority = getPriority();
		return null != priority ? priority.doubleValue() : getCapacityInVeh(ensemble);
	}
	
	// Link state .......................

    public Double[] getDensityInVeh(int ensemble) {
        try{
            int nVT = myScenario.get.numVehicleTypes();
            Double [] d = BeatsMath.zeros(nVT);
            for(int v=0;v<nVT;v++)
                d[v] = link_behavior.get_density_in_veh(ensemble, v);
            return d;
        }
        catch(IndexOutOfBoundsException excp){
            return null;
        }
    }

    public double getDensityInVeh(int ensemble,int vehicletype) {
        try{
            return link_behavior.get_density_in_veh(ensemble, vehicletype);
        }
        catch(IndexOutOfBoundsException excp){
            return Double.NaN;
        }
    }

    public double getTotalDensityInVeh(int ensemble) {
        return BeatsMath.sum(getDensityInVeh(ensemble));
    }

    public double getTotalDensityInVPMeter(int e) {
        return getTotalDensityInVeh(e)/_length;
    }

    // dimension of d is # vehicle types
    public boolean set_density_in_veh(int e,Double [] d){
        return link_behavior.set_density_in_veh(e,d);
    }

    public Double[] get_out_demand_in_veh(int e) {
        return link_behavior.flow_demand[e];
    }

    public Double get_total_out_demand_in_veh(int e){
        return BeatsMath.sum(link_behavior.flow_demand[e]);
    }

    public double get_total_space_supply_in_veh(int e) {
        return link_behavior.total_space_supply[e];
    }

    public double get_available_space_supply_in_veh(int e) {
        return link_behavior.available_space_supply[e];
    }

    /////////////////////////////////////////////////////////////////////
    // interface for node model
    /////////////////////////////////////////////////////////////////////

    public Double[] getOutflowInVeh(int ensemble) {
        try{
            return outflow[ensemble].clone();
        } catch(Exception e){
            return null;
        }
    }

    public double getTotalOutflowInVeh(int ensemble) {
        try{
            return BeatsMath.sum(outflow[ensemble]);
        } catch(Exception e){
            return Double.NaN;
        }
    }

    public double getInflowInVeh(int ensemble,int vehicletype) {
        try{
            return inflow[ensemble][vehicletype];
        } catch(Exception e){
            return Double.NaN;
        }
    }

    public Double[] getInflowInVeh(int ensemble) {
        try{
            return inflow[ensemble].clone();
        } catch(Exception e){
            return null;
        }
    }

    public double getTotalInflowInVeh(int ensemble) {
        try{
            return BeatsMath.sum(inflow[ensemble]);
        } catch(Exception e){
            return Double.NaN;
        }
    }

    public double getOutflowInVeh(int ensemble,int vehicletype) {
        try{
            return outflow[ensemble][vehicletype];
        } catch(Exception e){
            return Double.NaN;
        }
    }

    public double computeDelayInVeh(int ensemble,int vt_index){
        return link_behavior.compute_delay_in_veh(ensemble, vt_index);
    }

    public double computeSpeedInMPS(int ensemble){
        return link_behavior.compute_speed_in_mps(ensemble);
    }

    public void overrideDensityWithVeh(Double[] x,int ensemble){
        link_behavior.set_density_in_veh(ensemble, x);
    }

    // Fundamental diagram ....................

	public double getDensityJamInVeh(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getDensityJamInVeh();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getDensityCriticalInVeh(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getDensityCriticalInVeh();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCapacityDropInVeh(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getCapacityDropInVeh();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCapacityInVeh(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getCapacityInVeh();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getDensityJamInVPMPL(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getDensityJamInVeh() / getLengthInMeters() / _lanes;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getDensityCriticalInVPMPL(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getDensityCriticalInVeh() / getLengthInMeters() / _lanes;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCapacityDropInVPSPL(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getCapacityDropInVeh() / myScenario.get.simdtinseconds() / _lanes;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCapacityInVPS(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getCapacityInVeh() / myScenario.get.simdtinseconds();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCapacityInVPSPL(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD._getCapacityInVeh() / myScenario.get.simdtinseconds() / _lanes;
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getNormalizedVf(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getVfNormalized();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getVfInMPS(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getVfNormalized() * getLengthInMeters() / myScenario.get.simdtinseconds();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getCriticalSpeedInMPS(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if (null == FD)
				return Double.NaN;
			else if (null != FD.getCriticalSpeed())
				return FD.getCriticalSpeed().doubleValue();
			else
				return getVfInMPS(ensemble);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getNormalizedW(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getWNormalized();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	public double getWInMPS(int ensemble) {
		try {
			FundamentalDiagram FD = currentFD(ensemble);
			if(FD==null)
				return Double.NaN;
			return FD.getWNormalized() * getLengthInMeters() / myScenario.get.simdtinseconds();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

    public DemandProfile getDemandProfile(){
        return myDemandProfile;
    }

    public FundamentalDiagramProfile getFundamentalDiagramProfile(){
        return myFDprofile;
    }

}
