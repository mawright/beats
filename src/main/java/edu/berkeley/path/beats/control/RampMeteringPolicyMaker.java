package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.simulator.DemandSet;
import edu.berkeley.path.beats.simulator.InitialDensitySet;
import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.SplitRatioSet;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RampMeteringPolicyMaker {
    // dt should be same across all passed in objects (universal simulation dt)
    RampMeteringPolicySet givePolicy(Network net,
                                     FundamentalDiagramSet fd,
                                     DemandSet demand,
                                     SplitRatioSet splitRatios,
                                     InitialDensitySet ics,
                                     RampMeteringControlSet control,
                                     Double dt,
                                     Properties props);
}
