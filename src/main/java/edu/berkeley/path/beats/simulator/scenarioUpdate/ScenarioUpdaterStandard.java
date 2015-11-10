package edu.berkeley.path.beats.simulator.scenarioUpdate;

import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;

/**
 * Created by gomes on 10/26/14.
 */
public class ScenarioUpdaterStandard extends ScenarioUpdaterAbstract implements Serializable {

    private static final long serialVersionUID = 7437221182816459609L;

    public ScenarioUpdaterStandard(Scenario scenario,String nodeflowsolver_name,String nodesrsolver_name){
        super(scenario,nodeflowsolver_name,nodesrsolver_name);
    }

    @Override
    public void update() throws BeatsException {

        update_profiles();

        update_sensors_control_events();

        // update the network state......................
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork()){
            Network net = (Network) network;
            update_supply_demand(net);
            update_flow(net);
            update_density(net);
        }

        update_cumalitives_clock();
    }

}
