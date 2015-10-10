package edu.berkeley.path.beats.simulator.utils;

import edu.berkeley.path.beats.simulator.Clock;
import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Scenario;

import java.util.ArrayList;

public class BeatsTimeProfile <T> {

    protected T current_sample;
    protected double dtinseconds;            // not really necessary
    protected int samplesteps;
    protected int step_initial_abs;       // time steps at start since midnight
    protected boolean isdone;
    protected double start_time;
	protected ArrayList<T> data;
	
	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

    public BeatsTimeProfile(){}

    public BeatsTimeProfile(Double dt,Double startTime, double simdtinseconds){

        data = new ArrayList<T>();
        isdone = false;

        // step_initial
        start_time = Double.isInfinite(startTime) ? 0d : startTime;
        step_initial_abs = BeatsMath.round(start_time/simdtinseconds);

        // dt
        dtinseconds = dt!=null ? dt.floatValue() : Double.POSITIVE_INFINITY; // assume given in seconds
        samplesteps = BeatsMath.round(dtinseconds/simdtinseconds);
    }
    
	/////////////////////////////////////////////////////////////////////
	// validate, reset
	/////////////////////////////////////////////////////////////////////  

    public void reset(){
        isdone = false;
    }

    public void validate(){
//        if(start_time<0)
//            BeatsErrorLog.addError("start_time<0 in a time profile");
//        if(step_initial_abs<0)
//            BeatsErrorLog.addError("step_initial_abs<0 in a time profile");
        if(dtinseconds<=0)
            BeatsErrorLog.addError("dt<=0 in a time profile");
        if(!Double.isInfinite(dtinseconds) && samplesteps<=0)
            BeatsErrorLog.addError("samplesteps<=0 in a time profile");
    }

    // returns true iff a new sample was chosen
    public boolean sample(boolean forcesample,Clock clock){

        if(data==null || data.isEmpty())
            return false;

        boolean istime = clock.is_time_to_sample_abs(samplesteps, step_initial_abs);
        if(forcesample || (!isdone && istime)){

            int n = data.size()-1;
            int step = clock.sample_index_abs(samplesteps,step_initial_abs);

            if(forcesample){
                System.out.println(step+","+n+","+samplesteps+","+step_initial_abs+","+clock.getT());
                current_sample = data.get(Math.min(step,n));
                return true;
            }

            // demand is zero before step_initial_abs
            if(clock.getAbsoluteTimeStep()< step_initial_abs)
                return false;

            // sample the profile
            if(step<n){
                current_sample = data.get(step);
                return true;
            }

            // last sample
            if(step>=n && !isdone){
                current_sample = data.get(n);
                isdone = true;
                return true;
            }

        }
        return false;
    }

    public T sample_at_time(double time) {
        int n = data.size()-1;
        int step = BeatsMath.floor((time-start_time)/dtinseconds);
        step = Math.max(step, 0);
        step = Math.min(step,n);
        return data.get(step);
    }

    /////////////////////////////////////////////////////////////////////
    // getters / setters
    /////////////////////////////////////////////////////////////////////

    public T getCurrentSample() {
        return current_sample;
    }

    public T getFirst(){
        return data.get(0);
    }

    public double getDtinseconds() {
        return dtinseconds;
    }

    public boolean isDone() {
        return isdone;
    }

    public boolean isEmpty() {
        return data==null ? true : data.size()==0;
    }

    public Integer getNumTime() {
        return data==null ? 0 : data.size();
    }

    public T get(int i){
        if(data==null)
            return null;
        if(data.size()==0)
            return null;
        return data.get(i);
    }

    public int getSampleSteps(){
        return samplesteps;
    }

    public void set(int i,T f){
    	if(data!=null)
    		data.set(i,f);
    }

    public boolean hasNaN(){
        if(data==null)
            return false;
        for(int i=0;i<data.size();i++)
            if(data.get(i)==null)
                return true;
        return false;
    }

}
