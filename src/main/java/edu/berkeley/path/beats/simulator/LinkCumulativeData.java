package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

/**
 * Link cumulative data storage
 */
final public class LinkCumulativeData implements Serializable {

	private static final long serialVersionUID = 1446092154302320409L;

	private edu.berkeley.path.beats.simulator.Link link;
	private int nensemble;
	private int nvehtype;
	private Double[][] density;		// [veh]
	private Double[][] iflow;		// [veh]
	private Double[][] oflow;		// [veh]
	private int nsteps;

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

	LinkCumulativeData(edu.berkeley.path.beats.simulator.Link link) {
		this.link = link;
		nensemble = link.myScenario.get.numEnsemble();
		nvehtype = link.myScenario.get.numVehicleTypes();
		density = BeatsMath.zeros(nensemble,nvehtype);
		iflow = BeatsMath.zeros(nensemble,nvehtype);
		oflow = BeatsMath.zeros(nensemble,nvehtype);
		reset();
	}

	/////////////////////////////////////////////////////////////////////
	// update / reset
	/////////////////////////////////////////////////////////////////////
	
	void update() throws BeatsException {
		for (int i = 0; i < nensemble; ++i)
			for (int j = 0; j < nvehtype; ++j) {
				density[i][j] += link.getDensityInVeh(i, j);
				iflow[i][j] += link.getInflowInVeh(i, j);
				oflow[i][j] += link.getOutflowInVeh(i, j);
			}
		++nsteps;
	}

	void reset() {
		reset(density);
		reset(iflow);
		reset(oflow);
		nsteps = 0;
	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////
	
	// densities ........................................................
	
	// average density over the output period for a given ensemble and vehicle type
	public Double getMeanDensityInVeh(int ensemble, int vehtypenum) {
		return 0 == nsteps ? Double.NaN : density[ensemble][vehtypenum] / nsteps;
	}

	// average density[vehicle_type] over the output period for a given ensemble
	public Double[] getMeanDensityInVeh(int ensemble) {
		return 0 == nsteps ? BeatsMath.zeros(nvehtype) : BeatsMath.times(density[ensemble],1.0d / nsteps);
	}
	
	// average density over the output period for a given ensemble and all vehicle types
	public Double getMeanTotalDensityInVeh(int ensemble) {
		return 0 == nsteps ? Double.NaN : sum(density[ensemble]) / nsteps;
	}

	// inflow ........................................................

	// inflow accumulated over the output period for a given ensemble and vehicle type
	public Double getCumulativeInputFlowInVeh(int ensemble, int vehtypenum) {
		return iflow[ensemble][vehtypenum];
	}

	// inflow[vehicle_type] accumulated over the output period for a given ensemble
	public Double[] getCumulativeInputFlowInveh(int ensemble) {
		return iflow[ensemble];
	}

	// total inflow accumulated over the output period for a given ensemble
	public Double getCumulativeTotalInputFlowInVeh(int ensemble) {
		return sum(iflow[ensemble]);
	}

	// outflow ........................................................

	// outflow accumulated over the output period for a given ensemble and vehicle type
	public Double getCumulativeOutputFlowInVeh(int ensemble, int vehtypenum) {
		return oflow[ensemble][vehtypenum];
	}

	// outflow[vehicle_type] accumulated over the output period for a given ensemble
	public Double[] getCumulativeOutputFlowInVeh(int ensemble) {
		return oflow[ensemble];
	}
	
	// total outflow accumulated over the output period for a given ensemble
	public Double getCumulativeTotalOutputFlowInVeh(int ensemble) {
		return sum(oflow[ensemble]);
	}

	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	private static void reset(Double[][] matrix) {
		for (int i = 0; i < matrix.length; ++i)
			for (int j = 0; j < matrix[i].length; ++j)
				matrix[i][j] = 0.0d;
	}

	private static double sum(Double[] vector) {
		double sum = 0.0d;
		for (double val : vector)
			sum += val;
		return sum;
	}

}

