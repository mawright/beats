package edu.berkeley.path.beats.control.rm_interface;

import edu.berkeley.path.beats.simulator.Link;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class RampMeteringControl implements Serializable{
    private static final long serialVersionUID = -1920802709971458938L;
    public double min_rate; // flux in relation to max rate of link
    public double max_rate; // flux in relation to max rate of link
    public Link link;
    public RampMeteringControl(Link L){
        max_rate = Double.POSITIVE_INFINITY;
        min_rate = 0d;
        link = L;
    }
}
