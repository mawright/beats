package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.jaxb.Splitratio;
import edu.berkeley.path.beats.simulator.JaxbObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.SplitRatioProfile;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by gomes on 10/10/2015.
 */
public class SetSplitRatioTest {

    private static Scenario scenario;
    private static String config_folder = "data/config/";
    private static String config_file = "_smalltest.xml";
    private long node_id = -4;

    @BeforeClass
    public static void setUp() throws Exception {
        scenario = Jaxb.create_scenario_from_xml(config_folder + config_file);
        if (scenario == null)
            fail("scenario did not load");
        scenario.initialize(5, 0, 3600, 1);

    }

    @Test
    public void test_set_splits_for_node() {
        Splitratio[] splitratios = new Splitratio[2];
        JaxbObjectFactory jaxb = new JaxbObjectFactory();

        splitratios[0] = jaxb.createSplitratio();
        splitratios[0].setLinkIn(-3);
        splitratios[0].setLinkOut(-4);
        splitratios[0].setContent("0.1,0.2,0.3,0.4");

        splitratios[1] = jaxb.createSplitratio();
        splitratios[1].setLinkIn(-3);
        splitratios[1].setLinkOut(-7);
        splitratios[1].setContent("0.9,0.8,0.7,0.6");

        scenario.set.splits_for_node(-4, 0, 300, splitratios);

        SplitRatioProfile srp = scenario.get.splitprofile_for_node(-4);
        assertEquals(srp.getCurrentSplitRatio(0,0,0),0.1);
        assertEquals(srp.getCurrentSplitRatio(0,1,0),0.9d);
    }

}