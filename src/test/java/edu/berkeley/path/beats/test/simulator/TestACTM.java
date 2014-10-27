package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.Scenario;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.fail;

/**
 * Created by gomes on 10/24/14.
 */
public class TestACTM {

    private String config = "C:\\Users\\gomes\\Desktop\\test_actm\\210W_pm_cropped_L0_actm.xml";

    @Test
    public void test_actm() {
        try {
            Scenario scenario = Jaxb.create_scenario_from_xml(config);

            double timestep = 5d;
            double starttime = 0d;
            double endtime = 3600d;
            double outdt = 300d;
            String outtype = "text";
            String outprefix = "C:\\Users\\gomes\\code\\L0\\L0-mpc-demo\\data\\out";
            String uncertaintymodel = "gaussian";
            String nodeflowsolver = "proportional";
            String nodesrsolver = "A";
            String run_mode = "normal";

            boolean is_actm = true;

            scenario.initialize(timestep,starttime,endtime,outdt,outtype,outprefix,1,1,
                                uncertaintymodel,is_actm, nodeflowsolver, nodesrsolver,"",run_mode,
                                "",Double.NaN ,null);

            scenario.run();
        } catch (BeatsException e) {
            fail("initialization failure.");
        }
    }
}