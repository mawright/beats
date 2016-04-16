package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.io.Serializable;

public class SplitRatioPerturber implements Serializable {

    private static final long serialVersionUID = -1760068806074220232L;

    private SplitRatioPerturber() {
        // TODO Auto-generated method stub
    }

//    public static Double [][][][] sampleFromConcentrationParameters( Double [][][] params, int numEnsemble){
//        int nIn = params.length;
//        int nOut = nIn>0 ? params[0].length : 0;
//        int nVt = nOut>0 ? params[0][0].length : 0;
//
//        // Assumes concentration parameters of each input link are independent
//        Double [][][][] splitPerturbed = BeatsMath.zeros(numEnsemble,nIn,nOut,nVt);
//
//        int v,o,i,e;
//        Double[] paramsForInputLink = BeatsMath.zeros(nOut);
//        Double[][] sample;
//        for (v=0;v<nVt;v++){
//            for (i=0;i<nIn;i++){
//                for (o=0;o<nOut;o++){
//                    paramsForInputLink[o] = params[i][o][v] + 1;
//                }
//                sample = BeatsMath.sampleDirichlet(paramsForInputLink, numEnsemble);
//                for (e=0;e<numEnsemble;e++){
//                    splitPerturbed[e] = BeatsMath.zeros(nIn,nOut,nVt);
//                    for (o=0;o<nOut;o++){
//                        splitPerturbed[e][i][o][v] = sample[e][o];
//                    }
//                }
//            }
//        }
//
//        return splitPerturbed;
//    }

    // single sample version of above
    public static Double [][][] sampleFromConcentrationParametersOnce( Double [][][] params){
        int nIn = params.length;
        int nOut = nIn>0 ? params[0].length : 0;
        int nVt = nOut>0 ? params[0][0].length : 0;

        // Assumes concentration parameters of each input link are independent
        Double [][][] splitPerturbed = BeatsMath.zeros(nIn,nOut,nVt);

        int v,o,i;
        Double[] paramsForInputLink = BeatsMath.zeros(nOut);
        double[] sample;
        for (v=0;v<nVt;v++){
            for (i=0;i<nIn;i++){
                for (o=0;o<nOut;o++){
                    paramsForInputLink[o] = params[i][o][v] + 1;
                }
                sample = BeatsMath.sampleDirichlet(paramsForInputLink);
                splitPerturbed = BeatsMath.zeros(nIn,nOut,nVt);
                for (o=0;o<nOut;o++){
                    splitPerturbed[i][o][v] = sample[o];
                }
            }
        }
        return splitPerturbed;
    }

//    public static Double [][][][] perturb2OutputSplit(Double [][][] split, SplitRatioProfile profile, int numEnsemble){
//
//        int nIn = split.length;
//        int nOut = nIn>0 ? split[0].length : 0;
//        int nVt = nOut>0 ? split[0][0].length : 0;
//
//        Double [][][][] splitPerturbed = BeatsMath.zeros(numEnsemble,nIn,nOut,nVt);
//
//        if(nIn!=1 && nOut!=2){ // return non-perturbed if non 1 to 2 split given
//            Arrays.fill(splitPerturbed, split);
//            return splitPerturbed;
//        }
//        int e;
//        for (e=0;e<numEnsemble;e++){
//            splitPerturbed[e] = split.clone();
//        }
//
//        for (int v=0;v<nVt;v++){
//            Double max = 0d;
//            int max_index = 0;
//            for (int o=0;o<nOut;o++){
//                if (max<split[0][o][v]){
//                    max=split[0][o][v];
//                    max_index = o;
//                }
//            }
//            Double[] params;
//            if (profile.getSampleSize()!=null){
//                params = BeatsMath.betaParamsFromRVMeanAndSampleSize(max, profile.getSampleSize());
//            }
//            else {
//                params = BeatsMath.betaParamsFromRVMeanAndVariance(max, profile.getVariance()); }
//            Double[][] sample;
//            try{
//                sample = BeatsMath.sampleDirichlet(params, numEnsemble);
//            }
//            catch (NotStrictlyPositiveException ex){
//                double newmax = max - profile.getVariance()*1.5; //
//                params = BeatsMath.betaParamsFromRVMeanAndVariance(newmax, profile.getVariance());
//                sample = BeatsMath.sampleDirichlet(params, numEnsemble);
//            }
//            for (e=0;e<numEnsemble;e++){
//                splitPerturbed[e][0][max_index][v] = sample[e][0];
//                splitPerturbed[e][0][1-max_index][v] = sample[e][1];
//            }
//        }
//        return splitPerturbed;
//
//    }

    // single sample version of above
    public static Double [][][] perturb2OutputSplitOnce(final Double [][][] split, SplitRatioProfile profile){

        int nIn = split.length;
        int nOut = nIn>0 ? split[0].length : 0;
        int nVt = nOut>0 ? split[0][0].length : 0;

        // clone it
        Double [][][] splitPerturbed = BeatsMath.copy(split);

        if(nIn!=1 && nOut!=2) // return non-perturbed if non 1 to 2 split given
            return splitPerturbed;

        for (int v=0;v<nVt;v++){
            Double max = 0d;
            int max_index = 0;
            for (int o=0;o<nOut;o++){
                if (max<split[0][o][v]){
                    max=split[0][o][v];
                    max_index = o;
                }
            }
            Double[] params;
            if (profile.getSampleSize()!=null){
                params = BeatsMath.betaParamsFromRVMeanAndSampleSize(max, profile.getSampleSize());
            }
            else {
                params = BeatsMath.betaParamsFromRVMeanAndVariance(max, profile.getVariance()); }
            double[] sample;
            try{
                sample = BeatsMath.sampleDirichlet(params);
            }
            catch (NotStrictlyPositiveException ex){
                double newmax = max - profile.getVariance()*1.5; //
                params = BeatsMath.betaParamsFromRVMeanAndVariance(newmax, profile.getVariance());
                sample = BeatsMath.sampleDirichlet(params);
            }
            splitPerturbed[0][max_index][v] = sample[0];
            splitPerturbed[0][1-max_index][v] = sample[1];
        }
        return splitPerturbed;
    }

}