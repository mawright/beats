package edu.berkeley.path.beats.control.adjoint_glue;

import edu.berkeley.path.beats.control.*;
import edu.berkeley.path.beats.jaxb.Density;
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.ramp_metering.*;

import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 1/15/14
 * Time: 10:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class AdjointRampMeteringPolicyMaker implements RampMeteringPolicyMaker {


    static public class MainlineStructure {

        private List<Link> links = new LinkedList<Link>();
        private Map<Link, Link> mainlineOnrampMap = new HashMap<Link, Link>();
        private Map<Link, Link> mainlineSourceMap = new HashMap<Link, Link>();
        private Map<Link, Link> mainlineOfframpMap = new HashMap<Link, Link>();
        private Network network;
        public int nLinks  = 0;

        public MainlineStructure(Network network) {
            super();
            this.network = network;
            populate();
        }

        public List<Link> orderedOnramps() {
            List<Link> v = new LinkedList<Link>();
            for (Link l : links) {
                if (mainlineOnrampMap.containsKey(l)) {
                    v.add(mainlineOnrampMap.get(l));
                }
            }
            return v;
        }

        public List<Link> orderedSources() {
            List<Link> v = new LinkedList<Link>();
            for (Link l : links) {
                if (mainlineSourceMap.containsKey(l)) {
                    v.add(mainlineSourceMap.get(l));
                }
            }
            return v;
        }

        private void populate() {
            for (edu.berkeley.path.beats.jaxb.Link jaxBlink: this.network.getListOfLinks()) {
                Link l = (Link) jaxBlink;
                if (isMainlineSource(l)) {
                    addMainlineLink(l);
                    break;
                }
            }
            if(links.size()!=1)
                System.err.println("ERROR: Multiple mainline sources!");
            Link l = links.get(0);
            while (true) {
                l = nextMainline(l);
                if (l == null) {
                    break;
                }
                addMainlineLink(l);
            }
            l = links.get(0);
            mainlineSourceMap.put(l, source(l));
            for (int i = 1; i < links.size(); ++i) {
                l = links.get(i);
                Link or = onramp(l);
                if (or != null) {
                    mainlineOnrampMap.put(l, or);
                    mainlineSourceMap.put(l,source(or));
                }
                Link off = offramp(l);
                if (off != null) {
                    mainlineOfframpMap.put(links.get(links.indexOf(l)), off);
                }
            }
        }

        public Link source(Link l) {

            // GG: Hack to make it work in L0
            return l;

//            for (edu.berkeley.path.beats.jaxb.Link ll : l.getBegin_node().getInput_link()) {
//                Link lll = (Link) ll;
//                if (lll.getLinkType().getName().equalsIgnoreCase("Source")) {
//                    return lll;
//                }
//            }
//            return null;
        }

        public Link onramp(Link l) {
            for (edu.berkeley.path.beats.jaxb.Link ll : l.getBegin_node().getInput_link()) {
                Link lll = (Link) ll;
                if (lll.getLinkType().getName().equalsIgnoreCase("On-Ramp")) {
                    return lll;
                }
            }
            return null;
        }

        public Link offramp(Link l) {
            for (edu.berkeley.path.beats.jaxb.Link ll : l.getEnd_node().getOutput_link()) {
                Link lll = (Link) ll;
                if (lll.getLinkType().getName().equalsIgnoreCase("Off-Ramp")) {
                    return lll;
                }
            }
            return null;
        }

        public void addMainlineLink(Link link) {
            this.links.add(link);
            ++nLinks;
        }

        public List<Integer> onrampIndices() {
            List<Integer> list = new LinkedList<Integer>();
            for (int i = 0; i < links.size(); ++i) {
                if (mainlineSourceMap.containsKey(links.get(i))) {
                    list.add(i);
                }
            }
            return list;
        }

        public List<Integer> offrampIndices() {
            List<Integer> list = new LinkedList<Integer>();
            for (int i = 0; i < links.size(); ++i) {
                if (mainlineOfframpMap.containsKey(links.get(i))) {
                    list.add(i);
                }
            }
            return list;
        }

        static private Link nextMainline(Link link) {
            for (edu.berkeley.path.beats.jaxb.Link l : link.getEnd_node().getOutput_link()) {
                Link downLink = (Link) l;
                if (downLink.isFreeway()) {
                    return downLink;
                }
            }
            return null;
        }


        static private boolean isMainlineSource(Link link) {
            if (!link.isFreeway()) {
                return false;
            }
            for (edu.berkeley.path.beats.jaxb.Link l : link.getBegin_node().getInput_link()) {
                if (((Link) l).isFreeway()) {
                    return false;
                }
            }
            return true;
        }
    }

    static public class ScenarioMainlinePair {
        public final FreewayScenario scenario;
        public final MainlineStructure mainlineStructure;

        public ScenarioMainlinePair(FreewayScenario scenario, MainlineStructure mainlineStructure) {
            super();
            this.scenario = scenario;
            this.mainlineStructure= mainlineStructure;
        }
    }

    @Override
    public RampMeteringPolicySet givePolicy(Network net, FundamentalDiagramSet fd, DemandSet demand, SplitRatioSet splitRatios, InitialDensitySet ics, RampMeteringControlSet control, Double dt,Properties props) {
        AdjointRampMetering metering = new AdjointRampMetering(scenario);
        if (props != null) {
            metering.setProperties(props);
        }
        double[][] controlValue = metering.givePolicy();
    }

    private double[] flatten(double[][] controlValue) {
        int n = controlValue.length;
        int m = controlValue[0].length;
        double[] newArray = new double[n * m];
        int index = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                newArray[index++] = controlValue[i][j];
            }
        }
        return newArray;
    }

    private boolean networkSufficientlyCleared(FreewayScenario scenario, SimulationOutput simstate) {
        double vehiclesAtEnd = FreewaySimulator.totalVehiclesEnd(scenario.fw(), simstate, scenario.policyParams().deltaTimeSeconds());
        double totalSimulatedVehicles =  scenario.totalVehicles();
        return vehiclesAtEnd <= totalSimulatedVehicles * .01 + .1;

    }

    static public ScenarioMainlinePair convertScenario(Network net, FundamentalDiagramSet fd, DemandSet demand, SplitRatioSet splitRatios, InitialDensitySet ics, RampMeteringControlSet control, Double dt) {
        PolicyParameters policyParameters = new PolicyParameters(dt, -1, 0);

        SimulationParameters simParams = SimulationParameters.fromJava(BoundaryConditions.fromArrays(dems, splits), InitialConditions.fromArrays(densityIC, queueIC), minRates, maxRates);
        return new ScenarioMainlinePair(new FreewayScenario(freeway, simParams, policyParameters), mainline);
    }


}
