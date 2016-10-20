package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.InitialDensitySet;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Created by gomes on 5/25/2016.
 */
@Ignore
public class ControllerAPI {

    static Scenario scenario;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            String config_file = "C:\\Users\\gomes\\Desktop\\x.xml"; // Please fix this
            scenario = Jaxb.create_scenario_from_xml(config_file);
            if(scenario==null)
                fail("scenario did not load");

            // initialize
            double timestep = 4;
            double starttime = 0d;
            double endtime = Double.POSITIVE_INFINITY;
            int numEnsemble = 1;
            scenario.initialize(timestep,starttime,endtime,numEnsemble);
            scenario.reset();

        } catch (BeatsException e) {
            fail("initialization failure.");
        }
    }


    @Test
    public void xxx() {

        try {

            set_rate(100d);
            scenario.advanceNSeconds(100d);

            double [][] link_commodity_densities = scenario.get.densityForNetwork(1L,0);
//            double [] link_densities = link_commodity_densities[][0];
            // you get the message

//            InitialDensitySet current_densities_si()




            set_rate(200d);
            scenario.advanceNSeconds(100d);

            set_rate(300d);
            scenario.advanceNSeconds(100d);

            set_rate(400d);
            scenario.advanceNSeconds(100d);

            scenario.reset();

            set_rate(200d);
            scenario.advanceNSeconds(100d);


        } catch (BeatsException e) {
            e.printStackTrace();
        }

    }

    private void set_rate(double rate_in_vph){
        int controller_id = 0;
        for(int act_id=1;act_id<=29;act_id++)
            scenario.set.ramp_metering_rate(controller_id,act_id,rate_in_vph);
    }
}
