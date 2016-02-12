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
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileDouble1D;

import java.io.Serializable;
import java.util.ArrayList;

final public class DemandProfile extends edu.berkeley.path.beats.jaxb.DemandProfile implements Serializable {

    private static final long serialVersionUID = 6110764422329397207L;
    // does not change ....................................
    protected boolean isSinkDemand;
    private TypeUncertainty uncertaintyModel;
	private boolean isOrphan;
    private BeatsTimeProfileDouble1D demand_nominal;	// [veh] demand profile per vehicle type
	private double std_dev_add;				// [veh]
	private double std_dev_mult;			// [veh]
	private boolean isdeterministic;		// true if the profile is deterministic
    private boolean doknob;

	// does change ........................................
    private Double [][] current_sample_noisy_knobbed;
	public double _knob;

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	public void set_knob(double _knob) {
        if(Double.isNaN(_knob))
            return;

        this._knob = Math.max(_knob,0.0);
        this.doknob = !BeatsMath.equals(_knob,1d);

        // resample the profile
        update(true,null);
	}

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(Scenario myScenario) {

        // required
        Link myLink = myScenario.get.linkWithId(getLinkIdOrg());
        isOrphan = myLink==null;
        isSinkDemand = myLink.isSink();

		if(isOrphan || isSinkDemand)
			return;
		
		// attach to link
		myLink.myDemandProfile = this;

        // collect information needed to create the profile
        ArrayList<Integer> vehicle_type_index = new ArrayList<Integer>();
        ArrayList<String> demandStr = new ArrayList<String>();
        for(edu.berkeley.path.beats.jaxb.Demand d : getDemand()){
            vehicle_type_index.add(myScenario.get.vehicleTypeIndexForId(d.getVehicleTypeId()) );
            demandStr.add(d.getContent());
        }

        // create the profile
        demand_nominal = new BeatsTimeProfileDouble1D(
                vehicle_type_index,
                demandStr,
                myScenario.get.numVehicleTypes(),
                getDt(),
                getStartTime(),
                myScenario.get.simdtinseconds());

        // optional uncertainty model
        if(getStdDevAdd()!=null)
            std_dev_add = getStdDevAdd().doubleValue() * myScenario.get.simdtinseconds();
        else
            std_dev_add = Double.POSITIVE_INFINITY;		// so that the other will always win the min

        if(getStdDevMult()!=null)
//			std_dev_mult = getStdDevMult().doubleValue() * myScenario.getSimdtinseconds();
            std_dev_mult = getStdDevMult().doubleValue();
        else
            std_dev_mult = Double.POSITIVE_INFINITY;	// so that the other will always win the min

        isdeterministic = (getStdDevAdd()==null || std_dev_add==0.0) &&
                (getStdDevMult()==null || std_dev_mult==0.0);

        _knob = getKnob();
        doknob = !BeatsMath.equals(_knob,1d);
        current_sample_noisy_knobbed = BeatsMath.zeros(myScenario.get.numEnsemble(),myScenario.get.numVehicleTypes());
        uncertaintyModel = myScenario.get.uncertaintyModel();
    }

	protected void validate() {

        // sink demands get validated in the controller
        if(isSinkDemand)
            return;

		if(demand_nominal==null || demand_nominal.isEmpty()){
			BeatsErrorLog.addWarning("Demand profile ID=" + getId() + " has no data.");
			return;
		}
		
		if(isOrphan){
			BeatsErrorLog.addWarning("Bad origin link ID=" + getLinkIdOrg() + " in demand profile.");
			return;
		}

//        demand_nominal.validate(simdt);
	}

	protected void reset() {
		
		if(isOrphan || isSinkDemand)
			return;

        demand_nominal.reset();
        _knob = getKnob();
        doknob = BeatsMath.equals(_knob,1d);
	}
	
	protected void update(boolean noiseknobonly,Clock clock) {
		
		if(isOrphan || isSinkDemand)
			return;
		
		if(demand_nominal==null || demand_nominal.isEmpty())
			return;

        boolean sample_changed = noiseknobonly ? false : demand_nominal.sample(false,clock);

        if( noiseknobonly || sample_changed ){

            // get current sample
            Double [] current_sample = demand_nominal.getCurrentSample();

            int e,v;

            // add noise
            if(isdeterministic) {
                for (e = 0; e < current_sample_noisy_knobbed.length; e++)
                    current_sample_noisy_knobbed[e] = current_sample;
            }
            else {
                for (e = 0; e < current_sample_noisy_knobbed.length; e++)
                    for (v = 0; v < current_sample_noisy_knobbed[e].length; v++)
                        current_sample_noisy_knobbed[e][v] = addNoise(current_sample[v], uncertaintyModel);
            }

            // apply knob
            if(doknob)
                for(e=0;e<current_sample_noisy_knobbed.length;e++)
                    for(v=0;v<current_sample_noisy_knobbed[e].length;v++)
                        current_sample_noisy_knobbed[e][v] = _knob*current_sample_noisy_knobbed[e][v];
        }

	}
	
	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////

    private double addNoise(final double demandvalue,TypeUncertainty uncertaintyModel){

        double noisy_demand = demandvalue;

        // use smallest between multiplicative and additive standard deviations
        double std_dev_apply = Double.isInfinite(std_dev_mult) ? std_dev_add :
                Math.min( noisy_demand*std_dev_mult , std_dev_add );

        // sample the distribution
        switch(uncertaintyModel){

            case uniform:
                noisy_demand += BeatsMath.sampleZeroMeanUniform(std_dev_apply);
                break;

            case gaussian:
                noisy_demand += BeatsMath.sampleZeroMeanGaussian(std_dev_apply);
                break;
        }

        // non-negativity
        noisy_demand = Math.max(0.0,noisy_demand);

        return noisy_demand;

    }

	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////

	public Double [] getCurrentValue(int e){
        return current_sample_noisy_knobbed==null ? null : current_sample_noisy_knobbed[e];
	}

    public Double [] predict_in_VPS(int vehicle_type_index, double begin_time, double time_step, int num_steps,double simdtinseconds){

        Double [] val = BeatsMath.zeros(num_steps);

        if(demand_nominal==null || demand_nominal.isEmpty())
            return val;

        for(int i=0;i<num_steps;i++){

            // time in seconds after midnight
            double time = begin_time + i*time_step + 0.5*time_step;

            // corresponding profile step
            int step = BeatsMath.floor( (time-getStartTime())/getDt().floatValue() );
            if(step>=0){
                Double [] d =  demand_nominal.get(step);
                val[i] = d[vehicle_type_index];
                val[i] *= _knob;
                val[i] /= simdtinseconds;
            }

        }
        return val;
    }


}
