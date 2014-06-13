package edu.berkeley.path.beats.control.adjoint_glue;

import edu.berkeley.path.beats.control.ReroutePolicyMaker;
import edu.berkeley.path.beats.control.ReroutePolicyProfile;
import edu.berkeley.path.beats.control.ReroutePolicySet;
import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.jaxb.Node;
import edu.berkeley.path.dtapc.dataStructures.HashMapPairCellsDouble;
import edu.berkeley.path.dtapc.dataStructures.PairCells;
import edu.berkeley.path.dtapc.dta_solver.SOPC_Optimizer;
import edu.berkeley.path.dtapc.dta_solver.SO_OptimizerByFiniteDifferences;
import edu.berkeley.path.dtapc.dta_solver.Simulator;
import edu.berkeley.path.dtapc.generalLWRNetwork.DiscretizedGraph;
import edu.berkeley.path.dtapc.generalLWRNetwork.LWR_network;
import edu.berkeley.path.dtapc.generalNetwork.data.JsonDemand;
import edu.berkeley.path.dtapc.generalNetwork.data.JsonJunctionSplitRatios;
import edu.berkeley.path.dtapc.generalNetwork.data.JsonSplitRatios;
import edu.berkeley.path.dtapc.generalNetwork.data.demand.DemandsFactory;
import edu.berkeley.path.dtapc.generalNetwork.graph.*;
import edu.berkeley.path.dtapc.generalNetwork.state.internalSplitRatios.IntertemporalSplitRatios;
import edu.berkeley.path.dtapc.optimization.GradientDescent;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Created by gomes on 2/12/14.
 */
public class AdjointReroutesPolicyMaker implements ReroutePolicyMaker {

    static enum optimizer_types {FINITE_DIFFERENCE,ADJOINT};

    public ReroutePolicySet givePolicy(edu.berkeley.path.beats.jaxb.Network net,
                                       edu.berkeley.path.beats.jaxb.FundamentalDiagramSet fd,
                                       edu.berkeley.path.beats.jaxb.DemandSet demand,
                                       edu.berkeley.path.beats.jaxb.SplitRatioSet splitRatios,
                                       edu.berkeley.path.beats.jaxb.InitialDensitySet ics,
                                       edu.berkeley.path.beats.jaxb.RouteSet routes,
                                       Double dt,
                                       Properties properties) {

        double[] policy = computePolicy(net,
                fd,
                demand,
                splitRatios,
                ics,
                routes,
                dt,
                properties);

        ReroutePolicySet reroutePolicySet = new ReroutePolicySet();
        Double[] policyDouble = ArrayUtils.toObject(policy);
        ReroutePolicyProfile reroutePolicyProfile = new ReroutePolicyProfile();
        reroutePolicyProfile.reroutePolicy = Arrays.asList(policyDouble);
        reroutePolicySet.profiles.add(reroutePolicyProfile);

        return reroutePolicySet;
    }

