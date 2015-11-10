package edu.berkeley.path.beats.simulator.scenarioUpdate;

import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorACTM;
import edu.berkeley.path.beats.simulator.linkBehavior.LinkBehaviorCTM;
import edu.berkeley.path.beats.simulator.nodeBeahavior.Node_FlowSolver;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 10/26/14.
 */
public class ScenarioUpdaterACTM extends ScenarioUpdaterAbstract implements Serializable {

    private static final long serialVersionUID = -6303015223303097613L;
    private List<FwyNode> fwy_nodes;

    private static double xi = 0.1;
    private static double gamma = 4d;

    public ScenarioUpdaterACTM(Scenario scenario){
        super(scenario,null,"A");
    }

    @Override
    public void populate() {
        super.populate();

        // find first node
        List<Node> first_fwy_nodes = ((Network)scenario.getNetworkSet().getNetwork().get(0)).get_terminal_freeway_nodes();

        if(first_fwy_nodes.size()!=1)
            return;

        Node current_node = first_fwy_nodes.get(0);
        int c = 0;
        fwy_nodes = new ArrayList<FwyNode>();
        while(true){

            FwyNode fwynode = new FwyNode();
            fwy_nodes.add(fwynode);

            fwynode.node = current_node;

            for(int i=0;i<current_node.output_link.length;i++){
                Link link = current_node.output_link[i];
                if(link.link_type==Link.Type.freeway)
                    fwynode.dn_ml = link;
                if(link.link_type==Link.Type.offramp){
                    fwynode.offramp = link;
                    fwynode.fr_index = i;
                }
            }

            for(int i=0;i<current_node.input_link.length;i++) {
                Link link = current_node.input_link[i];
                if (link.link_type == Link.Type.onramp)
                    fwynode.onramp = link;
                if (link.link_type == Link.Type.freeway){
                    fwynode.up_ml = link;
                    fwynode.up_ml_index = i;
                }
            }

            if(fwynode.dn_ml==null){
                System.out.println("Reached end of freeway at node " + current_node.getId());
                break;
            }

            if(c++ > 1000){
                System.out.println("Exceeded maximum number of freeway segments. Possibly a loop exists.");
                break;
            }

            current_node = fwynode.dn_ml.getEnd_node();
        }

    }

    @Override
    public void update() throws BeatsException {

        // CHECK NUMENSEMBLE == 1
        // CHECK NUMVEHICLETYPES = 1
        int e=0;
        int vt = 0;

        update_profiles();

        update_sensors_control_events();

        // compute demand and total supply for all links
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
            for(edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink()) {
                Link bLink = (Link)link;
                bLink.updateOutflowDemand();
                bLink.link_behavior.update_total_space_supply();
            }

        // mainline allocation for onramps only: xi*(njam-n)
        for(FwyNode fwy_node : fwy_nodes )
            if(fwy_node.onramp!=null && fwy_node.dn_ml!=null) {
                fwy_node.supply_for_onramp = xi * fwy_node.dn_ml.link_behavior.total_space_supply[e];
            }

        // onramp flow = min(onramp demand,mainline supply for onramp)
        for (FwyNode fwy_node : fwy_nodes){
            if(fwy_node.onramp!=null){
                Double[] demand = fwy_node.onramp.get_out_demand_in_veh(e);
                double total_demand = BeatsMath.sum(demand);
                Double ratio = Math.min(1d,fwy_node.supply_for_onramp/total_demand);
                fwy_node.r = Math.min(total_demand,fwy_node.supply_for_onramp);
                fwy_node.onramp.setOutflow(e,BeatsMath.times(demand,ratio) );
            }
        }

        // available supply for mainline = w*(totalsupply - gamma*r )
        for(FwyNode fwy_node : fwy_nodes){
            Link link = fwy_node.dn_ml;
            if(link==null)
                continue;
            FundamentalDiagram FD = link.currentFD(e);
            double r = fwy_node.onramp==null ? 0d : fwy_node.r;
            double w = FD.getWNormalized();
            link.link_behavior.available_space_supply[e] = Math.max( w*(link.get_total_space_supply_in_veh(e) - gamma*r) , 0d );
        }

        // mainline flow
        for(FwyNode fwy_node : fwy_nodes){

            Node node = fwy_node.node;

            // update split ratio matrix
            double beta = 0d;
            if(fwy_node.fr_index>=0){
                Double [][][] splitratio_selected = node.getSplitRatio();
                beta = splitratio_selected[fwy_node.up_ml_index][fwy_node.fr_index][vt];
                beta = Double.isNaN(beta) ? 0d : beta;
            }

            Double [] fout = {0d}; // fake array for vehicle type

            fout[0] = Math.min( fwy_node.up_ml==null ? 0d :
                                                       fwy_node.up_ml.get_total_out_demand_in_veh(e) ,
                                fwy_node.dn_ml==null ? Double.POSITIVE_INFINITY :
                                                       fwy_node.dn_ml.link_behavior.available_space_supply[e]/(1-beta) );

            // assign flow to input links ..................................
            if(fwy_node.up_ml!=null)
                fwy_node.up_ml.setOutflow(e,fout);

            if(fwy_node.offramp!=null){
                Double [] s = {beta*fout[0]};
                fwy_node.offramp.setInflow(e, s);
            }

            if(fwy_node.dn_ml!=null){
                Double [] fin = {(1-beta)*fout[0]+fwy_node.r};
                fwy_node.dn_ml.setInflow(e,fin);
            }
        }

        // update density
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
            update_density((Network) network);

        update_cumalitives_clock();
    }

    @Override
    protected LinkBehaviorCTM create_link_behavior(Link link) {
        return new LinkBehaviorACTM(link);
    }

    @Override
    protected Node_FlowSolver create_node_flow_solver(Node node) {
        return null; //new Node_FlowSolver_ACTM(node);
    }

    public class FwyNode implements Serializable {
        private static final long serialVersionUID = -8920100129381839624L;
        Node node = null;
        Link dn_ml = null;
        Link up_ml = null;
        Link onramp = null;
        Link offramp = null;
        double r = 0d;
        double supply_for_onramp = Double.POSITIVE_INFINITY;
        int up_ml_index = -1;
        int fr_index = -1;

        @Override
        public String toString() {
            return String.format("%b\t%b\t%b\t%b",up_ml==null,onramp==null,dn_ml==null,offramp==null);
        }
    }

}
