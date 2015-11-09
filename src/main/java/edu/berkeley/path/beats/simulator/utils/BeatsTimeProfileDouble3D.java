package edu.berkeley.path.beats.simulator.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class BeatsTimeProfileDouble3D extends BeatsTimeProfile<Double[][][]> implements Serializable {

    private static final long serialVersionUID = -2466808932226474067L;

    public BeatsTimeProfileDouble3D(
            ArrayList<Integer> in_index,
            ArrayList<Integer> out_index,
            ArrayList<Integer> vt_index,
            ArrayList<String> splitString,
            int nIn,
            int nOut,
            int nVt,
            Double dt,
            Double startTime,
            double simdtinseconds) throws BeatsException {

        super(dt, startTime, simdtinseconds);

        // organize into array of SplitInfo, collect max length
        ArrayList<SplitInfo> splits = new ArrayList<SplitInfo>();
        int max_length = -1;
        for(int i=0;i<in_index.size();i++) {
            SplitInfo s = new SplitInfo(in_index.get(i), out_index.get(i), vt_index.get(i), splitString.get(i));
            splits.add(s);
            max_length = Math.max(max_length,s.get_length());
        }

        // populate data with nans
        int t;
        data = new ArrayList<Double [][][]>();
        for(t=0;t<max_length;t++)
            data.add(BeatsMath.nans(nIn, nOut, nVt));

        // copy in numbers
        for(t=0;t<data.size();t++) {
            Double[][][] z = data.get(t);
            for (SplitInfo splitinfo : splits) {
                int in = splitinfo.inIndex;
                int out = splitinfo.outIndex;
                int vt = splitinfo.vtIndex;
                z[in][out][vt] = splitinfo.X==null ? Double.NaN : splitinfo.X[Math.min(t,splitinfo.X.length-1)];
            }
            data.set(t, z);
        }
    }

    public void validate(double minval,double maxval) {
        super.validate();

        // check values are in range
        int i,j,k;
        for(Double[][][] X : data)
            for (i=0; i<X.length; i++)
                for (j = 0; j < X[i].length; j++)
                    for (k = 0; k < X[i][j].length; k++)
                        if (BeatsMath.greaterthan(X[i][j][k],maxval) | BeatsMath.lessthan(X[i][j][k],minval))
                            BeatsErrorLog.addError("Values out of range.");
    }

    class SplitInfo {
        int inIndex;
        int outIndex;
        int vtIndex;
        Double [] X;
        public SplitInfo(int inIndex,int outIndex,int vtIndex,String str){
            this.inIndex = inIndex;
            this.outIndex = outIndex;
            this.vtIndex = vtIndex;
            this.X = BeatsFormatter.readCSVstring_nonnegative(str, ",");
        }
        public int get_length(){
            return X==null ? 0 : X.length;
        }
    }

}