    public static double[] computePolicy(Network netBeATS,
                                         FundamentalDiagramSet fdBeATS,
                                         DemandSet demandSetBeATS,
                                         SplitRatioSet splitRatiosBeATS,
                                         InitialDensitySet idsBeATS,
                                         RouteSet rsBeATS,
                                         Double dtBeATS ,
                                         Properties properties ) {

	    /* This needs to NOT be hard coded */
        int delta_t = dtBeATS.intValue();
        int time_steps = 800;

        boolean debug_on = properties.getProperty("DEBUG").compareToIgnoreCase("ON")==0;

        double[] result = null;
        try {

            print(debug_on, "Converting BeATS network to a DTAPC network...");
            // Translate the network
            edu.berkeley.path.beats.jaxb.Network network = netBeATS;

            // Read the nodes
            Iterator<Node> node_iterator =
                    network.getNodeList().getNode().iterator();
            HashMap<Integer, Node> BeATS_nodes =
                    new HashMap<Integer, edu.berkeley.path.beats.jaxb.Node>(
                            network.getNodeList().getNode().size());
            edu.berkeley.path.beats.jaxb.Node tmp_node;


            print(debug_on, "");
            print(debug_on, "BeATS Nodes:");
            while (node_iterator.hasNext()) {
                tmp_node = node_iterator.next();
                BeATS_nodes.put((int) tmp_node.getId(), tmp_node);
                print(debug_on, BeATS_nodes.get((int) tmp_node.getId()).getId());
            }
            print(debug_on, "");

            // Read the links
            Iterator<edu.berkeley.path.beats.jaxb.Link> link_iterator =
                    network.getLinkList().getLink().iterator();
            HashMap<Integer, edu.berkeley.path.beats.jaxb.Link> BeATS_links =
                    new HashMap<Integer, edu.berkeley.path.beats.jaxb.Link>(
                            network.getLinkList().getLink().size());
            edu.berkeley.path.beats.jaxb.Link tmp_link;
            print(debug_on,"BeATS links:");
            while (link_iterator.hasNext()) {
                tmp_link = link_iterator.next();
                BeATS_links.put((Integer) (int) tmp_link.getId(), tmp_link);
                print(debug_on,BeATS_links.get((int) tmp_link.getId()).getId());
            }
            print(debug_on,"");

            // Read the Fundamental Diagram profiles
            Iterator<FundamentalDiagramProfile> fdp_iterator =
                    fdBeATS.getFundamentalDiagramProfile().iterator();

            // PATH link id -> FDP
            HashMap<Integer, FundamentalDiagramProfile> fundamentalDiagramProfiles =
                    new HashMap<Integer, FundamentalDiagramProfile>(
                            fdBeATS.getFundamentalDiagramProfile().size());

            print(debug_on,"BeATS FD params:");
            FundamentalDiagramProfile tmp_fdp;
            while (fdp_iterator.hasNext()) {
                tmp_fdp = fdp_iterator.next();
                fundamentalDiagramProfiles.put((int) tmp_fdp.getLinkId(), tmp_fdp);
                print(debug_on,fundamentalDiagramProfiles.get((int) tmp_fdp.getLinkId()).getFundamentalDiagram().get(0).getCapacity() + ", ");
                print(debug_on,fundamentalDiagramProfiles.get((int) tmp_fdp.getLinkId()).getFundamentalDiagram().get(0).getCapacity() + ", ");
                print(debug_on,fundamentalDiagramProfiles.get((int) tmp_fdp.getLinkId()).getFundamentalDiagram().get(0).getCapacity());
                print(debug_on,"");
            }

			/*
			 * Create the internal "mutable "graph:
			 * Add the nodes and keep the mapping to BeATS nodes
			 * Add the links and keep the mapping to BeATS links
			 * Ensure the update of the pointers to nodes and links
			 */
            MutableGraph mutable_graph = new MutableGraph();

            print(debug_on,"");
            print(debug_on,"Mutable graph nodes");
			/* We first add the nodes */
            Iterator<Node> BeATS_nodes_iterator = BeATS_nodes.values().iterator();
            HashMap<Integer, edu.berkeley.path.dtapc.generalNetwork.graph.Node> BeATS_node_to_Mutable_node =
                    new HashMap<Integer, edu.berkeley.path.dtapc.generalNetwork.graph.Node>(BeATS_nodes.size());

            while (BeATS_nodes_iterator.hasNext()) {
                tmp_node = BeATS_nodes_iterator.next();
                mutable_graph.addNode(0, 0);
                BeATS_node_to_Mutable_node.put((int) tmp_node.getId(),
                        mutable_graph.getLastAddedNode());
                print(debug_on,mutable_graph.getLastAddedNode().getUnique_id());
            }
            print(debug_on,"");

            print(debug_on,"Mutable graph links");
			/* We then add the links */
            Iterator<edu.berkeley.path.beats.jaxb.Link> BeATS_links_iterator = BeATS_links.values().iterator();
            HashMap<Integer, edu.berkeley.path.dtapc.generalNetwork.graph.Link> BeATS_link_to_Mutable_link =
                    new HashMap<Integer, edu.berkeley.path.dtapc.generalNetwork.graph.Link>(BeATS_links.size());

            HashMap<Integer,edu.berkeley.path.beats.jaxb.Link> Mutable_link_to_BeATS_link =
                    new HashMap<Integer,edu.berkeley.path.beats.jaxb.Link>(BeATS_links.size());

            while (BeATS_links_iterator.hasNext()) {
                tmp_link = BeATS_links_iterator.next();
                edu.berkeley.path.dtapc.generalNetwork.graph.Node from, to;
                from = BeATS_node_to_Mutable_node.get((int) tmp_link.getBegin().getNodeId());
                assert from != null;
                to = BeATS_node_to_Mutable_node.get((int) tmp_link.getEnd().getNodeId());
                assert to != null;
                mutable_graph.addLink(from, to);
                print(debug_on,mutable_graph.getLastAddedLink().getUnique_id() + " from node: ");
                print(debug_on,mutable_graph.getLastAddedLink().from.getUnique_id() + " to: ");
                print(debug_on,mutable_graph.getLastAddedLink().to.getUnique_id());
                print(debug_on,"");

                BeATS_link_to_Mutable_link.put((int) tmp_link.getId(),
                        mutable_graph.getLastAddedLink());

                Mutable_link_to_BeATS_link.put(mutable_graph.getLastAddedLink().getUnique_id(),tmp_link);

                edu.berkeley.path.dtapc.generalNetwork.graph.Link tmp = mutable_graph.getLastAddedLink();
                tmp.l = tmp_link.getLength();
            }
            print(debug_on,"");

            assert mutable_graph.check() : "We should have nodes[i].id = i and links[i].id = i";

			/*
			 * Iterate through the node and do the following:
			 * - Update the priority at the nodes. The priority is based on the number of
			 *   lanes if the incoming links
			 * - Add origins and destinations
			 */

            print(debug_on,"Origins and destinations");
            // Add origins and destinations
            for (int i = 0; i < mutable_graph.sizeNode(); i++) {
                edu.berkeley.path.dtapc.generalNetwork.graph.Node tmp = mutable_graph.getNode(i);
                if (tmp.incoming_links.size() > 1) {
                    int nb = tmp.incoming_links.size();
                    double total_nb_lanes = 0;
                    for (int j = 0; j < nb; j++) {
                        int id = tmp.incoming_links.get(j).getUnique_id();
                        edu.berkeley.path.beats.jaxb.Link l = Mutable_link_to_BeATS_link.get(id);
                        assert l != null;
                        total_nb_lanes += l.getLanes();
                    }
                    tmp.priorities = new HashMap<Integer, Double>(nb);
                    Vector<edu.berkeley.path.dtapc.generalNetwork.graph.Link> incoming = tmp.incoming_links;
                    for (int link = 0; link < incoming.size(); link++) {
                        int id = tmp.incoming_links.get(link).getUnique_id();
                        edu.berkeley.path.beats.jaxb.Link l = Mutable_link_to_BeATS_link.get(id);
                        tmp.priorities.put(id, l.getLanes() / total_nb_lanes);
                    }
                }
                if (tmp.incoming_links.isEmpty()) {
                    mutable_graph.addSingleBufferSource(tmp);
                    print(debug_on,"Origin: " + tmp.getUnique_id());
                }
                if (tmp.outgoing_links.isEmpty()) {
                    mutable_graph.addSingleBufferDestination(tmp);
                    print(debug_on,"Destination: " + tmp.getUnique_id());
                }
            }

            print(debug_on,"");
            print(debug_on,"Initial densities");
            // We get the initial densities and put them into the Mutable links
            Iterator<Density> BeATS_densities_iterator = idsBeATS.getDensity().iterator();

            Density tmp_density;
            while (BeATS_densities_iterator.hasNext()) {
                tmp_density = BeATS_densities_iterator.next();
                edu.berkeley.path.dtapc.generalNetwork.graph.Link l =
                        BeATS_link_to_Mutable_link.get((int) tmp_density.getLinkId());
                assert l != null;
                l.initial_density =
                        Double.parseDouble(tmp_density.getContent());
                print(debug_on,l.getUnique_id() + ": " + l.initial_density);
            }

            BeATS_links_iterator = BeATS_links.values().iterator();

            print(debug_on,"");
            print(debug_on,"Mutable graph FD params");
			/* We update the fundamental triangular diagram in the links */
			/* The rest of the code currently only handles a single FD values across time */
            while (BeATS_links_iterator.hasNext()) {
                edu.berkeley.path.beats.jaxb.Link tmp = BeATS_links_iterator.next();
                edu.berkeley.path.dtapc.generalNetwork.graph.Link Mutable_link = BeATS_link_to_Mutable_link.get((int) tmp.getId());

                //			    for (int i = 0; i < fundamentalDiagrams.get(tmp).
                //		        		getFundamentalDiagram().size(); i++) {
                FundamentalDiagram tmp_fd = fundamentalDiagramProfiles.get((int) tmp.getId()).
                        getFundamentalDiagram().get(0);
                //			    	Mutable_link.jam_density= tmp_fd.getJamDensity();
                Mutable_link.F_max = tmp_fd.getCapacity();
                Mutable_link.v = tmp_fd.getFreeFlowSpeed();
                Mutable_link.w = tmp_fd.getCongestionSpeed();
                //Mutable_link.dt = fundamentalDiagramProfiles.get((int) tmp.getId()).getDt();
                Mutable_link.jam_density = Mutable_link.F_max/Mutable_link.v + Mutable_link.F_max/Mutable_link.w;
                print(debug_on,"link: " + Mutable_link.getUnique_id());
                print(debug_on," F_max: " + Mutable_link.F_max);
                print(debug_on," v: " + Mutable_link.v);
                print(debug_on," w: " + Mutable_link.w);
                print(debug_on," dt: " + Mutable_link.dt);
                print(debug_on," jam_density: " + Mutable_link.jam_density);
                print(debug_on,"");

            }

            print(debug_on,"");
            print(debug_on,mutable_graph.toString());

            print(debug_on,"Mutable graph paths ");
            // Add paths
            RouteSet BeATS_routeSet = rsBeATS;
            Iterator<Route> BeATS_routes_iterator = BeATS_routeSet.getRoute().iterator();
            while (BeATS_routes_iterator.hasNext()) {
                Route tmpRoute = BeATS_routes_iterator.next();
                ArrayList<Integer> routeList = new ArrayList<Integer>();
                Iterator<RouteLink> routeLink_iterator = tmpRoute.getRouteLink().iterator();
                while (routeLink_iterator.hasNext()) {
                    routeList.add((int) BeATS_link_to_Mutable_link.get((int) routeLink_iterator.next().getLinkId()).getUnique_id());
                }
                Path newpath = new Path((int) BeATS_routeSet.getId(), (int) tmpRoute.getId(), routeList);
                mutable_graph.addPath(newpath);
                Iterator<Integer> pathList_iterator = mutable_graph.getPaths().get((int) tmpRoute.getId()).iterator();
                print(debug_on,tmpRoute.getId() + ": ");
                while (pathList_iterator.hasNext()) {
                    print(debug_on,pathList_iterator.next().intValue() + " ");
                }
                print(debug_on,"");
            }

            Graph graph = new Graph(mutable_graph);

            DiscretizedGraph discretized_graph = new DiscretizedGraph(graph, delta_t,time_steps);

            // Add internal split ratios
            print(debug_on,"");
            print(debug_on,"Discretized graph split ratios");
            // We get the non-compliant split ratios
            //		    assert scenario.getSplitRatioSet().getSplitRatioProfile().size() == ? :
            //		    	"Number of split ratio profiles should be equal to total commodities, which is
            //		   		 non-compliants plus all routes";
            double split_ratios_dt = splitRatiosBeATS
                    .getSplitRatioProfile()
                    .get(0).getDt();
            print(debug_on,"dt =" + split_ratios_dt);

            LinkedList<HashMapPairCellsDouble> SR_list =
                    new LinkedList<HashMapPairCellsDouble>();

            Iterator<Splitratio> non_compliant_SR_iterator =
                    splitRatiosBeATS
                            .getSplitRatioProfile()
                            .get(0).getSplitratio()
                            .iterator();

            Splitratio tmp_SR;
            JsonJunctionSplitRatios[] NC_split_ratios;
            JsonSplitRatios[] Json_SR = new JsonSplitRatios[1];
            while (non_compliant_SR_iterator.hasNext()) {
                tmp_SR = non_compliant_SR_iterator.next();
                if (tmp_SR.getVehicleTypeId() == 0) {
                    print(debug_on,"Non-compliant split ratios:");
                    HashMapPairCellsDouble non_compliant_split_ratios;
                    List<String> history =
                            new ArrayList<String>(Arrays.asList(tmp_SR.getContent().split(",")));
                    double[] history_table = new double[history.size()];
                    for (int i = 0; i < history_table.length; i++) {
                        history_table[i] = Double.parseDouble(history.get(i));
                    }
                    NC_split_ratios = new JsonJunctionSplitRatios[history_table.length];
                    for (int k = 0; k < history_table.length; k++) {
                        if (k < SR_list.size()) {
                            non_compliant_split_ratios = SR_list.get(k);
                        }
                        else {
                            non_compliant_split_ratios = new HashMapPairCellsDouble();
                            SR_list.addLast(non_compliant_split_ratios);
                        }
                        NC_split_ratios[k] = new JsonJunctionSplitRatios(k, (int) BeATS_link_to_Mutable_link.get((int) tmp_SR.getLinkIn()).getUnique_id(),
                                (int) BeATS_link_to_Mutable_link.get((int) tmp_SR.getLinkOut()).getUnique_id(), (int) tmp_SR.getVehicleTypeId(), history_table[k]);

                        non_compliant_split_ratios.put(
                                new PairCells((int) BeATS_link_to_Mutable_link.get( (int)tmp_SR.getLinkIn()).getUnique_id(), BeATS_link_to_Mutable_link.get( (int) tmp_SR.getLinkOut()).getUnique_id()),
                                history_table[k]);
                        print(debug_on,"beta: " + NC_split_ratios[k].beta + " ");
                        print(debug_on,"c: " + NC_split_ratios[k].c + " ");
                        print(debug_on,"in_id: " + NC_split_ratios[k].in_id + " ");
                        print(debug_on,"out_id: " + NC_split_ratios[k].out_id + " ");
                        print(debug_on,"k: " + NC_split_ratios[k].k + " ");
                        print(debug_on,"");
                    }
                    print(debug_on,"");
                    Json_SR[0] = new JsonSplitRatios((int) BeATS_node_to_Mutable_node.get((int) splitRatiosBeATS
                            .getSplitRatioProfile().get(0).getNodeId()).getUnique_id(), NC_split_ratios);
                    print(debug_on,"Non-compliant split ratio node id:" + Json_SR[0].node_id);
                }
            }
            print(debug_on,"");
            HashMapPairCellsDouble[] SR_array =
                    new HashMapPairCellsDouble[SR_list.size()];
            SR_list.toArray(SR_array);

            IntertemporalSplitRatios intTempSR = discretized_graph.split_ratios;
            intTempSR.addNonCompliantSplitRatios(discretized_graph, Json_SR);

            // Now we build the discretized network
            LWR_network lwr_network = new LWR_network(discretized_graph);

            // Creating the simulator
            double alpha = 0.5; // Fraction of compliant flow
            Simulator simulator = new Simulator(delta_t, time_steps, alpha);
            simulator.discretized_graph = discretized_graph;
            simulator.lwr_network = lwr_network;

            // Demand set
            Iterator<DemandProfile> demandProfile_iterator =
                    demandSetBeATS.getDemandProfile().iterator();
            // For now we deal with only one origin
            assert demandSetBeATS.getDemandProfile().size() == 1;

            DemandProfile tmp_demandProfile;
            double[] totalDemand = new double[time_steps];
            for (int i = 0; i < time_steps; i++){
                totalDemand[i] = 0;
            }
            double profile_dt = Double.NaN;
            while (demandProfile_iterator.hasNext()) { // There is only one for now
                tmp_demandProfile = demandProfile_iterator.next();

                Iterator<Demand> demand_iterator = tmp_demandProfile.getDemand().iterator();
                Demand tmp_demand;
                // We have the discretization
                profile_dt = tmp_demandProfile.getDt();
                int origin_id = (int) tmp_demandProfile.getLinkIdOrg();

                print(debug_on,"Demand for origin " + origin_id + " dt = " + dtBeATS);

                while (demand_iterator.hasNext()) {
                    tmp_demand = demand_iterator.next();

                    List<String> history =
                            new ArrayList<String>(Arrays.asList(tmp_demand.getContent().split(
                                    ",")));
                    double[] history_table = new double[history.size()];
                    for (int i = 0; i < history_table.length; i++){
                        history_table[i] = Double.parseDouble(history.get(i));
                        totalDemand[i] = totalDemand[i] + history_table[i];
                    }
                }
            }

            JsonDemand[] json_demands = new JsonDemand[1];
            json_demands[0] = new JsonDemand((int) mutable_graph.getLink((int) BeATS_link_to_Mutable_link.get((int) demandSetBeATS.getDemandProfile().
                    get(0).getLinkIdOrg()).from.getUnique_id()).getUnique_id(), totalDemand);
            simulator.origin_demands = new DemandsFactory(simulator.time_discretization, profile_dt, json_demands, discretized_graph.node_to_origin).buildDemands();
            print(debug_on,simulator.origin_demands.toString());
            print(debug_on,"");
            simulator.initializSplitRatios();

            print(debug_on,"");
            print(debug_on,simulator.splits.toString());

			/* Checking the requirements on the network */
            print(debug_on,"Checking that the network respect needed requirements...");
            lwr_network.checkConstraints(delta_t);

            if(debug_on)
                lwr_network.print();
            print(debug_on,"Done");
            print(debug_on,"No control cost: " + simulator.objective() + "\n");

            SOPC_Optimizer optimizer = null;

            switch(AdjointReroutesPolicyMaker.optimizer_types.valueOf(properties.getProperty("OPTIMIZER_TYPE"))){
                case FINITE_DIFFERENCE:
                    optimizer = new SO_OptimizerByFiniteDifferences(simulator);
                    break;
                case ADJOINT:
                    optimizer = new SOPC_Optimizer(simulator);
                    break;
                default:
                    // ERROR OUT
                    break;
            }

            GradientDescent descentMethod = new GradientDescent(
                    Integer.parseInt(properties.getProperty("MAX_ITERATIONS")) ,
                    properties.getProperty("DESCENT_METHOD"));
            descentMethod.setGradient_condition(
                    Double.parseDouble(properties.getProperty("STOPPING_CRITERIA")) );
            result = descentMethod.solve(optimizer);

            print(debug_on,"Final control");
            for (int i = 0; i < result.length; i++)
                print(debug_on,result[i]);

            print(debug_on,"Test done!");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private static void print(boolean debug,Object x){
        if(debug)
            System.out.println(x.toString());
    }
}
