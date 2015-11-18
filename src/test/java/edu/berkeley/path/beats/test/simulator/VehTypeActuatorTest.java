package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.control.Controller_VehType_Swapper;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Defaults;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * Created by gomes on 6/4/2015.
 */
public class VehTypeActuatorTest {

    static Scenario static_scenario;
    private static String config_folder = "data/config/";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
		try {
			String config_file = "_smalltest_actvehtype.xml";
			static_scenario = Jaxb.create_scenario_from_xml(config_folder + config_file);
			if(static_scenario==null)
				fail("scenario did not load");

			// initialize
			double timestep = 1d;
			double starttime = 0d;
			double endtime = Double.POSITIVE_INFINITY;
			int numEnsemble = 1;
			static_scenario.initialize(timestep,starttime,endtime,numEnsemble,"gaussian","general","A");
			static_scenario.reset();

		} catch (BeatsException e) {
			fail("initialization failure.");
		}
    }

    @Test
    @Ignore
    public void testConstruction() {
        Controller_VehType_Swapper controller = (Controller_VehType_Swapper) static_scenario.get.controllerWithId(0);
        Assert.assertEquals(controller.getMyType().toString(), "Vehicle_Type_Swapper");
    }

    @Test
    @Ignore
    public void testRun() {
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
