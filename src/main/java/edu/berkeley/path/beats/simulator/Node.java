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
import edu.berkeley.path.beats.simulator.nodeBeahavior.NodeBehavior;
import edu.berkeley.path.beats.simulator.nodeBeahavior.Node_FlowSolver;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.List;

/** Node class.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public class Node extends edu.berkeley.path.beats.jaxb.Node implements Serializable {

    private static final long serialVersionUID = -4134801470575166497L;

    public SplitRatioLogger split_ratio_logger = null;

	// does not change ....................................
	protected Network myNetwork;

	// connectivity
    public int nIn;
    public int nOut;

    public boolean istrivialsplit;
	protected boolean isTerminal;

	// link references
	public Link [] output_link;
    public Link [] input_link;

	// split ratio from profile
	protected SplitRatioProfile my_profile;
	protected boolean has_profile;

    // node behavior
    public NodeBehavior node_behavior;

	// does change ........................................

    // split ratio from controller
    protected boolean has_controller;
    protected boolean has_controller_split;
    protected List<Splitratio> controller_splits;

    // selected split ratio
    private Double [][][] splitratio_nominal;
	
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	protected Node(){}

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
    
	protected void populate(Network myNetwork) {

        Scenario myScenario = myNetwork.getMyScenario();

    	// Note: It is assumed that this comes *before* SplitRatioProfile.populate

		this.myNetwork = myNetwork;
		
		nOut = 0;
		if(getOutputs()!=null){
			nOut = getOutputs().getOutput().size();
			output_link = new Link[nOut];
			for(int i=0;i<nOut;i++){
				edu.berkeley.path.beats.jaxb.Output output = getOutputs().getOutput().get(i);
				output_link[i] = myNetwork.getLinkWithId(output.getLinkId());
			}
		}

		nIn = 0;
        if(getInputs()!=null){
            nIn = getInputs().getInput().size();
            input_link = new Link[nIn];
            for(int i=0;i<nIn;i++){
                edu.berkeley.path.beats.jaxb.Input input = getInputs().getInput().get(i);
                input_link[i] = myNetwork.getLinkWithId(input.getLinkId());
            }
        }
		
		isTerminal = nOut==0 || nIn==0;
        istrivialsplit = nOut<=1;
        has_profile = false;

    	if(isTerminal)
    		return;

        if(!istrivialsplit && myScenario.split_logger_prefix!=null && !myScenario.split_logger_prefix.isEmpty())
            split_ratio_logger = new SplitRatioLogger(this);

		// initialize the split ratio matrix
		// NOTE: SHOULD THIS GO IN RESET?
		splitratio_nominal = BeatsMath.zeros(nIn,nOut,myScenario.get.numVehicleTypes());
        BeatsMath.normalize(splitratio_nominal);

		// default node flow and split solvers
//        node_behavior = new NodeBehavior(this,
//                                          new Node_SplitRatioSolver_Greedy(this) ,
//                                          new Node_FlowSolver_LNCTM(this) ,
//                                          new Node_SupplyPartitioner(this) );
	}
    
	protected void validate() {
				
		if(isTerminal)
			return;
		
		if(output_link!=null)
			for(Link link : output_link)
				if(link==null)
					BeatsErrorLog.addError("Incorrect output link ID in node ID=" + getId());

		if(input_link!=null)
			for(Link link : input_link)
				if(link==null)
					BeatsErrorLog.addError("Incorrect input link ID in node ID=" + getId());
		
		if(node_behavior.sr_solver !=null)
            node_behavior.sr_solver.validate();
    }
	
	protected void reset() {
    	if(isTerminal)
    		return;
        if(node_behavior.flow_solver!=null)
		    node_behavior.flow_solver.reset();
        if(node_behavior.sr_solver!=null)
            node_behavior.sr_solver.reset();
	}
	
	public void update_flows() {

        if(isTerminal)
            return;

        int e,i,j;
        Scenario myScenario = myNetwork.getMyScenario();
        int numEnsemble = myScenario.get.numEnsemble();
        int numVTypes = myScenario.get.numVehicleTypes();

        // Select a nominal split ratio from profile, event, or controller
        if(istrivialsplit) {
            // nominal is all ones
            splitratio_nominal = BeatsMath.ones(nIn, nOut, numVTypes);
        }
        else {
            if (has_profile)
				sample_split_ratio_profile();
            if (has_controller_split)
                override_splits(splitratio_nominal, controller_splits);
            //if(has_event_split)
            //    override_splits(splitratio_nominal,event_splits);
        }

        for(e=0;e<numEnsemble;e++){

            Double [][][] splitratio_perturbed;

            if(my_profile==null || istrivialsplit) {
                splitratio_perturbed = splitratio_nominal; //BeatsMath.nans(nIn,nOut,numVTypes);
            }
            else
            {
                if(!my_profile.isdeterministic() && my_profile.hasConcentrationParameters() ) //&& my_profile.isCurrentConcentrationParametersValid())
                    splitratio_perturbed = SplitRatioPerturber.sampleFromConcentrationParametersOnce(my_profile.getCurrentConcentration());
                else if(!my_profile.isdeterministic() && nOut==2 && nIn==1)
                    splitratio_perturbed = SplitRatioPerturber.perturb2OutputSplitOnce(splitratio_nominal, my_profile);
                else
                    splitratio_perturbed = splitratio_nominal;
            }

            // compute applied split ratio matrix
            Double [][][] splitratio_applied = istrivialsplit ?
                    splitratio_perturbed :
                    node_behavior.sr_solver.computeAppliedSplitRatio(splitratio_perturbed,e);

            /////////////////////////////////////////////////
            // write first to logger
            if(split_ratio_logger!=null && e==0)
                split_ratio_logger.write(splitratio_applied);
            /////////////////////////////////////////////////

            // compute node flows ..........................................
            Node_FlowSolver.IOFlow IOflow = node_behavior.flow_solver.computeLinkFlows(splitratio_applied,e);

            if(IOflow==null)
                return;

            // assign flow to input links ..................................
            for(i=0;i<nIn;i++)
                input_link[i].setOutflow(e,IOflow.getIn(i));

            // assign flow to output links .................................
            for (j=0;j<nOut;j++)
                output_link[j].setInflow(e,IOflow.getOut(j));

        }

	}

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

    // split ratio profile ..............................................

	protected void setMySplitRatioProfile(SplitRatioProfile mySplitRatioProfile) {
		this.my_profile = mySplitRatioProfile;
		if(!istrivialsplit){
			this.has_profile = true;
		}
	}

    public double getSplitRatioProfileValue(int i,int j,int k){
        if(my_profile==null || my_profile.getCurrentSplitRatio()==null)
            return Double.NaN;
        return my_profile.getSplit(i,j,k);
    }

	public void sample_split_ratio_profile() {
		splitratio_nominal = my_profile.getCurrentSplitRatio();
	}

	// controllers ......................................................

	protected void set_controller_split(List<Splitratio> cs){
        if(!has_controller)
            return;
        controller_splits = cs;
        has_controller_split = true;
    }

    protected void deactivate_split_control(){
        if(!has_controller)
            return;
        controller_splits = null;
        has_controller_split = false;
    }

	public boolean register_split_controller(){
		if(has_controller)		// used to detect multiple controllers
			return false;
		else{
			has_controller = true;
			return true;
		}
	}
	
	// events ..........................................................

	// used by Event.setNodeEventSplitRatio
//	protected void applyEventSplitRatio(Double3DMatrix x) {
//		splitratio_selected.copydata(x);
//		normalizeSplitRatioMatrix(splitratio_selected);
//		hasactivesplitevent = true;
//	}

	// used by Event.revertNodeEventSplitRatio
//	protected void removeEventSplitRatio() {
//		hasactivesplitevent = false;
//	}
	
	// used by Event.revertNodeEventSplitRatio
//	protected boolean isHasActiveSplitEvent() {
//		return hasactivesplitevent;
//	}

	// used by Event.setNodeEventSplitRatio
//    protected Double3DMatrix getSplitratio() {
//		return splitratio_selected;
//	}
    
	/////////////////////////////////////////////////////////////////////
	// operations on split ratio matrices
	/////////////////////////////////////////////////////////////////////


//    public Double3DMatrix[] select_and_perturb_split_ratio(){
//
//    	int numVTypes = getMyNetwork().getMyScenario().get.numVehicleTypes();
//
//        // Select a split ratio from profile, event, or controller
//        if(istrivialsplit)
//            splitratio_selected = new Double3DMatrix(getnIn(),getnOut(),numVTypes,1d);
//        else{
//            if(has_profile)
//                splitratio_selected = new Double3DMatrix(my_profile.getCurrentSplitRatio());
//            if(has_controller_split)
//                splitratio_selected.override_splits(this,controller_splits);
////        if(has_event_split)
////            splitratio_selected.override_splits(event_splits);
//        }
//
//        // perturb split ratio
//        int numEnsemble = myNetwork.getMyScenario().get.numEnsemble();
//        Double3DMatrix[] splitratio_selected_perturbed = new Double3DMatrix[numEnsemble];
//        if(!isdeterministic && my_profile.hasConcentrationParameters()
//        		&& my_profile.isCurrentConcentrationParametersValid())
//        	splitratio_selected_perturbed = SplitRatioPerturber.sampleFromConcentrationParameters(my_profile.getCurrentConcentrationParameters(), numEnsemble);
//        else if(!isdeterministic && nOut==2 && nIn==1)
//            splitratio_selected_perturbed = SplitRatioPerturber.perturb2OutputSplit(splitratio_selected, my_profile, numEnsemble);
//        else
//            Arrays.fill(splitratio_selected_perturbed, 0, splitratio_selected_perturbed.length, splitratio_selected);
//
//        return splitratio_selected_perturbed;
//
//    }


//	protected boolean validateSplitRatioMatrix(Double3DMatrix X){
//
//		int i,j,k;
//		double value;
//
//		// dimension
//		if(X.getnIn()!=nIn || X.getnOut()!=nOut || X.getnVTypes()!=myNetwork.getMyScenario().get.numVehicleTypes()){
//			BeatsErrorLog.addError("Split ratio for node " + getId() + " has incorrect dimensions.");
//			return false;
//		}
//
//		// range
//		for(i=0;i<X.getnIn();i++){
//			for(j=0;j<X.getnOut();j++){
//				for(k=0;k<X.getnVTypes();k++){
//					value = X.get(i,j,k);
//					if( !Double.isNaN(value) && (value>1 || value<0) ){
//						BeatsErrorLog.addError("Invalid split ratio values for node ID=" + getId());
//						return false;
//					}
//				}
//			}
//		}
//		return true;
//	}
//
//    protected void normalizeSplitRatioMatrix(Double3DMatrix X){
//
//    	int i,j,k;
//		boolean hasNaN;
//		int countNaN;
//		int idxNegative;
//		double sum;
//
//    	for(i=0;i<X.getnIn();i++)
//    		for(k=0;k<myNetwork.getMyScenario().get.numVehicleTypes();k++){
//				hasNaN = false;
//				countNaN = 0;
//				idxNegative = -1;
//				sum = 0.0f;
//				for (j = 0; j < X.getnOut(); j++)
//					if (Double.isNaN(X.get(i,j,k))) {
//						countNaN++;
//						idxNegative = j;
//						if (countNaN > 1)
//							hasNaN = true;
//					}
//					else
//						sum += X.get(i,j,k);
//
//				if (countNaN==1) {
//					X.set(i,idxNegative,k,Math.max(0f, (1-sum)));
//					sum += X.get(i,idxNegative,k);
//				}
//
//				if ( !hasNaN && BeatsMath.equals(sum, 0.0) ) {
//					X.set(i,0,k,1d);
//					//for (j=0; j<n2; j++)
//					//	data[i][j][k] = 1/((double) n2);
//					continue;
//				}
//
//				if ((!hasNaN) && (sum<1.0)) {
//					for (j=0;j<X.getnOut();j++)
//						X.set(i,j,k,(double) (1/sum) * X.get(i,j,k));
//					continue;
//				}
//
//				if (sum >= 1.0)
//					for (j=0; j<X.getnOut(); j++)
//						if (Double.isNaN(X.get(i,j,k)))
//							X.set(i,j,k,0d);
//						else
//							X.set(i,j,k,(double) (1/sum) * X.get(i,j,k));
//    		}
//    }
    
	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

    public boolean isTerminal() {
		return isTerminal;
	}

	/** network that containts this node */ 	
	public Network getMyNetwork() {
		return myNetwork;
	}
	    
	/** List of links exiting this node */ 
    public Link[] getOutput_link() {
		return output_link;
	}

    /** List of links entering this node */ 
	public Link[] getInput_link() {
		return input_link;
	}

    /** Index of link with given ID in the list of input links of this node */
	public int getInputLinkIndex(long id){
		for(int i=0;i<getnIn();i++){
			if(input_link[i]!=null)
				if(input_link[i].getId()==id)
					return i;
		}
		return -1;
	}
	
    /** Index of link with given ID in the list of output links of this node */
	public int getOutputLinkIndex(long id){
		for(int i=0;i<getnOut();i++){
			if(output_link[i]!=null)
				if(output_link[i].getId()==id)
					return i;
		}
		return -1;
	}
	
    /** Number of links entering this node */ 
	public int getnIn() {
		return nIn;
	}

    /** Number of links exiting this node */ 
	public int getnOut() {
		return nOut;
	}

    public Double [][][] getSplitRatio(){
        return splitratio_nominal;
    }

    public Double getSplitRatio(int in,int out){
        return BeatsMath.sum(splitratio_nominal[in][out]);
    }

