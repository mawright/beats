package edu.berkeley.path.beats.simulator.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class BeatsTimeProfileDouble extends BeatsTimeProfile<Double> implements Serializable {

    private static final long serialVersionUID = 2393304563152031375L;

    // initialize a 1D vector from comma separated string of positive numbers
    // negative numbers get replaced with nan.
    public BeatsTimeProfileDouble(String str, String delim, Double dt, Double startTime, double simdtinseconds) {
        super(dt,startTime,simdtinseconds);
        data = dataFromDoubles(BeatsFormatter.readCSVstring_nonnegative(str, delim));
    }

    public void reset() {
        super.reset();
        current_sample = data.get(0);
    }

    public void multiplyscalar(double value) {
        if (data == null)
            return;
        int i;
        for (i = 0; i < data.size(); i++)
            data.set(i, data.get(i) * value);
    }

    private ArrayList<Double> dataFromDoubles(Double [] X){
        ArrayList<Double> z = new ArrayList<Double>();
        for(Double x : X)
            z.add(x);
        return z;
    }

}