package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matt on 9/23/15.
 * Split ratio assignment algorithm described in Section 4 of Wright, Gomes, Horowitz and Kurzhanskiy,
 * "A new model for multi-commodity macroscopic modeling of complex traffic networks"
 */
public class Node_SplitRatioSolver_Balancing extends Node_SplitRatioSolver  implements Serializable {

	private static final long serialVersionUID = -7517471691845897550L;
	protected Double [][][] computed_splitratio; //nIn x nOut x nVType
	protected int nVType;
	protected Double [][] demands;
	protected double [] inputPriorities;

	protected boolean [][][] splitKnown;
	protected Double [][] splitRemaining; // nIn x nVType

	protected List<Integer>[] U_j; // links i that wish to send to link j
	protected List<Integer>[] U_j_tilde;
	protected List<Integer>[][] V_ic; // links j to which (i,c) has undefined split ratios
	protected List<Integer> V_tilde; // links j to which split may still be assigned

	protected double[][] unallocated_demand;
	protected double[][][] oriented_demand;
	protected double[][] oriented_priority;
	protected double[][] dsratio;
	protected int[] max_dsratio_index;
	protected int[] min_dsratio_index;
	protected int min_demanded_output_link; // j^-
	protected int min_oriented_DSratio_i; // i^-
	protected int min_oriented_DSratio_c; //c^-
	protected double largest_oriented_dsratio;
	protected double smallest_oriented_dsratio;

	private final double zeroThreshold = Double.MIN_VALUE * 2;

	public Node_SplitRatioSolver_Balancing(Node myNode) {
		super(myNode);
	}

	@Override
	public void validate() {

	}

	@Override
	public void reset() {
		computed_splitratio = new Double[myNode.nIn][myNode.nOut][nVType];
		nVType = myNode.getMyNetwork().getMyScenario().get.numVehicleTypes();
		demands = new Double[myNode.nIn][nVType];
		splitKnown = new boolean[myNode.nIn][myNode.nOut][nVType];
		splitRemaining = new Double[myNode.nIn][nVType];

		U_j = (ArrayList<Integer>[])new ArrayList[myNode.nOut];
		U_j_tilde = (ArrayList<Integer>[])new ArrayList[myNode.nOut];
		V_ic = (ArrayList<Integer>[][])new ArrayList[myNode.nIn][nVType];

		unallocated_demand = new double[myNode.nIn][nVType];
		oriented_demand = new double[myNode.nIn][myNode.nOut][nVType];
		oriented_priority = new double[myNode.nIn][myNode.nOut];
		dsratio = new double[myNode.nIn][myNode.nOut];
		max_dsratio_index = new int[2];
		min_dsratio_index = new int[2];
	}

	@Override
	public Double [][][] computeAppliedSplitRatio(final Double [][][] splitratio_selected,final int e) {

		// get link information
		inputPriorities = myNode.getInputLinkPriorities(e);
		for(int i=0;i<myNode.getnIn();i++){
			demands[i] = myNode.getInput_link()[i].get_out_demand_in_veh(e);
		}

		initializeAssignedSplits(splitratio_selected);
		assignZeroDemandSplits();
		assignZeroRemainingSplits();

		initializeSets();
		regularizePriorities();

		while(!allSplitsSolved()) {
			calculateRemainingDemand();
			calculateOrientedDemand();
			calculateOrientedPriorities(splitratio_selected);
			computeDSRatios(e);
			findMinimumDemandedOutputLink(e);
			findMinimumOrientedDSRatio(e);
			distributeSplitRatio(e);
			updateSets();
		}

		return computed_splitratio;
	}

	private void initializeAssignedSplits(final Double[][][] input_splitratio) {
		for(int i=0;i<myNode.nIn;i++) {
			for(int c=0;c<nVType;c++) {
				// initialize counter of how much split remains to be assigned assignment
				splitRemaining[i][c] = 1d;

				for(int j=0;j<myNode.nOut;j++) {

					if (Double.isNaN(input_splitratio[i][j][c])) {
						splitKnown[i][j][c] = false;
						computed_splitratio[i][j][c] = 0d;
					}
					else {
						splitKnown[i][j][c] = true;
						splitRemaining[i][c] = splitRemaining[i][c] - input_splitratio[i][j][c];
						computed_splitratio[i][j][c] = input_splitratio[i][j][c];
					}
				}
			}
		}
	}

