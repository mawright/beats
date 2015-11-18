package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.control.Controller_VehType_Swapper;
import edu.berkeley.path.beats.simulator.*;
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
    private static Controller_VehType_Swapper controller;

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
    public void testConstruction() {
        controller = (Controller_VehType_Swapper) static_scenario.get.controllerWithId(0);

        Assert.assertEquals(controller.getMyType(), Controller.Algorithm.Vehicle_Type_Swapper );
        Assert.assertEquals( controller.getMyActuator().get_type(), Actuator.Type.vehtype_changer );
    }

    @Test
    @Ignore
    public void testRun() {

        try {
            static_scenario.advanceNSeconds(10);
        } catch (BeatsException e) {
            fail(e.toString());
        }

        Double[] X = controller.getMyLink().getDensityInVeh(0);
    }

}