//	/**
//	 * Retrieves a split ratio for the given input/output link pair and vehicle type
//	 * @param inLinkInd input link index
//	 * @param outLinkInd output link index
//	 * @param vehTypeInd vehicle type index
//	 * @return the split ratio
//	 */
	public double getSplitRatio(int inLinkInd, int outLinkInd, int vehTypeInd) {
        try{
            return splitratio_nominal[inLinkInd][outLinkInd][vehTypeInd];
        } catch(Exception e) {
            return Double.NaN;
        }
	}

    public double [] getInputLinkPriorities(int ensembleIndex) {
        double [] priorities = new double[nIn];
        for(int i=0;i<nIn;i++){
            priorities[i] = input_link[i].getPriority(ensembleIndex);
        }
        return priorities;
    }

    public SplitRatioProfile getSplitRatioProfile(){
        return my_profile;
    }

    public boolean istrivialsplit() {
        return istrivialsplit;
    }

//    public static double[][][] perturb2DSplitForTest(double[][][] splitData){
//    	// implemented this way because Double3DMatrix is not public so not viewable in java/test/ package
//    	Double3DMatrix split = new Double3DMatrix(splitData);
//        JaxbObjectFactory factory = new JaxbObjectFactory();
//        SplitRatioProfile profile = (SplitRatioProfile) factory.createSplitRatioProfile();
//        profile.setVariance(.03);
//    	Double3DMatrix[] perturbedSplit = SplitRatioPerturber.perturb2OutputSplit(split, profile, 1);
//    	return perturbedSplit[0].cloneData();
//    }

    private void override_splits(Double [][][] data,List<Splitratio> srs){

        for(Splitratio sr : srs){
            int in_index = this.getInputLinkIndex(sr.getLinkIn());
            int out_index = this.getOutputLinkIndex(sr.getLinkOut());
            int vt_index = this.getMyNetwork().getMyScenario().get.vehicleTypeIndexForId(sr.getVehicleTypeId());

            if(in_index<0 || out_index<0 || vt_index<0)
                continue;

            Double val;
            try{
                val = Double.parseDouble(sr.getContent());
            } catch( NumberFormatException e){
                val = Double.NaN;
            }
            if(val.isNaN())
                continue;

            data[in_index][out_index][vt_index] = val;
        }

    }
}
