package edu.berkeley.path.beats.simulator;

import java.util.Arrays;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;

public class SplitRatioPerturber {

	/**
	 * @param args
	 */
	private SplitRatioPerturber() {
		// TODO Auto-generated method stub
	}
	
	public static Double3DMatrix[] perturb2OutputSplit(Double3DMatrix split, double variance, int numEnsemble){
		Double3DMatrix[] splitPerturbed = new Double3DMatrix[numEnsemble];
		
		if(split.getnIn()!=1 && split.getnOut()!=2){
			Arrays.fill(splitPerturbed, split);
			return splitPerturbed;
		}
		int e;
		for (e=0;e<numEnsemble;e++){
			splitPerturbed[e] = new Double3DMatrix(split);
		}
		
		for (int v=0;v<split.getnVTypes();v++){
			double max = 0;
			int max_index = 0;
			for (int o=0;o<split.getnOut();o++){
				if (max<split.get(0, o, v)){
					max=split.get(0, o, v);
					max_index = o;
				}
			}
			double[] params = BeatsMath.betaParamsFromRVMeanAndVariance(max, variance);
			double[][] sample;
			try{
				sample = BeatsMath.sampleDirichlet(params, numEnsemble);
			}
			catch (NotStrictlyPositiveException ex){
				double newmax = max - variance*1.5;
				params = BeatsMath.betaParamsFromRVMeanAndVariance(newmax, variance);
				sample = BeatsMath.sampleDirichlet(params, numEnsemble);
			}
			for (e=0;e<numEnsemble;e++){
				splitPerturbed[e].set(0, max_index, v, sample[e][0]);
				splitPerturbed[e].set(0, 1-max_index, v, sample[e][1]);
			}
		}
		return splitPerturbed;
	}

}
