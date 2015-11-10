package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;

import java.io.Serializable;

public abstract class Node_SplitRatioSolver implements Serializable {

    private static final long serialVersionUID = -8863696145226240360L;

    public Node myNode;

    public abstract Double [][][] computeAppliedSplitRatio(final Double [][][] splitratio_selected,final int ensemble_index);
    public abstract void reset();
    public abstract void validate();
    
	public Node_SplitRatioSolver(Node myNode) {
		super();
		this.myNode = myNode;
	}

}
