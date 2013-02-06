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

import java.util.List;

/** Storage for a scenario state. 
 * <p>
* @author Gabriel Gomes
*/
final public class BeatsState {

	protected Scenario myScenario;
	protected int numNetworks;					// number of networks in the scenario
	protected NetworkState [] networkState;		// array of states for networks
	protected int numVehicleTypes; 				// size of 2nd dimension of networkState

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
	
	public BeatsState(Scenario myScenario) {
		if(myScenario==null)
			return;
		if(myScenario.getNetworkList()==null)
			return;
		if(myScenario.getNetworkList().getNetwork()==null)
			return;
		this.myScenario = myScenario;
		
		this.numNetworks = myScenario.getNetworkList().getNetwork().size();
		this.numVehicleTypes = myScenario.getNumVehicleTypes();

		this.networkState = new NetworkState[numNetworks];
		for(int i=0;i<numNetworks;i++){
			int numLinks = myScenario.getNetworkList().getNetwork().get(i).getLinkList().getLink().size();
			this.networkState[i] = new NetworkState(numLinks);
		}
	}

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	protected void recordstate() throws BeatsException {
		int i,j;
		for(int netindex=0;netindex<numNetworks;netindex++){
			edu.berkeley.path.beats.jaxb.Network network = myScenario.getNetworkList().getNetwork().get(netindex);
			List<edu.berkeley.path.beats.jaxb.Link> links = network.getLinkList().getLink();
			for(i=0;i<networkState[netindex].getNumLinks();i++){
				Link link = (Link) links.get(i);				
				//LinkCumulativeData link_cum_data = myScenario.getCumulatives(link);
//				for(j=0;j<numVehicleTypes;j++){
//					networkState[netindex].density[i][j] = link_cum_data.getMeanDensity(0, j);
//					networkState[netindex].flow[i][j] = link_cum_data.getMeanOutputFlow(0, j);
//				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	
	public Double getDensity(int netindex,int i,int j) {
		if(netindex<0 || netindex>=numNetworks)
			return Double.NaN;
		NetworkState  N = networkState[netindex];
		if(i<0 || i>=N.getNumLinks() || j<0 || j>=numVehicleTypes)
			return Double.NaN;
		else
			return N.density[i][j];
	}

	public Double getFlow(int netindex,int i,int j) {
		if(netindex<0 || netindex>=numNetworks)
			return Double.NaN;
		NetworkState  N = networkState[netindex];
		if(i<0 || i>=N.getNumLinks() || j<0 || j>=numVehicleTypes)
			return Double.NaN;
		else
			return N.flow[i][j];
	}
	
	/////////////////////////////////////////////////////////////////////
	// internal class
	/////////////////////////////////////////////////////////////////////
	
	public class NetworkState{

		protected int numLinks; 		// size of 1st dimension
		protected double[][] density; 	// [veh]
		protected double[][] flow; 		// [veh]

		public NetworkState(int numLinks) {
			this.numLinks = numLinks;
			this.density = new double[numLinks][numVehicleTypes];
			this.flow = new double[numLinks][numVehicleTypes];
		}
		public int getNumLinks() {
			return numLinks;
		}
	}
	
}
