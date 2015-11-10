package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.Defaults;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.nodeBeahavior.Node_FlowSolver_General;
import edu.berkeley.path.beats.simulator.nodeBeahavior.RestrictionCoefficients;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by matt on 7/14/15.
 */
public class GeneralNodeModelTest {

	private static Scenario static_scenario;
	private static String config_folder = "data/config/";
	private static String output_folder = "data/test/output/";
	private static String fixture_folder = "data/test/fixture/";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		try {
//			String config_file = "_smalltest.xml";
//			static_scenario = Jaxb.create_scenario_from_xml(config_folder + config_file);
//			if(static_scenario==null)
//				fail("scenario did not load");
//
//			// initialize
//			double timestep = Defaults.getTimestepFor(config_file);
//			double starttime = 300d;
//			double endtime = Double.POSITIVE_INFINITY;
//			int numEnsemble = 10;
//			static_scenario.initialize(timestep,starttime,endtime,numEnsemble,"gaussian","general","A");
//			static_scenario.reset();
//
//		} catch (BeatsException e) {
//			fail("initialization failure.");
//		}
	}

	@Test
	public void twoInTwoOutTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml(config_folder + "_onejunctiontest.xml");
			// initialize
			double timestep = 1;
			double starttime = 0;
			double endtime = 1;
			int numEnsemble = 1;
			scenario.initialize(timestep,starttime,endtime,numEnsemble,"gaussian","general","A");
			scenario.reset();

			double [][] initialDensity = new double[4][1];
			initialDensity[0][0] = 1000;
			initialDensity[1][0] = 1000;
			initialDensity[2][0] = 0;
			initialDensity[3][0] = 0;
			scenario.set.totalDensity(initialDensity);

			scenario.advanceNSeconds(1);
			double [][] X = scenario.get.totalDensity(scenario.getNetworks().get(0).getId());
			double [][] expected = new double[4][1];
			expected[0][0] = 1000 - 600 - 66.67;
			expected[1][0] = 1000 - 533.33;
			expected[2][0] = 600;
			expected[3][0] = 66.67 + 533.33;
			assertEquals(expected[0][0], X[0][0], .01);
			assertEquals(expected[1][0], X[1][0], .01);
			assertEquals(expected[2][0], X[2][0], .01);
			assertEquals(expected[3][0], X[3][0], .01);
		}
		catch (BeatsException e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void tampereStrictFIFOTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml(config_folder + "_4x4tamperetest.xml");
			// initialize
			double timestep = 1;
			double starttime = 0;
			double endtime = 1;
			int numEnsemble = 1;
			scenario.initialize(timestep,starttime,endtime,numEnsemble,"gaussian","general","A");
			scenario.reset();

			Node junctionNode = scenario.get.nodeWithId(-9);
			Node_FlowSolver_General solver = (Node_FlowSolver_General) junctionNode.node_behavior.flow_solver;
			RestrictionCoefficients restrictionCoefficients = solver.getRestrictionCoefficients();
			for(Link inLink : junctionNode.getInput_link()) {
				for(Link restrictorLink : junctionNode.getOutput_link()) {
					for(Link restrictedLink : junctionNode.getOutput_link()) {
						restrictionCoefficients.setCoefficient(inLink,restrictorLink,restrictedLink,1);
					}
				}
			}

			double [][] initialDensity = new double[8][1];
			initialDensity[0][0] = 500;
			initialDensity[1][0] = 2000;
			initialDensity[2][0] = 800;
			initialDensity[3][0] = 1700;
			initialDensity[4][0] = 0;
			initialDensity[5][0] = 0;
			initialDensity[6][0] = 0;
			initialDensity[7][0] = 0;
			scenario.set.totalDensity(initialDensity);

			scenario.advanceNSeconds(1);
			double [][] X = scenario.get.totalDensity(scenario.getNetworks().get(0).getId());
			double [][] expected = new double[8][1];
			expected[0][0] = 0;
			expected[1][0] = 2000 - 68.5 - 205.5 - 1096;
			expected[2][0] = 0;
			expected[3][0] = 1700 - 80.6 - 644.5 - 644.5;
			expected[4][0] = 68.5 + 80.6 + 100;
			expected[5][0] = 50 + 644.5 + 100;
			expected[6][0] = 150 + 205.5 + 644.5;
			expected[7][0] = 300 + 1096 + 600;
			assertEquals(expected[0][0], X[0][0], 0.5);
			assertEquals(expected[1][0], X[1][0], 0.5);
			assertEquals(expected[2][0], X[2][0], 0.5);
			assertEquals(expected[3][0], X[3][0], 0.5);
			assertEquals(expected[4][0], X[4][0], 0.5);
			assertEquals(expected[5][0], X[5][0], 0.5);
			assertEquals(expected[6][0], X[6][0], 0.5);
			assertEquals(expected[7][0], X[7][0], 0.5);

		}
		catch (BeatsException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void tampereRelaxedFIFOTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml(config_folder + "_4x4tamperetest.xml");
			// initialize
			double timestep = 1;
			double starttime = 0;
			double endtime = 1;
			int numEnsemble = 1;
			scenario.initialize(timestep,starttime,endtime,numEnsemble,"gaussian","general","A");
			scenario.reset();

			double [][] initialDensity = new double[8][1];
			initialDensity[0][0] = 500;
			initialDensity[1][0] = 2000;
			initialDensity[2][0] = 800;
			initialDensity[3][0] = 1700;
			initialDensity[4][0] = 0;
			initialDensity[5][0] = 0;
			initialDensity[6][0] = 0;
			initialDensity[7][0] = 0;
			scenario.set.totalDensity(initialDensity);

			scenario.advanceNSeconds(1);
			double [][] X = scenario.get.totalDensity(scenario.getNetworks().get(0).getId());
			double [][] expected = new double[8][1];
			expected[0][0] = 0;
			expected[1][0] = 2000 - 205.5 - 1211.75 - 89.916;
			expected[2][0] = 800 - 488.25 - 81.375 - 81.375;
			expected[3][0] = 1700 - 644.5 - 100 - 722.25;
			expected[4][0] = 89.916 + 81.375 + 100;
			expected[5][0] = 50 + 81.375 + 722.25;
			expected[6][0] = 150 + 644.5 + 205.5;
			expected[7][0] = 300 + 1211.75 + 488.25;
			assertEquals(expected[0][0], X[0][0], 0.5);
			assertEquals(expected[1][0], X[1][0], 0.5);
			assertEquals(expected[2][0], X[2][0], 0.5);
			assertEquals(expected[3][0], X[3][0], 0.5);
			assertEquals(expected[4][0], X[4][0], 0.5);
			assertEquals(expected[5][0], X[5][0], 0.5);
			assertEquals(expected[6][0], X[6][0], 0.5);
			assertEquals(expected[7][0], X[7][0], 0.5);

		}
		catch (BeatsException e) {
			fail(e.getMessage());
		}
	}

}
