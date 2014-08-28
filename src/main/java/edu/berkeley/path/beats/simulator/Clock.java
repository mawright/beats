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

public final class Clock {

    private final double to;    // [sec after midnight] reset time
    private final int step_o;   // [-] # time steps from midnight to to
    private final double dt;    // [sec] time step
    private final double maxt;	// [sec after midnight] final time
	private double t;			// [sec after midnight] current time
    private int rel_step;		// [-] # time steps after to

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Clock(double to,double tf,double dt){
		this.to = to;
        this.step_o = BeatsMath.round(to/dt);
		this.dt = dt;
        this.maxt = tf;
        reset();
	}

    /////////////////////////////////////////////////////////////////////
    // reset / advance
    /////////////////////////////////////////////////////////////////////

    public void reset(){
        rel_step = 0;
		t = to;
	}

    public void advance(){
        rel_step++;
        t = to + rel_step*dt;
    }

    /////////////////////////////////////////////////////////////////////
    // sampling
    /////////////////////////////////////////////////////////////////////

    public boolean is_time_to_sample_rel(int dt_steps){
        if(rel_step<=1)
            return true;
        return rel_step % dt_steps == 0;
    }

    public boolean is_time_to_sample_abs(int dt_steps,int step_initial_abs){
        //if(rel_step<=1)
        if(rel_step==0)
            return true;
        int abs_step = rel_step + step_o;
        if(abs_step<step_initial_abs)
            return false;
        return (abs_step-step_initial_abs) % dt_steps == 0;
    }

    public int sample_index_abs(int dt_steps,int step_initial_abs){
        return dt_steps>0 ? BeatsMath.floor((rel_step+step_o-step_initial_abs)/((float)dt_steps)) : 0;
    }

    /////////////////////////////////////////////////////////////////////
    // getters
    /////////////////////////////////////////////////////////////////////

    /** current time in seconds **/
	public double getT() {
		return t;
	}

    public double getTElapsed(){
		return t-to;
	}

	/** time steps since beginning of simulation */
    public int getRelativeTimeStep() {
		return rel_step;
	}

    public int getAbsoluteTimeStep() {
        return rel_step+step_o;
    }

//	protected int getTotalSteps(){
//		if(Double.isInfinite(maxt))
//			return -1;
//		return (int) Math.ceil((maxt-to)/dt);
//	}

    public boolean expired(){
		return t>maxt;
	}

    public double getStartTime(){
		return to;
	}

	public double getEndTime(){
		return maxt;
	}

    public double getDt(){
        return dt;
    }
    
//	public void print(){
//		System.out.println("t=" + t + "\t\tstep=" + currentstep);
//	}
    
    /////////////////////////////////////////////////////////////////////
    // setter
    /////////////////////////////////////////////////////////////////////
    
    /** set to a specific timestep **/
    protected void setRelativeTimeStep(int step){
    	rel_step = step;
    }
}
