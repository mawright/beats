package edu.berkeley.path.beats.simulator.scenarioUpdate;

import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorCTM;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorQueue;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorQueueAndTravelTime;
import edu.berkeley.path.beats.simulator.nodeBeahavior.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

/**
 * Created by gomes on 10/26/14.
 */
public abstract class ScenarioUpdaterAbstract implements ScenarioUpdaterInterface {

    protected Scenario scenario;

    protected TypeNodeFlowSolver nodeflowsolver;
    protected TypeNodeSplitSolver nodesrsolver;

    public ScenarioUpdaterAbstract(Scenario scenario,String nodeflowsolver_name,String nodesrsolver_name){
        this.scenario = scenario;

        this.nodeflowsolver = nodeflowsolver_name==null ?
                TypeNodeFlowSolver.proportional :
                TypeNodeFlowSolver.valueOf(nodeflowsolver_name);

        this.nodesrsolver = nodesrsolver_name==null ?
                TypeNodeSplitSolver.A :
                TypeNodeSplitSolver.valueOf(nodesrsolver_name);

    }

    @Override
    public void populate() {

        for( edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork() ){

            // set link behaviors
            for(edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink()){
                Link blink = (Link)link;
                blink.link_behavior =  create_link_behavior(blink);
            }

            // set node behaviors
            for(edu.berkeley.path.beats.jaxb.Node node : network.getNodeList().getNode()){
                Node bNode = (Node)node;
                bNode.node_behavior = new NodeBehavior( bNode,
                                                        create_node_sr_solver(bNode) ,
                                                        create_node_flow_solver(bNode) ,
                                                        create_node_supply_partitioner(bNode) );
            }
        }
    }

    protected LinkBehaviorCTM create_link_behavior(Link link){
        LinkBehaviorCTM link_behavior;
        switch(link.link_type){
            case freeway:
                link_behavior = new LinkBehaviorCTM(link);
                break;
            case intersection_approach:
                link_behavior = new LinkBehaviorQueue(link);
                break;
            case street:
                link_behavior = new LinkBehaviorQueueAndTravelTime(link);
                break;
            default:
                link_behavior = new LinkBehaviorCTM(link);
                break;
        }
        return link_behavior;
    }

    protected Node_SplitRatioSolver create_node_sr_solver(Node node){

        // create node split ratio solver
        Node_SplitRatioSolver node_sr_solver;
        switch(nodesrsolver){
            case A:
            case greedy:
                node_sr_solver = new Node_SplitRatioSolver_Greedy(node);
                break;
            case HAMBURGER:
                Parameters param = (Parameters) node.getNodeType().getParameters();
                if(param!=null && param.has("threshold") && param.has("scaling_factor"))
                    node_sr_solver = new Node_SplitRatioSolver_HAMBURGER(node);
                else
                    node_sr_solver = new Node_SplitRatioSolver_Greedy(node);
                break;
            case balancing:
                node_sr_solver = new Node_SplitRatioSolver_Balancing(node);
                break;
            default:
                node_sr_solver = new Node_SplitRatioSolver_Greedy(node);
        }
        return node_sr_solver;
    }

    protected Node_FlowSolver create_node_flow_solver(Node node){
        Node_FlowSolver node_flow_solver;
        switch(nodeflowsolver) {
            case proportional:
                node_flow_solver = new Node_FlowSolver_LNCTM(node);
                break;
            case symmetric:
            case tampere:
                node_flow_solver = new Node_FlowSolver_Symmetric(node);
                break;
            case general:
                node_flow_solver = new Node_FlowSolver_General(node);
                break;
            default:
                node_flow_solver = new Node_FlowSolver_LNCTM(node);
        }
        return node_flow_solver;
    }

    protected Node_SupplyPartitioner create_node_supply_partitioner(Node node){
        return new Node_SupplyPartitioner(node);
    }

    /////////////////////////////////////
    // helper methods
    /////////////////////////////////////

    protected void update_supply_demand(Network network) throws BeatsException {
        if(network.isempty)
            return;
        for(edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink()){
            Link bLink = ((Link)link);
            bLink.updateOutflowDemand();
            bLink.link_behavior.update_total_space_supply();
            bLink.link_behavior.update_available_space_supply();
        }
    }

    protected void update_flow(Network network) throws BeatsException {
        if(network.isempty)
            return;
        for (edu.berkeley.path.beats.jaxb.Node node : network.getNodeList().getNode())
            ((Node) node).update_flows();
    }

    protected void update_density(Network network) throws BeatsException {
        if(network.isempty)
            return;
        for(edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink())
            ((Link)link).update_densities();
    }

    protected void update_profiles() throws BeatsException {

        // sample profiles .............................
        if(scenario.getDownstreamBoundaryCapacitySet()!=null)
            ((CapacitySet)scenario.getDownstreamBoundaryCapacitySet()).update();

        if(scenario.getDemandSet()!=null)
            ((DemandSet)scenario.getDemandSet()).update();

        if(scenario.getSplitRatioSet()!=null)
            ((SplitRatioSet) scenario.getSplitRatioSet()).update();

        if(scenario.getFundamentalDiagramSet()!=null)
            for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fdProfile : scenario.getFundamentalDiagramSet().getFundamentalDiagramProfile())
                ((FundamentalDiagramProfile) fdProfile).update(false,scenario.get.clock());
    }

    protected void update_sensors_control_events() throws BeatsException{

        // update sensor readings .......................
        scenario.sensorset.update();

        // update controllers
        scenario.controllerset.update();

        // update and deploy actuators
        scenario.actuatorset.deploy(scenario.get.currentTimeInSeconds());

        // update events
        scenario.eventset.update();

    }

    protected void update_cumalitives_clock() throws BeatsException {

        scenario.cumulatives.update();

        if(scenario.perf_calc!=null)
            scenario.perf_calc.update();

        // advance the clock
        scenario.clock.advance();

    }

}
