package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;

import java.io.Serializable;

/**
 * Created by gomes on 10/28/14.
 */
public class Node_SupplyPartitioner implements Serializable {

    private static final long serialVersionUID = -308007296457311285L;

    Node node;

    public Node_SupplyPartitioner(Node node){
        this.node = node;
    }

//    public double compute_total_available_suppy_in_link(double total_supply,Link link,int e){
//
//        Scenario scenario = node.getMyNetwork().getMyScenario();
//        double available_supply;
//        FundamentalDiagram FD = link.currentFD(e);
//        available_supply = Math.min( FD.getWNormalized()*total_supply , FD._getCapacityInVeh() );
//
//        // flow uncertainty model
//        if(scenario.isHas_flow_unceratinty()){
//            double delta_flow=0.0;
//            double std_dev_flow = scenario.getStd_dev_flow();
//            switch(scenario.getUncertaintyModel()){
//                case uniform:
//                    delta_flow = BeatsMath.sampleZeroMeanUniform(std_dev_flow);
//                    break;
//
//                case gaussian:
//                    delta_flow = BeatsMath.sampleZeroMeanGaussian(std_dev_flow);
//                    break;
//            }
//            available_supply = Math.max( 0d , available_supply + delta_flow );
//            available_supply = Math.min( available_supply , FD._getDensityJamInVeh() - link.getTotalDensityInVeh(e));
//        }
//
//        return available_supply;
//    }
}
