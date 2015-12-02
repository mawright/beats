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

package edu.berkeley.path.beats.simulator.utils;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.math3.distribution.GammaDistribution;

/** XXX. 
 * YYY
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public final class BeatsMath implements Serializable {

	private static final long serialVersionUID = -361466048892717553L;
	private static Random random = new Random();
	private static final double EPSILON = (double) 1e-4;

    public static Double [] zeros(int n1){
        if(n1<0)
            return null;
        Double [] z = new Double[n1];
        for(int i=0;i<n1;i++)
            z[i]=0d;
        return z;
    }

//    public static double [][] zeros_double(int n1,int n2){
//        if (n1<0 || n2<0)
//            return null;
//        return new double[n1][n2];
//    }

    public static Double [][] zeros(int n1,int n2){
        if (n1<0 || n2<0)
            return null;
        Double [][] z = new Double[n1][n2];
        for(int i=0;i<n1;i++)
            z[i] = BeatsMath.zeros(n2);
        return z;
    }

    public static Double [][][] zeros(int n1,int n2,int n3){
        if (n1<0 || n2<0 || n3<0)
            return null;
        Double [][][] z = new Double[n1][n2][n3];
        for(int i=0;i<n1;i++)
            z[i] = BeatsMath.zeros(n2,n3);
        return z;
    }

    public static Double [][][][] zeros(int n1,int n2,int n3,int n4){
        if (n1<0 || n2<0 || n3<0 || n4<0)
            return null;
        Double [][][][] z = new Double[n1][n2][n3][n4];
        for(int i=0;i<n1;i++)
            z[i] = BeatsMath.zeros(n2,n3,n4);
        return z;
    }

    public static Double [][][] nans(int n1,int n2,int n3){
        Double [][][] X = BeatsMath.zeros(n1,n2,n3);
        int i,j,k;
        for(i=0;i<n1;i++)
            for(j=0;j<n2;j++)
                for(k=0;k<n3;k++)
                    X[i][j][k] = Double.NaN;
        return X;
    }

    public static Double [][][] ones(int n1,int n2,int n3){
        Double [][][] X = BeatsMath.zeros(n1,n2,n3);
        int i,j,k;
        for(i=0;i<n1;i++)
            for(j=0;j<n2;j++)
                for(k=0;k<n3;k++)
                    X[i][j][k]= 1d;
        return X;
    }

    public static Double sum(Double [] V){
		if(V==null)
			return Double.NaN;
		Double answ = 0d;
		for(int i=0;i<V.length;i++)
			if(V[i]!=null)
				answ += V[i];
		return answ;
	}

	public static double sum(double [] V){
		if(V==null)
			return Double.NaN;
		double answ = 0d;
		for(int i=0;i<V.length;i++)
			if(!Double.isNaN(V[i]))
				answ += V[i];
		return answ;
	}
	
	public static Double sum(Collection<Double> V) {
		if (null == V) 
			return Double.NaN;
		Double ans = .0d;
		Iterator<Double> iter = V.iterator();
		while (iter.hasNext()) ans += iter.next();
		return ans;
	}

	public static Double [] sum(Double [][] V,int dim){
		if(V==null)
			return null;
		if(V.length==0)
			return null;
		if(V[0].length==0)
			return null;
		Double [] answ;
		int i,j;
		int n1 = V.length;
		int n2 = V[0].length;
		switch(dim){
		case 1:
			answ = BeatsMath.zeros(n2);
			for(i=0;i<V.length;i++)
				for(j=0;j<V[i].length;j++){
					if(answ[j]==null)
						answ[j]=0d;
					if(V[i][j]!=null)
						answ[j] += V[i][j];
				}
			return answ;
		case 2:
			answ = BeatsMath.zeros(n1);
			for(i=0;i<V.length;i++){
				answ[i]=0d;
				for(j=0;j<V[i].length;j++)
					if(V[i][j]!=null)
						answ[i] += V[i][j];
			}
			return answ;
		default:
			return null;
		}
	}

    public static Double [] times(Double [] V,Double a){
        if(V==null)
            return null;
        Double [] answ = new Double [V.length];
        for(int i=0;i<V.length;i++)
            answ[i] = a*V[i];
        return answ;
    }

	public static double [] times(double [] V,double a){
		if(V==null)
			return null;
		double [] answ = new double [V.length];
		for(int i=0;i<V.length;i++)
			answ[i] = a*V[i];
		return answ;
	}
	
	public static int ceil(double a){
		return (int) Math.ceil(a-BeatsMath.EPSILON);
	}
	
	public static int floor(double a){
		return (int) Math.floor(a+BeatsMath.EPSILON);
	}
	
	public static int round(double a){
		return (int) Math.round(a);
	}
	
	public static boolean any (boolean [] x){
		if(x==null)
			return false;
		if(x.length==0)
			return false;
		for(int i=0;i<x.length;i++)
			if(x[i])
				return true;
		return false;
	}
	
	public static boolean all (boolean [] x){
		if(x==null)
			return false;
		if(x.length==0)
			return false;
		for(int i=0;i<x.length;i++)
			if(!x[i])
				return false;
		return true;
	}

    public static boolean all_non_negative (Double [] x){
        if(x==null)
            return false;
        if(x.length==0)
            return true;
        for(int i=0;i<x.length;i++)
            if(x[i]<0)
                return false;
        return true;
    }

	
	public static boolean[] not(boolean [] x){
		if(x==null)
			return null;
		if(x.length==0)
			return null;
		boolean [] y = x.clone();
		for(int i=0;i<y.length;i++)
			y[i] = !y[i];
		return y;
	}
	
	public static int count(boolean [] x){
		if(x==null)
			return 0;
		if(x.length==0)
			return 0;
		int s = 0;
		for(int i=0;i<x.length;i++)
			if(x[i])
				s++;
		return s;
	}
	
	public static ArrayList<Integer> find(boolean [] x){
		if(x==null)
			return null;
		ArrayList<Integer> r = new ArrayList<Integer>();
		for(int i=0;i<x.length;i++)
			if(x[i])
				r.add(i);
		return r;
	}
	
	public static boolean isintegermultipleof(Double A,Double a){
		if(A.isInfinite())
			return true;
		if(A==0)
			return true;
		if(a==0)
			return false;
		boolean result;
		result = BeatsMath.equals( BeatsMath.round(A/a) , A/a );
		result &=  A/a>0;
		return result;
	}
	
	public static boolean equals(double a,double b){
		return Math.abs(a-b) < BeatsMath.EPSILON;
	}	

	public static boolean equals(double a,double b,double epsilon){
		return Math.abs(a-b) < epsilon;
	}	
	
	public static boolean equals1D(ArrayList<Double> a,ArrayList<Double> b){
		if(a==null || b==null)
			return false;
		if(a.size()!=b.size())
			return false;
		for(int i=0;i<a.size();i++){
			if(a.get(i).isNaN() && b.get(i).isNaN())
				continue;
			if(a.get(i).isInfinite() && b.get(i).isInfinite())
				continue;
			if( !BeatsMath.equals(a.get(i), b.get(i)) )
				return false;
		}
		return true;
	}
	
	public static boolean equals2D(ArrayList<ArrayList<Double>> a,ArrayList<ArrayList<Double>> b){
		if(a==null || b==null)
			return false;
		if(a.size()!=b.size())
			return false;
		for(int i=0;i<a.size();i++)
			if( !BeatsMath.equals1D(a.get(i), b.get(i)) )
				return false;
		return true;
	}
	
	public static boolean equals3D(ArrayList<ArrayList<ArrayList<Double>>> a,ArrayList<ArrayList<ArrayList<Double>>> b){
		if(a==null || b==null)
			return false;
		if(a.size()!=b.size())
			return false;
		for(int i=0;i<a.size();i++)
			if( !BeatsMath.equals2D(a.get(i), b.get(i)) )
				return false;
		return true;
	}
	
	public static boolean greaterthan(double a,double b){
		return a > b + BeatsMath.EPSILON;
	}

	public static boolean greaterorequalthan(double a,double b){
		return !lessthan(a,b);
	}
	
	public static boolean lessthan(double a,double b){
		return a < b - BeatsMath.EPSILON;
	}

	public static boolean lessorequalthan(double a,double b){
		return !greaterthan(a,b);
	}

    public static Double max(List<Double> X){
        if(X.isEmpty())
            return Double.NaN;
        Double x = Double.NEGATIVE_INFINITY;
        for(Double d : X)
            if(d>x)
                x=d;
        return x;
    }

    public static Double min(List<Double> X){
        if(X.isEmpty())
            return Double.NaN;
        Double x = Double.POSITIVE_INFINITY;
        for(Double d : X)
            if(d<x)
                x=d;
        return x;
    }

	// greatest common divisor of two integers
	public static int gcd(int p, int q) {
		if (q == 0) {
			return p;
		}
		return gcd(q, p % q);
	}

    // deep copy a double array
    public static double[] copy(double[] x){
        if(x==null)
            return null;
        if(x.length==0)
            return null;
        double [] y = new double[x.length];
        for(int i=0;i<x.length;i++)
            y[i]=x[i];
        return y;
    }

    // deep copy a double array
    public static Double[] copy(Double[] x){
        return (x==null || x.length==0) ? null : x.clone();
    }

	// deep copy a double array
	public static Double[][] copy(Double[][] x){
		if(x==null)
			return null;
		if(x.length==0)
			return null;
		if(x[0].length==0)
			return null;
		int n1 = x.length;
		int n2 = x[0].length;
		Double [][] y = new Double[n1][n2];
		int i,j;
		for(i=0;i<n1;i++)
			for(j=0;j<n2;j++)
				y[i][j]=x[i][j];
		return y;
	}

	public static double sampleZeroMeanUniform(double std_dev){
		return std_dev*Math.sqrt(3)*(2*BeatsMath.random.nextDouble()-1);
	}
	
	public static double sampleZeroMeanGaussian(double std_dev){
		return std_dev*BeatsMath.random.nextGaussian();
	}

    public static double[] sampleDirichlet(Double[] concentration_parameters){

        int i;
        double[] sample = new double[concentration_parameters.length];
        for(i=0;i<concentration_parameters.length;i++){
            if (concentration_parameters[i] <= 0d)
                concentration_parameters[i] = 0.01d; //concentration parameters must be positive
            GammaDistribution Gamma = new GammaDistribution(concentration_parameters[i], 1);
            sample[i] = Gamma.sample();
        }

        double sum_sample = BeatsMath.sum(sample);
        sample = BeatsMath.times(sample, 1d/sum_sample);

        return sample;
    }


    public static double[][] sampleDirichlet(double[] concentration_parameters, int numSamples){
		// Samples from a Dirichlet distribution using Gamma distributions
		// Random variables distributed according to a Dirichlet distribution are random vectors
		// whose entries are in the range (0, 1) and sum to 1.
		// sample dims: numSamples DirichletDims
		int i, e;
		double[][] sample = new double[numSamples][concentration_parameters.length];
		for(i=0;i<concentration_parameters.length;i++){
			if (concentration_parameters[i] <= 0d)
				concentration_parameters[i] = 0.01d; //concentration parameters must be positive
			GammaDistribution Gamma = new GammaDistribution(concentration_parameters[i], 1);
			for(e=0;e<numSamples;e++){
				sample[e][i] = Gamma.sample();
			}
		}
		for(e=0;e<numSamples;e++){
			double sum_sample = BeatsMath.sum(sample[e]);
			sample[e] = BeatsMath.times(sample[e], 1d/sum_sample);
		}
		return sample;
	}
	
	public static Double[] betaParamsFromRVMeanAndVariance(double mean, double variance){
		// The beta distribution is the special case of the Dirichlet distribution for k = 2
		// k > 2 not implemented yet
		if( mean>.99d)
			mean = .95;
		Double[] params = new Double[2];
		double m = mean;
		double v = variance;
		params[0] = - m * (Math.pow(m, 2) - m + v) / v;
		params[1] = (m-1) * (Math.pow(m, 2) - m + v) / v;
		return params;
	}

    public static double[] betaParamsFromRVModeAndSampleSize(double m, double n){
        double[] params = new double[2];
        params[0] = m*n - 2*m + 1;
        params[1] = 2*m + n - m*n - 1;
        return params;
    }

    public static Double[] betaParamsFromRVMeanAndSampleSize(double m, double n){
        Double[] params = new Double[2];
        params[0] = m*n;
        params[1] = (1-m)*n;
        return params;
    }

    public static Double [][][] normalize(Double [][][] In) {

        int i, j, k;
        boolean hasNaN;
        int countNaN;
        int idxNegative;
        double sum;

        if(In==null)
            return null;

        int nIn = In.length;
        int nOut = nIn > 0 ? In[0].length : 0;
        int nVt = nOut > 0 ? In[0][0].length : 0;
        Double [][][] Out = In.clone();

        for (i = 0; i < nIn; i++) {
            for (k = 0; k < nVt; k++) {
                hasNaN = false;
                countNaN = 0;
                idxNegative = -1;
                sum = 0.0f;

                // count NaNs in ikth row of In
                // sum non-nan entries in ikth row of In
                for (j = 0; j < nOut; j++) {
                    if (Double.isNaN(In[i][j][k])) {
                        countNaN++;
                        idxNegative = j;
                        if (countNaN > 1)
                            hasNaN = true;
                    } else
                        sum += In[i][j][k];
                }

                // case single nan
                if (countNaN == 1) {
                    Out[i][idxNegative][k] = Math.max(0f, (1 - sum));
                    sum += Out[i][idxNegative][k];
                }

                // case all zeros => set first to 1
                if (!hasNaN && BeatsMath.equals(sum, 0.0)) {
                    Out[i][0][k] = 1d;
                    continue;
                }

                // case no nans, sum<1 => normalize
                if ((!hasNaN) && (sum < 1.0)) {
                    for (j = 0; j < nOut; j++)
                        Out[i][j][k] = Out[i][j][k] / sum;
                    continue;
                }

                // sum>1 => normalize non-zero entries
                if (sum >= 1.0) {
                    for (j = 0; j < nOut; j++)
                        if (Double.isNaN(In[i][j][k]))
                            Out[i][j][k] = 0d;
                        else
                            Out[i][j][k] = Out[i][j][k] / sum;
                }
            }
        }
        return Out;
    }

}
