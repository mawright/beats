package edu.berkeley.path.beats.simulator.scenarioUpdate;

import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

/**
 * Created by gomes on 10/26/14.
 */
public class ScenarioUpdaterFrFlow extends ScenarioUpdaterAbstract {

    private static final long serialVersionUID = 6573601804020311187L;

    public ScenarioUpdaterFrFlow(Scenario scenario,String nodeflowsolver_name,String nodesrsolver_name){
        super(scenario,nodeflowsolver_name,nodesrsolver_name);
    }

    @Override
    public void update() throws BeatsException {

        update_profiles();

        // update supply/demand prior to controllers if run_mode==fw_fr_split_output
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
            update_supply_demand((Network) network);

        update_sensors_control_events();

        // update the network state......................
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork()){
            Network net = (Network) network;
            update_flow(net);
            update_density(net);
        }

        update_cumalitives_clock();
    }

}
