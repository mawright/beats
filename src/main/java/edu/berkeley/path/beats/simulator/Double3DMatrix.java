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

import java.util.StringTokenizer;

// 3D matrix class used for representing time-invariant split ratio matrices.

final class Double3DMatrix {
	
	private int nIn;			// [1st dimension] number of input links
	private int nOut;			// [2nd dimension] number of columns
	private int nVTypes;		// [3rd dimension] number of slices
	private boolean isempty;	// true if there is no data;
	private Double [][][] data;
    
	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
    
    public Double3DMatrix(int nIn,int nOut,int nVTypes,Double val) {
    	this.nIn = nIn;
    	this.nOut = nOut;
    	this.nVTypes = nVTypes;
    	data = new Double[nIn][nOut][nVTypes];
    	for(int i=0;i<nIn;i++)
        	for(int j=0;j<nOut;j++)
            	for(int k=0;k<nVTypes;k++)
            		data[i][j][k] = val;
    	this.isempty = nIn==0 && nOut==0 && nVTypes==0;
    }
    
    public Double3DMatrix(String str) {

    	int numtokens,i,j,k;
		boolean issquare = true;
    	nIn = 0;
    	nOut = 0;
    	nVTypes = 0;
    	
    	if ((str.isEmpty()) || (str.equals("\n")) || (str.equals("\r\n"))){
    		isempty = true;
			return;
    	}
    	
    	str.replaceAll("\\s","");
    	
    	// populate data
		StringTokenizer slicesX = new StringTokenizer(str, ";");
		nIn = slicesX.countTokens();
		i=0;
		boolean allnan = true;
		while (slicesX.hasMoreTokens() && issquare) {
			String sliceX = slicesX.nextToken();
			StringTokenizer slicesXY = new StringTokenizer(sliceX, ",");
			
			// evaluate nOut, check squareness
			numtokens = slicesXY.countTokens();
			if(nOut==0) // first time here
				nOut = numtokens;
			else{
				if(nOut!=numtokens){
					issquare = false;
					break;
				}
			}
			
			j=0;
			while (slicesXY.hasMoreTokens() && issquare) {
				String sliceXY = slicesXY.nextToken();
				StringTokenizer slicesXYZ = new StringTokenizer(sliceXY,":");
				
				// evaluate nVTypes, check squareness
				numtokens = slicesXYZ.countTokens();
				if(nVTypes==0){ // first time here
					nVTypes = numtokens;
					data = new Double[nIn][nOut][nVTypes];
				}
				else{
					if(nVTypes!=numtokens){
						issquare = false;
						break;
					}
				}
				
				k=0;
				while (slicesXYZ.hasMoreTokens() && issquare) {
					try {
						Double value = Double.parseDouble(slicesXYZ.nextToken());
						if(value>=0){
							data[i][j][k] = value;
							allnan = false;
						}
						else
							data[i][j][k] = Double.NaN;
					} catch (NumberFormatException e) {
						data[i][j][k] = Double.NaN;
					}
					k++;
				}
				j++;
			}
			i++;
		}

		if(allnan){
			nIn=0;
			nOut=0;
			nVTypes=0;
			data = null;
		}
		
		if(!issquare){
			BeatsErrorLog.addError("Data is not square.");
			nIn=0;
			nOut=0;
			nVTypes=0;
			data = null;
		}

    	this.isempty = nIn==0 && nOut==0 && nVTypes==0;
		
    }
     
    public Double3DMatrix(double [][][] x){
    	nIn = x.length;
    	if(nIn==0)
    		return;
    	nOut = x[0].length;
    	if(nOut==0)
    		return;
    	nVTypes = x[0][0].length;
    	if(nVTypes==0)
    		return;
    	data = new Double[nIn][nOut][nVTypes];
    	for(int i=0;i<nIn;i++)
        	for(int j=0;j<nOut;j++)
            	for(int k=0;k<nVTypes;k++)
            		data[i][j][k] = x[i][j][k];
    	isempty = false;
    }
    
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////  

	public boolean isEmpty() {
		return isempty;
	}
	
    public int getnIn() {
		return nIn;
	}

	public int getnOut() {
		return nOut;
	}

	public int getnVTypes() {
		return nVTypes;
	}

	public Double [][][] getData() {
		return data;
	}
	
	public Double [][][] cloneData() {
		Double [][][] cData = new Double [nIn][nOut][nVTypes];
		int i,j,k;
		for(i=0;i<nIn;i++)
			for(j=0;j<nOut;j++)
				for(k=0;k<nVTypes;k++)
					cData[i][j][k] = data[i][j][k];
		return cData;
	}

	public Double get(int i,int j,int k){
    	if(isempty)
    		return Double.NaN;
    	else{
			try{
	    		return data[i][j][k];
			}
			catch(ArrayIndexOutOfBoundsException e){
				return null;
			}
    	}
    }
	
	public Double getSumOverTypes(int i,int j){
		double sum = 0.0;
		for(int k=0;k<data[i][j].length;k++)
			sum += data[i][j][k];
		return sum;
	}
    
    @Override
	public String toString() {
		String str = new String();
		str = "[";
    	for(int i=0;i<nIn;i++){
        	for(int j=0;j<nOut;j++){
            	for(int k=0;k<nVTypes;k++){
        			str += data[i][j][k];
            		if(k<nVTypes-1)
            			str += ":";
            	}
        		if(j<nOut-1)
        			str += ",";
        	}
    		if(i<nIn-1)
    			str += ";";
    	}
    	str += "]";
		return str;
	}
	
	/////////////////////////////////////////////////////////////////////
	// alter data
	/////////////////////////////////////////////////////////////////////  

	public void set(int i,int j,int k,Double f){
    	if(isempty)
    		return;
    	data[i][j][k] = f;
    }
    	
	public void setAllVehicleTypes(int i,int j,Double [] f){
    	if(isempty)
    		return;
		for(int k=0;k<nVTypes;k++)
			data[i][j][k] = f[k];
	}
	
    public void multiplyscalar(double value){
    	if(isempty)
    		return;
    	int i,j,k;
    	for(i=0;i<nIn;i++)
    		for(j=0;j<nOut;j++)
    			for(k=0;k<nVTypes;k++)
    				data[i][j][k] *= value;	
    }
    
    public void addscalar(double value){
    	if(isempty)
    		return;
    	int i,j,k;
    	for(i=0;i<nIn;i++)
    		for(j=0;j<nOut;j++)
    			for(k=0;k<nVTypes;k++)
    				data[i][j][k] += value;	
    }
    
    public void copydata(Double3DMatrix in){
    	if(in.nIn!=nIn || in.nOut!=nOut || in.nVTypes!=nVTypes)
    		return;
    	int i,j,k;
    	for(i=0;i<nIn;i++)
    		for(j=0;j<nOut;j++)
    			for(k=0;k<nVTypes;k++)
    				data[i][j][k] = in.data[i][j][k];	  
    }
    
	/////////////////////////////////////////////////////////////////////
	// check data
	/////////////////////////////////////////////////////////////////////  
    
    public boolean hasNaN(){
    	if(isempty)
    		return false;
    	int i,j,k;
    	for(i=0;i<nIn;i++)
    		for(j=0;j<nOut;j++)
    			for(k=0;k<nVTypes;k++)
    				if(data[i][j][k].isNaN())
    					return true;
    	return false;
    }
    
}
