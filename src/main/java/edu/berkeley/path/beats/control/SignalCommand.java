package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.NEMA;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: gomes
 * Date: 3/6/14
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("rawtypes")
public class SignalCommand implements Comparable,Serializable {

    private static final long serialVersionUID = 8063530719559558233L;

    public static enum Type {hold,forceoff};

    public Type type;
    public NEMA.ID nema;
    public Double time;
    public Double yellowtime;
    public Double redcleartime;
    //public boolean is_first = false;

    public SignalCommand(Type type,NEMA.ID phase,double time){
        this.type = type;
        this.nema = phase;
        this.time = time;
        this.yellowtime = Double.NaN;
        this.redcleartime = Double.NaN;
    }

    public SignalCommand(Type type,NEMA.ID phase,double time,double yellowtime,double redcleartime){
        this.type = type;
        this.nema = phase;
        this.time = time;
        this.yellowtime = yellowtime;
        this.redcleartime = redcleartime;
    }

    @Override
    public int compareTo(Object arg0) {

        if(arg0==null)
            return 1;

        int compare;
        SignalCommand that = (SignalCommand) arg0;

        // first ordering by time stamp
        Double thiststamp = this.time;
        Double thattstamp = that.time;
        compare = thiststamp.compareTo(thattstamp);
        if(compare!=0)
            return compare;

        // second ordering by phases
        NEMA.ID thistphase = this.nema;
        NEMA.ID thattphase = that.nema;
        compare = thistphase.compareTo(thattphase);
        if(compare!=0)
            return compare;

        // third ordering by type
        Type thisttype = this.type;
        Type thatttype = that.type;
        compare = thisttype.compareTo(thatttype);
        if(compare!=0)
            return compare;

        // fourth ordering by yellowtime
        Double thistyellowtime = this.yellowtime;
        Double thattyellowtime = that.yellowtime;
        compare = thistyellowtime.compareTo(thattyellowtime);
        if(compare!=0)
            return compare;

        // fifth ordering by redcleartime
        Double thistredcleartime = this.redcleartime;
        Double thattredcleartime = that.redcleartime;
        compare = thistredcleartime.compareTo(thattredcleartime);
        if(compare!=0)
            return compare;

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==null)
            return false;
        else
            return this.compareTo((SignalCommand) obj)==0;
    }

    @Override
    public String toString() {
        return time + ": " + type.toString() + " " + nema + " (y=" + yellowtime + ",r=" + redcleartime + ")";
    }
}
