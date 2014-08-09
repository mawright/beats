package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.simulator.Node_FlowSolver.SupplyDemand;

public class Node_SplitRatioSolver_HAMBURGER extends Node_SplitRatioSolver {
	
	/* Fields */
	private double threshold;
    private double scaling_factor;
    private int fwy_id = -1;
    private int off_ramp_id = -1;

	/* Constructor */
	public Node_SplitRatioSolver_HAMBURGER(Node myNode) {
		super(myNode);
		
		for (int p = 0 ; p < myNode.getNodeType().getParameters().getParameter().size() ; p++)
		{
			// Assign threshold value.
			if (myNode.getNodeType().getParameters().getParameter().get(p).getName().equals("threshold"))
			{
				this.threshold = Double.parseDouble(myNode.getNodeType().getParameters().getParameter().get(p).getValue());
			}
			
			// Assign scaling_factor.
			if (myNode.getNodeType().getParameters().getParameter().get(p).getName().equals("scaling_factor"))
			{
				this.scaling_factor = Double.parseDouble(myNode.getNodeType().getParameters().getParameter().get(p).getValue());
			}
		}
		// Find diverging link
		for (int n = 0 ; n < myNode.output_link.length ; n++)
		{
			// Check for link type Freeway (id == 1)
			if (myNode.output_link[n].getLinkType().getName().equals("Freeway"))
			{
				fwy_id = n;
			}

			else if (myNode.output_link[n].getLinkType().getName().equals("Off-Ramp") || myNode.output_link[n].getLinkType().getName().equals("Interconnect"))
			{
				off_ramp_id = n;
			}
		}		
	}
		
	/* Methods */
	// Method for adjust the split ratio
	@Override
	protected Double3DMatrix computeAppliedSplitRatio(Double3DMatrix splitratio_selected, SupplyDemand demand_supply,final int ensemble_index) {
		
		Double3DMatrix splitratio_new = new Double3DMatrix(splitratio_selected.getData());
		
		// Get index for general vehicle type.
		int vehicle_index = myNode.myNetwork.getMyScenario().getVehicleTypeIndexForId(1);
		
		// Get downstream mainline density and local split ratio for the off-ramp
		double mainline_density = getMainlineDensity(ensemble_index);
		
		double sr_local_avg = getLocalSR(splitratio_selected, vehicle_index);
		
		
		// Adjusts the split ratio and
		for	(int v = 0 ; v < splitratio_selected.getnVTypes();v++)
		{
			// sr_predict = sr_local_avg + K * max(density - threshold, 0)
			double diverging_ratio = sr_local_avg + scaling_factor * Math.max(mainline_density - threshold, 0);
			
			// Handles illegal split ratios by round them to a legal one and sends a warning.
			if (diverging_ratio < 0)
			{
				BeatsErrorLog.addWarning("Split ratio at node ID = " + myNode.getId() + " has been adjusted to an illegal ratio (" + diverging_ratio +") it has been ceiled to 0.");
				diverging_ratio = 0;
			}
			else if (diverging_ratio > 1)
			{
				BeatsErrorLog.addWarning("Split ratio at node ID = " + myNode.getId() + " has been adjusted to an illegal ratio (" + diverging_ratio +") it has been floored to 1.");
				diverging_ratio = 1;
			}
			
			splitratio_new.set(0, off_ramp_id, v, diverging_ratio);
			splitratio_new.set(0, fwy_id, v,1 - diverging_ratio);
		}
		

		return splitratio_new;
	}
	
	// Method for getting mainline density
	private double getMainlineDensity(int ensemble_index)
	{
		double mainline_density = myNode.output_link[fwy_id].getTotalDensityInVPMeter(ensemble_index)*myNode.output_link[fwy_id].get_Lanes();
		
		return mainline_density;
	}
	
	// Method for getting local_sr
	private double getLocalSR(Double3DMatrix splitratio_selected, int vehicle_index)
	{
		double sr_local_avg = splitratio_selected.get(0, off_ramp_id, vehicle_index);
		
		return sr_local_avg;
	}

	// Reset method
	@Override
	protected void reset() {
		// Nothing to reset.

	}
	
	// Validation method
	@Override
	protected void validate() {
		// Validation of incoming links
		if(myNode.input_link.length != 1)
		{
			BeatsErrorLog.addError("Incorrect number of incomming links at node ID = " + myNode.getId() + " , total number of incomming links are " + myNode.input_link.length + " it must be 1.");
		}
		
		// Validation of outgoing links
		if(myNode.output_link.length != 2)
		{
			BeatsErrorLog.addError("Incorrect number of outgoing links at node ID = " + myNode.getId() + " , total number of outgoing links are " + myNode.output_link.length + " it must be 2.");
		}
		
		if(fwy_id == -1)
		{
			BeatsErrorLog.addError("Missing downstream link of type Freeway at node ID = " + myNode.getId() + " ,  it must be exactly one link downstream of type Freeway.");
		}
		
		if(off_ramp_id == -1)
		{
			BeatsErrorLog.addError("Missing diverging link of type Off-Ramp/Interconnect at node ID = " + myNode.getId() + " ,  it must be exactly one diverging link of type Off-Ramp or Interconnect.");
		}
	}

}
