package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.jaxb.DemandSet;
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.jaxb.InitialDensitySet;
import edu.berkeley.path.beats.jaxb.Network;
import edu.berkeley.path.beats.jaxb.RouteSet;
import edu.berkeley.path.beats.jaxb.SplitRatioSet;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ReroutePolicyMaker {
    // dt should be same across all passed in objects (universal simulation dt)
    ReroutePolicySet givePolicy(Network net,
                                     FundamentalDiagramSet fd,
                                     DemandSet demand,
                                     SplitRatioSet splitRatios,
                                     InitialDensitySet ics,
                                     RouteSet routes,
                                     Double dt ,
                                     Double horizon_length,
                                     Properties properties);
}
