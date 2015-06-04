package edu.berkeley.path.beats.simulator.utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gomes on 5/18/2015.
 */
public class BeatsTimeProfileDouble1D extends BeatsTimeProfile<Double[]> {

    protected int numTypes;

    public BeatsTimeProfileDouble1D(ArrayList<Integer> vehicle_type_index, ArrayList<String> demandStr, int numVehTypes, Double dt, Double startTime, double simdtinseconds) {

        super(dt,startTime,simdtinseconds);

        this.numTypes = numVehTypes;

//        if(vehicle_type_index.size()!=demandStr.size())
//            throw new BeatsException("Error intitializing BeatsTimeProfileDoubleArray");
//
//        if(vehicle_type_index.size()>=numVehTypes)
//            throw new BeatsException("Error intitializing BeatsTimeProfileDoubleArray");

        // Unpack number strings, hash type_index -> profile
        HashMap<Integer, Double [] > numbers = new HashMap<Integer, Double [] >();
        int maxProfileLength = -1;
        for(int i=0;i<vehicle_type_index.size();i++){
            String str = demandStr.get(i);
            Double [] X = BeatsFormatter.readCSVstring_nonnegative(str, ",");
            maxProfileLength = Math.max(maxProfileLength,X.length);
            numbers.put(vehicle_type_index.get(i),X);
        }

        // populate data with zeros
        int t;
        this.data = new ArrayList<Double[]>();
        for(t=0;t<maxProfileLength;t++)
            data.add(BeatsMath.zeros(numTypes));

        // copy in numbers, normalize
        for(t=0;t<data.size();t++) {
            Double[] z = data.get(t);
            for(int i=0;i<numTypes;i++) {
                Double[] X = numbers.get(i);
                if(X!=null)
                    z[i] = X[t] * simdtinseconds;
            }
            data.set(t, z);
        }

    }

    @Override
    public void reset(){
        super.reset();
        current_sample = BeatsMath.zeros(numTypes);
    }

}