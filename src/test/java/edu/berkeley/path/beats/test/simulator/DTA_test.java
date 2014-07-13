package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.control.ReroutePolicySet;
import edu.berkeley.path.beats.control.adjoint_glue.AdjointReroutesPolicyMaker;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.fail;

public class DTA_test {

    private static String configfilename = "data/config/Rerouting_sent_newxsd_v3.xml";
    private static Scenario scenario;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            scenario = ObjectFactory.createAndLoadScenario(configfilename);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_getDensityForLinkIdInVeh() {

        Properties properties = new Properties();
        properties.setProperty("MAX_ITERATIONS","3");
        properties.setProperty("DEBUG","ON");
        properties.setProperty("DESCENT_METHOD","RProp");
        properties.setProperty("OPTIMIZER_TYPE","FINITE_DIFFERENCE");
        properties.setProperty("STOPPING_CRITERIA","10E-9");

        ReroutePolicySet result = AdjointReroutesPolicyMaker.computePolicy(
                scenario.getNetworkSet().getNetwork().get(0),
                scenario.getFundamentalDiagramSet(),
                scenario.getDemandSet(),
                scenario.getSplitRatioSet(),
                scenario.getInitialDensitySet(),
                scenario.getRouteSet(),
                5d,
                720d,
                properties);

        org.junit.Assert.assertNotNull(result);
    }


}
