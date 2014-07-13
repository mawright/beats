package edu.berkeley.path.beats.control.adjoint_glue;

import edu.berkeley.path.beats.control.*;
import edu.berkeley.path.beats.jaxb.Density;
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.ramp_metering.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
        public int nLinks = 0;

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
            for (edu.berkeley.path.beats.jaxb.Link ll : l.getBegin_node().getInput_link()) {
                Link lll = (Link) ll;
                if (lll.getLinkType().getName().equalsIgnoreCase("Source")) {
                    return lll;
                }
            }
            return null;
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
    public RampMeteringPolicySet givePolicy(Network net, FundamentalDiagramSet fd, DemandSet demand, SplitRatioSet splitRatios, InitialDensitySet ics, RampMeteringControlSet control, Double dt) {
//        ScenarioMainlinePair pair = convertScenario(net, fd, demand, splitRatios, ics, control, dt);
//        FreewayScenario scenario = pair.scenario;
//        MainlineStructure mainlineStructure = pair.mainlineStructure;
//        int origT = scenario.simParams().numTimesteps();
//        SimulationOutput simstate = FreewaySimulator.simpleSim(scenario);
//        while (!networkSufficientlyCleared(scenario, simstate)) {
//            scenario = scenario.expandSimTime(.3);
//            simstate = FreewaySimulator.simpleSim(scenario);
//        }
//        Adjoint$.MODULE$.optimizer_$eq(new ChainedOptimizer());
//        double[][] controlValue = new AdjointRampMetering(scenario.fw()).givePolicy(scenario.simParams(), scenario.policyParams());
//        simstate = FreewaySimulator.simpleSim(scenario, flatten(controlValue));
//        RampMeteringPolicySet policySet = new RampMeteringPolicySet();
//
//        // Start from 1 because we don't think of the first onramp as controllable
//        List<Integer> onrampIndices = mainlineStructure.onrampIndices();
//        for (int i = 1; i <  onrampIndices.size(); ++i) {
//            int rampIndex = onrampIndices.get(i);
//            RampMeteringPolicyProfile policy = new RampMeteringPolicyProfile();
//            policySet.profiles.add(policy);
//            policy.sensorLink = mainlineStructure.mainlineOnrampMap.get(mainlineStructure.links.get(onrampIndices.get(i)));
//            double maxFlux = (Double) (scenario.fw().rMaxs().apply(rampIndex));
//            double lowerLimitFlux = 0.0;
//            double upperLimitFlux = Double.MAX_VALUE;
//            for (RampMeteringControl limit : control.control) {
//                if (limit.link.equals(policy.sensorLink)) {
//                    lowerLimitFlux = limit.min_rate;
//                    upperLimitFlux = limit.max_rate;
//                    break;
//                }
//            }
//            for (int t = 0; t < origT; ++t) {
//                double uValue = controlValue[t][i - 1];
//                double rampFlux = simstate.fluxRamp()[t][rampIndex];
//                double maxQueueFlux = simstate.queue()[t][rampIndex] / scenario.policyParams().deltaTimeSeconds();
//                if (uValue >= 1.0 || rampFlux >= maxQueueFlux || maxQueueFlux <= 0.1 * maxFlux || rampFlux >= .95 * maxFlux) {
//                    policy.rampMeteringPolicy.add(Math.max(maxFlux, lowerLimitFlux));
//                    continue;
//                }
//                policy.rampMeteringPolicy.add(Math.min(upperLimitFlux, Math.max(rampFlux, lowerLimitFlux)));
//            }
//        }
//        return policySet;
        return null;
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
//        MainlineStructure mainline = new MainlineStructure(net);
//        Map<Link, FundamentalDiagram> fdMap = new HashMap<Link, FundamentalDiagram>();
//        for (edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile f : fd.getFundamentalDiagramProfile()) {
//            fdMap.put(net.getLinkWithId(f.getLinkId()), (FundamentalDiagram) (f.getFundamentalDiagram().get(0)));
//        }
//        FreewayLink[] freewayLinks = new FreewayLink[mainline.nLinks];
//        int linkIndex = 0;
//        for (Link link : mainline.links) {
//            double length = link.getLength();
//            double p = 4.0;
//            FundamentalDiagram f = fdMap.get(link);
//            double rMax = 0.0;
//            if (linkIndex == 0) {
//                Link source = mainline.mainlineSourceMap.get(mainline.links.get(0));
//                rMax = fdMap.get(source).getCapacity() * source.getLanes();
//            }
//            if (mainline.mainlineOnrampMap.containsKey(link)) {
//                Link onramp = mainline.mainlineOnrampMap.get(link);
//                rMax = fdMap.get(onramp).getCapacity() * onramp.getLanes();
//            }
//            freewayLinks[linkIndex] = new FreewayLink(new edu.berkeley.path.ramp_metering.FundamentalDiagram(f.getFreeFlowSpeed(), f.getCapacity() * link.getLanes(), f.getJamDensity() * link.getLanes()), length, rMax, p);
//            ++linkIndex;
//        }
//        List<Integer> onrampList = mainline.onrampIndices();
//        List<Integer> offrampList = mainline.offrampIndices();
//        int[] onramps = new int[onrampList.size()];
//        int[] offramps = new int[offrampList.size()];
//        for (int i = 0; i < onrampList.size(); ++i) {
//            onramps[i] = onrampList.get(i);
//        }
//        for (int i = 0; i < offrampList.size(); ++i) {
//            offramps[i] = offrampList.get(i);
//        }
//        Freeway freeway = Freeway.fromArrays(freewayLinks, onramps, offramps);
        PolicyParameters policyParameters = null; //new PolicyParameters(dt, -1);
//        assert demand.getDemandProfile().get(0).getDt() % Math.floor(dt) == 0;
//        int bcDtFactor = (int) (Math.floor(demand.getDemandProfile().get(0).getDt())) / (int) Math.floor(dt);
//        Map<Link, double[]> indexedDemand = new HashMap<Link, double[]>();
//        for (edu.berkeley.path.beats.jaxb.DemandProfile d : demand.getDemandProfile()) {
//            String[] splits = d.getDemand().get(0).getContent().split(",");
//            double[] dem = new double[splits.length * bcDtFactor];
//            int iter = 0;
//            for (int i = 0; i < splits.length; ++i) {
//                double v = Double.parseDouble(splits[i]);
//                for (int j = 0; j < bcDtFactor; ++j) {
//                    dem[iter] = v;
//                    ++iter;
//                }
//            }
//            indexedDemand.put(net.getLinkWithId(d.getLinkIdOrg()), dem);
//        }
//        Map<Link, double[]> indexedRatios = new HashMap<Link, double[]>();
//        for (edu.berkeley.path.beats.jaxb.SplitRatioProfile d : splitRatios.getSplitRatioProfile()) {
//            for (edu.berkeley.path.beats.jaxb.Splitratio split : d.getSplitratio()) {
//                String[] splits = split.getContent().split(",");
//                double[] dem = new double[splits.length * bcDtFactor];
//                int iter = 0;
//                for (int i = 0; i < splits.length; ++i) {
//                    double v = 1 - Double.parseDouble(splits[i]);
//                    for (int j = 0; j < bcDtFactor; ++j) {
//                        dem[iter] = v;
//                        ++iter;
//                    }
//                }
//                indexedRatios.put(net.getLinkWithId(split.getLinkOut()), dem);
//            }
//        }
//        int t = indexedDemand.values().iterator().next().length;
//        double[][] splits = new double[offramps.length][t];
//        for (int i = 0; i < offramps.length; ++i) {
//            splits[i] = indexedRatios.get(mainline.mainlineOfframpMap.get(mainline.links.get(offramps[i])));
//        }
//        double[][] dems = new double[onramps.length][t];
//        for (int i = 0; i < onramps.length; ++i) {
//            dems[i] = indexedDemand.get(mainline.mainlineSourceMap.get(mainline.links.get(onramps[i])));
//        }
//        double[] densityIC = new double[mainline.nLinks];
//        double[] queueIC = new double[mainline.nLinks];
//        for ( Density d : ics.getDensity()) {
//            double value = Double.parseDouble(d.getContent());
//            Link link = net.getLinkWithId(d.getLinkId());
//            if (mainline.links.contains(link)) {
//                densityIC[mainline.links.indexOf(link)] = value;
//                continue;
//            }
//            if (mainline.mainlineOnrampMap.containsValue(link)) {
//                for (int i = 0; i < mainline.nLinks; ++i) {
//                    if (mainline.mainlineOnrampMap.containsKey(mainline.links.get(i)) && mainline.mainlineOnrampMap.get(mainline.links.get(i)).equals(link)) {
//                        queueIC[i] = value;
//                        continue;
//                    }
//                }
//                continue;
//            }
//        }
//        for ( Density d : ics.getDensity()) {
//            double value = Double.parseDouble(d.getContent());
//            Link link = net.getLinkWithId(d.getLinkId());
//            if (mainline.mainlineSourceMap.containsValue(link)) {
//                for (int i = 0; i < mainline.nLinks; ++i) {
//                    if (mainline.mainlineSourceMap.containsKey(mainline.links.get(i)) && mainline.mainlineSourceMap.get(mainline.links.get(i)).equals(link)) {
//                        queueIC[i] = value;
//                        break;
//                    }
//                }
//            }
//        }
//        double[] minRates = new double[onramps.length - 1];
//        double[] maxRates = new double[onramps.length - 1];
//        List<Link> orderedOnramps = mainline.orderedOnramps();
//        for (RampMeteringControl c : control.control) {
//            Link l = c.link;
//            int index = orderedOnramps.indexOf(l);
//            minRates[index] = c.min_rate;
//            maxRates[index] = c.max_rate;
//        }
//
//        SimulationParameters simParams = SimulationParameters.fromJava(BoundaryConditions.fromArrays(dems, splits), InitialConditions.fromArrays(densityIC, queueIC), minRates, maxRates);
//        return new ScenarioMainlinePair(new FreewayScenario(freeway, simParams, policyParameters), mainline);

           return null;
    }


}