	private void assignZeroDemandSplits() {
		for (int i=0;i<myNode.nIn;i++){
			for (int c=0;c<nVType;c++){
				if (demands[i][c] < zeroThreshold) { // splits may be assigned arbitrarily - assign 1 to first j
					computed_splitratio[i][0][c] = 1d;
					splitRemaining[i][c] = 0d;
					splitKnown[i][0][c] = true;

					for(int j=1;j<myNode.nOut;j++){
						computed_splitratio[i][j][c] = 0d;
						splitKnown[i][j][c] = true;
					}
				}
			}
		}
	}

	private void assignZeroRemainingSplits() {
		int i,j,c;
		for(i=0;i<myNode.nIn;i++){
			for(c=0;c<nVType;c++){
				if (splitRemaining[i][c] < zeroThreshold) {
					for(j=0;j<myNode.nOut;j++) {
						if(!splitKnown[i][j][c]) {
							splitKnown[i][j][c] = true;
							computed_splitratio[i][j][c] = 0d;
						}
					}
				}
			}
		}
	}

	private void initializeSets() {
		int i,j,c;

		V_tilde = new ArrayList<Integer>(myNode.nOut);

		for(j=0;j<myNode.nOut;j++){
			U_j[j] = new ArrayList<Integer>(myNode.nIn);
			U_j_tilde[j] = new ArrayList<Integer>(myNode.nIn);

			outerloop1:
			for(i=0;i<myNode.nIn;i++){
				for(c=0;c<nVType;c++){
					if (!splitKnown[i][j][c]) {
						U_j[j].add(i);
						U_j_tilde[j].add(i);
						continue outerloop1;
					}
				}
			}
			if(!U_j[j].isEmpty())
				V_tilde.add(j);
		}

		for(i=0;i<myNode.nIn;i++){
			for(c=0;c<nVType;c++){
				V_ic[i][c] = new ArrayList<Integer>(myNode.nOut);
				for(j=0;j<myNode.nOut;j++){
					if (U_j[j].contains(i))
						V_ic[i][c].add(j);
				}
				if (V_ic[i][c].size()==1) {
					j = V_ic[i][c].get(0);
					splitKnown[i][j][c] = true;
					computed_splitratio[i][j][c] = splitRemaining[i][c];
					splitRemaining[i][c] = 0d;
					V_ic[i][c].clear();
				}
			}
		}
	}

	private void regularizePriorities() {
		int numZeroPriority = 0;
		int i;
		for (i = 0; i < myNode.nIn; i++) {
			if (inputPriorities[i] <  zeroThreshold)
				numZeroPriority++;
		}
		if (numZeroPriority==0)
			return;

		for (i = 0; i <myNode.nIn; i++) {
			inputPriorities[i] = inputPriorities[i] * (myNode.nIn - numZeroPriority) / numZeroPriority
					+ numZeroPriority / (Math.pow(myNode.nIn,2));
		}
	}

	private boolean allSplitsSolved() {
		return V_tilde.isEmpty();
	}

	private void calculateRemainingDemand() {
		for (int i = 0; i < myNode.nIn; i++) {
			for (int c=0; c < nVType; c++) {
				unallocated_demand[i][c] = splitRemaining[i][c] * demands[i][c];
			}
		}
	}

	private void calculateOrientedDemand() {
		for (int i = 0; i < myNode.nIn; i++) {
			for (int j=0; j<myNode.nOut; j++) {
				for (int c=0; c < nVType; c++) {
					oriented_demand[i][j][c] = computed_splitratio[i][j][c] * demands[i][c];
				}
			}
		}
	}

