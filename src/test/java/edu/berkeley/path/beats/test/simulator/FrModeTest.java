package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by gomes on 5/11/2015.
 */
public class FrModeTest {

    @Test
    public void testFrMode() {
        try {

            int linkid = 7;

            String config = "C:\\Users\\Felix\\code\\autoCalibrationProject\\config\\210E_joined.xml";
            Scenario scenario = Jaxb.create_scenario_from_xml(config);
//            Scenario scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_SRcontrol2.xml");
            if (scenario == null)
                fail("scenario did not load");
            String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";

            System.out.println(outprefix);

            // run fr mode with knob = 2
            scenario.initialize(4d, 0d, 86400d, 300d, "text", outprefix, 1, 1, null, null, null, null, "fw_fr_split_output", null, null, null);
//            scenario.initialize(4d, 0d, 86400d, 300d, "text", outprefix, 1, 1, null, null, null, null, "normal", null, null, null);
            scenario.set.demand_knob_for_link_id(linkid,2d);
            scenario.run();

            // check
            Link link = scenario.get.linkWithId(linkid);
            Double d = BeatsMath.sum(link.getOutflowInVeh(0)) * 3600d / 5d;
            assertTrue(BeatsMath.equals(d, 100d));

            // run fr mode with knob = 3
            scenario.reset();
            scenario.set.demand_knob_for_link_id(linkid,3d);
            scenario.run();

            // check
            link = scenario.get.linkWithId(7);
            d = BeatsMath.sum(link.getOutflowInVeh(0)) * 3600d / 5d;
            assertTrue(BeatsMath.equals(d, 150d));

        } catch (BeatsException e) {
            e.printStackTrace();
        }
    }
}
