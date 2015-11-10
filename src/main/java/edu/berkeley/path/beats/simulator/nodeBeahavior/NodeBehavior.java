package edu.berkeley.path.beats.simulator.nodeBeahavior;

import edu.berkeley.path.beats.simulator.Node;

import java.io.Serializable;

/**
 * Created by gomes on 10/28/14.
 */
public class NodeBehavior implements Serializable {

    private static final long serialVersionUID = -6865485388389533039L;

    public Node node;
    public Node_SplitRatioSolver sr_solver;
    public Node_FlowSolver flow_solver;
    public Node_SupplyPartitioner supply_partitioner;

    public NodeBehavior(Node node,Node_SplitRatioSolver sr_solver,Node_FlowSolver flow_solver,Node_SupplyPartitioner supply_partitioner){
        this.node = node;
        this.supply_partitioner = supply_partitioner;
        this.sr_solver = sr_solver;
        this.flow_solver = flow_solver;
    }

    protected double [][] getDemand(int e){
        int nIn = node.getnIn();
        int nVT = node.getMyNetwork().getMyScenario().get.numVehicleTypes();
        double [][] x = new double[nIn][nVT];
        for(int i=0;i<nIn;i++)
            for(int k=0;k<nVT;k++)
                x[i][k] = node.input_link[i].get_out_demand_in_veh(e)[k];
        return x;
    }

    protected double [] getAvailableSupply(int e){
        double [] x = new double[node.getnOut()];
        for(int j=0;j<node.getnOut();j++)
            x[j] = node.output_link[j].get_available_space_supply_in_veh(e);
        return x;
    }

}
