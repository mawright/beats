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

package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

public class Node_FlowSolver_LNCTM extends Node_FlowSolver implements Serializable {

    private static final long serialVersionUID = -96391726575060181L;
    // used in update()
	protected double [] outDemandKnown;	// [nOut]
	protected double [] dsratio;			// [nOut]
	protected boolean [][] iscontributor;	// [nIn][nOut]

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

	public Node_FlowSolver_LNCTM(Node myNode) {
		super(myNode);
	}

	/////////////////////////////////////////////////////////////////////
	// implementation
	/////////////////////////////////////////////////////////////////////

	@Override
	public void reset() {
		iscontributor = new boolean[myNode.nIn][myNode.nOut];
		dsratio 		= new double[myNode.nOut];
		outDemandKnown 	= new double[myNode.nOut];
	}
	
	@Override
    public IOFlow computeLinkFlows(final Double [][][] sr,final int ensemble_index){

    	int i,j,k;
		int nIn = myNode.nIn;
		int nOut = myNode.nOut;        
    	int numVehicleTypes = myNode.getMyNetwork().getMyScenario().get.numVehicleTypes();

        double [][] demand = myNode.node_behavior.getDemand(ensemble_index);
        double [] supply = myNode.node_behavior.getAvailableSupply(ensemble_index);

        // input i contributes to output j .............................
    	for(i=0;i<sr.length;i++)
        	for(j=0;j<sr[i].length;j++)
        		iscontributor[i][j] = BeatsMath.sum(sr[i][j])>0;

        double [] applyratio = new double[nIn];

        for(i=0;i<nIn;i++)
            applyratio[i] = Double.NEGATIVE_INFINITY;
        
        for(j=0;j<nOut;j++){

            // re-compute known output demands .........................
            outDemandKnown[j] = 0d;
            for(i=0;i<nIn;i++)
                for(k=0;k<numVehicleTypes;k++)
                    outDemandKnown[j] += demand[i][k]*sr[i][j][k];

            // compute and sort output demand/supply ratio .............
            if(BeatsMath.greaterthan(supply[j], 0d))
                dsratio[j] = Math.max( outDemandKnown[j] / supply[j] , 1d );
            else
	            dsratio[j] = Double.POSITIVE_INFINITY;

            // reflect ratios back on inputs
            for(i=0;i<nIn;i++)
                if(iscontributor[i][j])
                    applyratio[i] = Math.max(dsratio[j],applyratio[i]);
        }

        IOFlow ioflow = new IOFlow(nIn,nOut,numVehicleTypes);

        // scale down input demands
        for(i=0;i<nIn;i++){
            for(k=0;k<numVehicleTypes;k++){
                ioflow.setIn(i,k , demand[i][k] / applyratio[i] );
            }
        }

        // compute out flows ...........................................   
        for(j=0;j<nOut;j++){
            for(k=0;k<numVehicleTypes;k++){
                double val = 0d;
                for(i=0;i<nIn;i++)
                    val += ioflow.getIn(i,k)*sr[i][j][k];
                ioflow.setOut(j,k,val);
            }
        }

        return ioflow;
	}

//	/////////////////////////////////////////////////////////////////////
//	// protected interface
//	/////////////////////////////////////////////////////////////////////
//
//	protected Double3DMatrix resolveUnassignedSplits(final Double3DMatrix splitratio,final SupplyDemand demand_supply){
//		return null;
//	}
	
}