	private void calculateOrientedPriorities(final Double[][][] input_splitratio) {
		double[] gamma = new double[nVType];
		double[] numerator = new double[nVType];
		for (int i = 0; i<myNode.nIn; i++) {
			for( int j=0; j<myNode.nOut; j++) {
				for( int c=0; c<nVType; c++) {
					if(Math.abs(demands[i][c]) < zeroThreshold) {
						gamma[c] = 0d;
					}
					else if(!Double.isNaN(input_splitratio[i][j][c])) {
						gamma[c] = input_splitratio[i][j][c];
					}
					else {
						gamma[c] = computed_splitratio[i][j][c] + splitRemaining[i][c] / V_ic[i][c].size();
					}

					numerator[c] = gamma[c] * demands[i][c];
				}

				oriented_priority[i][j] = inputPriorities[i] * BeatsMath.sum(numerator) / BeatsMath.sum(demands[i]);
			}
		}
	}

	private void computeDSRatios(int e) {
		double numerator, denominator, sum_of_priorities_Uj;
		max_dsratio_index[0] = 0; max_dsratio_index[1] = 0;
		min_dsratio_index[0] = 0; min_dsratio_index[1] = 0;
		smallest_oriented_dsratio = Double.POSITIVE_INFINITY;
		largest_oriented_dsratio = Double.NEGATIVE_INFINITY;
		for(int j=0;j<myNode.nOut;j++) {
			sum_of_priorities_Uj = 0;
			for(int iprime : U_j[j]) {
				sum_of_priorities_Uj += oriented_priority[iprime][j];
			}
			for(int i=0;i<myNode.nIn;i++) {
				numerator = BeatsMath.sum(oriented_demand[i][j]);
				if(sum_of_priorities_Uj * numerator < zeroThreshold) {
					dsratio[i][j] = 0d;
				} else {
					denominator = oriented_priority[i][j] * myNode.getOutput_link()[j].get_available_space_supply_in_veh(e);
					dsratio[i][j] = sum_of_priorities_Uj * numerator / denominator;
				}

				if( dsratio[i][j] > largest_oriented_dsratio) {
					max_dsratio_index[0] = i; max_dsratio_index[1] = j;
					largest_oriented_dsratio = dsratio[i][j];
				}
				else if( dsratio[i][j] < smallest_oriented_dsratio) {
					min_dsratio_index[0] = i; min_dsratio_index[1] = j;
					smallest_oriented_dsratio = dsratio[i][j];
				}
			}
		}
	}

	private void findMinimumDemandedOutputLink(int e) {
		ArrayList<Integer> set_of_output_links_with_min_dsratio = new ArrayList<Integer>(myNode.nOut);
		for(int j : V_tilde) {
			for(int i : U_j_tilde[j]) {
				if( Math.abs(dsratio[i][j] - smallest_oriented_dsratio) <= zeroThreshold
						&& !set_of_output_links_with_min_dsratio.contains(j))
					set_of_output_links_with_min_dsratio.add(j);
			}
		}
		min_demanded_output_link = 0;
		double numerator,fraction;
		double min_fraction = Double.POSITIVE_INFINITY;
		for(int jprime : set_of_output_links_with_min_dsratio ) {
			numerator = 0;
			for(int i=0;i<myNode.nIn;i++) {
				numerator += BeatsMath.sum(oriented_demand[i][jprime]);
			}
			fraction = numerator / myNode.getOutput_link()[jprime].get_available_space_supply_in_veh(e);
			if( fraction < min_fraction ) {
				min_demanded_output_link = jprime;
				min_fraction = fraction;
			}
		}
	}

	private void findMinimumOrientedDSRatio(int e) {
		double sum_of_priorities_Uj = 0;
		for(int i : U_j[min_demanded_output_link])
			sum_of_priorities_Uj += oriented_priority[i][min_demanded_output_link];

		ArrayList<Integer> set_of_input_links_with_min_oriented_dsratio = new ArrayList<Integer>(myNode.nIn);
		double numerator,denominator,fraction;
		double min_fraction = Double.POSITIVE_INFINITY;
		for(int i : U_j_tilde[min_demanded_output_link]) {
			numerator = BeatsMath.sum(oriented_demand[i][min_demanded_output_link]);
			denominator = oriented_priority[i][min_demanded_output_link] *
					myNode.getOutput_link()[min_demanded_output_link].get_available_space_supply_in_veh(e);
			fraction = sum_of_priorities_Uj * numerator / denominator;
			if( fraction < min_fraction + zeroThreshold)
				set_of_input_links_with_min_oriented_dsratio.add(i);
		}
		double min_remaining_allocated_demand = Double.POSITIVE_INFINITY;
		min_oriented_DSratio_c = 0;
		min_oriented_DSratio_i = 0;
		for(int i : set_of_input_links_with_min_oriented_dsratio) {
			for(int c=0;c<nVType;c++) {
				if( unallocated_demand[i][c] < min_remaining_allocated_demand && splitRemaining[i][c] > 0) {
					min_oriented_DSratio_i = i;
					min_oriented_DSratio_c = c;
					min_remaining_allocated_demand = unallocated_demand[i][c];
				}
			}
		}
		smallest_oriented_dsratio = dsratio[min_oriented_DSratio_i][min_demanded_output_link];
	}

