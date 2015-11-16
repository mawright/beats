package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * Created by gomes on 6/4/2015.
 */
public class VehTypeActuatorTest {

    @Test
    public void test() {
        try {
            Scenario scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_actcomm.xml");
            if (scenario == null)
                fail("scenario did not load");
            String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";

            scenario.initialize(5d, 0d, 3600d, 5d, "text", outprefix, 1, 1, null, null, null, null, "normal", null, null, null);
            scenario.run();


        } catch (BeatsException e) {
            e.printStackTrace();
        }
    }

}
