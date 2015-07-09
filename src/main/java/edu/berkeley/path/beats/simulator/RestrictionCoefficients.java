package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.Table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matt on 7/3/15.
 */
public class RestrictionCoefficients extends edu.berkeley.path.beats.jaxb.RestrictionCoefficients {

	private Node myNode;
	private HashMap<Link,Table> RestrictionMatrices;
	private boolean defaultFullRestriction = false;  // if true, all out-links have full restriction on other out-links, leading to
	// traditional strict FIFO

	protected void populate(Node node) {
		this.myNode = node;

		int numIn = myNode.getnIn();

		int i;
		// if only one mutual restriction matrix, it applies to all in-links
		if(getTable().size()==1){
			RestrictionMatrices = new HashMap<Link, Table>(numIn);
			for(i=0;i<numIn;i++){
				Table matrixForAllLinks = new Table(getTable().get(0));
				RestrictionMatrices.put(myNode.getInput_link()[i],matrixForAllLinks);
			}
		}
		// if no mutual restriction matrices specified, default to behavior of strict FIFO
		// (all coefficients are equal to 1)
		else if(getTable().size()==0) {
			defaultFullRestriction = true;
		}
		else {
			Long inputLinkId;
			RestrictionMatrices = new HashMap<Link, Table>(numIn);
			for(i=0;i<numIn;i++){
				inputLinkId = Long.parseLong(getTable().get(i).getName());
				try {
					RestrictionMatrices.put(
							myNode.getInput_link()[myNode.getInputLinkIndex(inputLinkId)],
							new Table(getTable().get(i)));
				}
				catch (ArrayIndexOutOfBoundsException ex){
					BeatsErrorLog.addWarning("Node with ID=" + myNode.getId() +
							" has a restriction matrix for input link ID=" + inputLinkId +
							" but no matching input link. Defaulting to strict FIFO for this node.");
					RestrictionMatrices.clear();
					defaultFullRestriction = true;
					break;
				}
			}
		}

	}

	protected void validate() {
		if(defaultFullRestriction)
				return;

		for(Link nodeInLink: myNode.getInput_link()){
			if(!RestrictionMatrices.containsKey(nodeInLink))
				BeatsErrorLog.addError("Restriction matrix for node " + myNode.getId() + " does not have a matrix for" +
				"in-link w/ ID " + nodeInLink.getId());
		}

		for(Map.Entry<Link, Table> restMat: RestrictionMatrices.entrySet()){
			Table currentMatrix = restMat.getValue();
			Link currentLink = restMat.getKey();
			// check for valid matrix structure
			if(currentMatrix.getNoColumns()!=currentMatrix.getRows().size())
				BeatsErrorLog.addError("Restriction matrix for node " + myNode.getId() + " in-link " +
				currentLink.getId() + " is non-square.");
			for(Table.Row row : currentMatrix.getRows()){
				if(row.get_name() == null) {
					BeatsErrorLog.addError("Restriction matrix for node " + myNode.getId() + " in-link " +
							currentLink.getId() + " has row name(s) unset.");
				} else if (!currentMatrix.getColumnNames().contains(row.get_name())) {
					BeatsErrorLog.addError("Restriction matrix for node " + myNode.getId() + " in-link " +
							currentLink.getId() + " has mismatched row/column names.");
				}
				// check entries make sense (in [0,1])
				for(String value: row.column_value){
					double numericalValue = Double.parseDouble(value);
					if(Double.isNaN(numericalValue)) {
						value = "1";
						BeatsErrorLog.addWarning("Restriction matrix for node " + myNode.getId() + " in-link " +
								currentLink.getId() + " has illegal values(s); setting to 1.");
					}
					if(numericalValue > 1) {
						value = "1";
						BeatsErrorLog.addWarning("Restriction matrix for node " + myNode.getId() + " in-link " +
								currentLink.getId() + " has value(s) > 1; truncating to 1.");
					}
					if(numericalValue < 0) {
						value = "0";
						BeatsErrorLog.addWarning("Restriction matrix for node " + myNode.getId() + " in-link " +
								currentLink.getId() + " has value(s) < 0; setting to 0.");
					}
				}
			}
		}

	}

	public double getCoefficient( Link inLink, Link RestrictorLink, Link RestrictedLink){
		if(defaultFullRestriction)
			return 1;

		return 0;
	}


}