	private void distributeSplitRatio(int e) {
		double numerator,denominator,delta_split;
		if( Math.abs(largest_oriented_dsratio- smallest_oriented_dsratio) < zeroThreshold) {
			denominator = 0;
			for( int j : V_ic[min_oriented_DSratio_i][min_oriented_DSratio_c]) {
				denominator += oriented_priority[min_oriented_DSratio_i][j] *
						myNode.getOutput_link()[j].get_available_space_supply_in_veh(e);
			}
			for( int j : V_ic[min_oriented_DSratio_i][min_oriented_DSratio_c]) {
				numerator = oriented_priority[min_oriented_DSratio_i][j] *
						myNode.getOutput_link()[j].get_available_space_supply_in_veh(e);
				computed_splitratio[min_oriented_DSratio_i][j][min_oriented_DSratio_c] +=
						splitRemaining[min_oriented_DSratio_i][min_oriented_DSratio_c] * numerator / denominator;
			}
			splitRemaining[min_oriented_DSratio_i][min_oriented_DSratio_c] = 0d;
		}
		else {
			delta_split = compute_delta_split(e);
			computed_splitratio[min_oriented_DSratio_i][min_demanded_output_link][min_oriented_DSratio_c] += delta_split;
			splitRemaining[min_oriented_DSratio_i][min_oriented_DSratio_c] -= delta_split;
		}
	}

	private double compute_delta_split(int e) {
		double LHdenominator = 0d;
		for( int i : U_j[min_demanded_output_link])
			LHdenominator += oriented_priority[i][min_demanded_output_link];
		LHdenominator *= unallocated_demand[min_oriented_DSratio_i][min_oriented_DSratio_c];
		double LHfraction = largest_oriented_dsratio * oriented_priority[min_oriented_DSratio_i][min_demanded_output_link]
				* myNode.getOutput_link()[min_demanded_output_link].get_available_space_supply_in_veh(e) /
				LHdenominator;
		double RHfraction = BeatsMath.sum(oriented_demand[min_oriented_DSratio_i][min_demanded_output_link]) /
				unallocated_demand[min_oriented_DSratio_i][min_oriented_DSratio_c];

		return Math.min(splitRemaining[min_oriented_DSratio_i][min_oriented_DSratio_c], LHfraction - RHfraction);
	}

	private void updateSets() {
		for( int i=0;i<myNode.nIn;i++) {
			for (int j : V_tilde) {
				if (U_j_tilde[j].contains(i) && isThisInputLinkOutOfSplit(i))
					U_j_tilde[j].remove(U_j_tilde[j].indexOf(i)); // remove expects the index of the object
				// to be removed when you pass it an int
			}
		}

		for( int j=0;j<myNode.nOut;j++) {
			if( U_j_tilde[j].isEmpty())
				V_tilde.remove(V_tilde.indexOf(j));
		}
	}

	private boolean isThisInputLinkOutOfSplit(int i) {
		for( int c=0;c<nVType;c++) {
			if (Math.abs(splitRemaining[i][c]) > zeroThreshold)
				return false;
		}
		return true;
	}

	private ArrayList<Integer> makeTuple(int i, int j, int c) {
		ArrayList<Integer> output = new ArrayList<Integer>(3);
		output.add(0,i);
		output.add(1,j);
		output.add(2,c);
		return output;
	}

}
