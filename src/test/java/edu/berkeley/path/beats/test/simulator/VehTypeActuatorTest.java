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
			double timestep = 10d;
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
    public void testRun() {

        try {
            static_scenario.advanceNSeconds(100);
        } catch (BeatsException e) {
            fail(e.toString());
        }

        Double[] X = controller.getMyLink().getInflowInVeh(0);
        Double[] Y = controller.getMyLink().getDensityInVeh(0);

        // for the first 109 seconds the switch ratio is 0.5
        Assert.assertEquals(X[0], X[1]);
        Assert.assertEquals(Y[0], Y[1]);

        try {
            static_scenario.advanceNSeconds(100);
        } catch (BeatsException e) {
            fail(e.toString());
        }

        X = controller.getMyLink().getInflowInVeh(0);
        Double Z = X[0] + X[1];

        // after that the switch ratio from 0 to 1 is 0.2
        Assert.assertEquals(X[0], Z*0.8, 1e-4);
        Assert.assertEquals(X[1], Z*0.2, 1e-4);
    }

    @Test
    public void testNormalization() {
        try {
            static_scenario.advanceNSeconds(100);
        } catch (BeatsException e) {
            fail(e.toString());
        }

        Double[] X = controller.getMyLink().getInflowInVeh(0);

        // the switch ratio from 0 to 1 is 0.5, from 0 to 2 is 0.75, it should normalize to 0.4 and 0.6
        Assert.assertEquals(X[1] * .6, X[2] * .4, 1e-4);
    }

}
