package edu.berkeley.path.beats.test.simulator;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.Defaults;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Node_SplitRatioSolver;
import edu.berkeley.path.beats.simulator.Node_SplitRatioSolver_A;
import edu.berkeley.path.beats.simulator.Node_SplitRatioSolver_HAMBURGER;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.BeatsErrorLog.BeatsError;

public class NodeTest {

	private static Node node;
	private static String config_folder = "data/config/";
	private static String config_file = "_smalltest.xml";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Scenario scenario = ObjectFactory.createAndLoadScenario(config_folder+config_file);
		if(scenario==null)
			fail("scenario did not load");
		
		double timestep = Defaults.getTimestepFor(config_file);
		double starttime = 0;
		double endtime = 300;
		int numEnsemble = 1;
		scenario.initialize(timestep,starttime,endtime,numEnsemble);
		node = scenario.getNodeWithId(-4);
	}

	@Test
	public void test_getMyNetwork() {
		assertEquals(node.getMyNetwork().getId(),-1);
	}

	@Test
	public void test_getOutput_link() {
		Link[] links = node.getOutput_link();
		assertEquals(links[0].getId(),-4);
		assertEquals(links[1].getId(),-7);
	}

	@Test
	public void test_getInput_link() {
		Link[] links = node.getInput_link();
		System.out.println(links[0].getId());
		assertEquals(links[0].getId(),-3);
	}

	@Test
	public void test_getInputLinkIndex() {
		assertEquals(node.getInputLinkIndex(-3),0);
		assertEquals(node.getInputLinkIndex(100000),-1);

	}

	@Test
	public void test_getOutputLinkIndex() {
		assertEquals(node.getOutputLinkIndex(-4),0);		
		assertEquals(node.getOutputLinkIndex(-7),1);
		assertEquals(node.getOutputLinkIndex(100000),-1);

		// edge case
//		assertEquals(node.getOutputLinkIndex(null),-1);
	}

	@Test
	public void test_getnIn() {
		assertEquals(node.getnIn(),1);
	}

	@Test
	public void test_getnOut() {
		assertEquals(node.getnOut(),2);
	}

//	@Test
//	public void test_hasController() {
//		assertFalse(node.hasController());
//	}

	@Test
	public void test_getSplitRatio_a() {
		double [][][] X = node.getSplitRatio();
		assertEquals(X[0][0][0],1d,1e-4);
		assertEquals(X[0][1][0],0d,1e-4);
	}

	@Test
	public void test_getSplitRatio_b() {
		assertEquals(node.getSplitRatio(0, 0, 0),1d,1e-4);
		assertEquals(node.getSplitRatio(0, 1, 0),0d,1e-4);

		// edge cases
		assertTrue(Double.isNaN(node.getSplitRatio(-1, 1, 0)));
		assertTrue(Double.isNaN(node.getSplitRatio(100, 1, 0)));
		assertTrue(Double.isNaN(node.getSplitRatio(0, -1, 0)));
		assertTrue(Double.isNaN(node.getSplitRatio(0, 100, 0)));
		assertTrue(Double.isNaN(node.getSplitRatio(0, 0, -1)));
		assertTrue(Double.isNaN(node.getSplitRatio(0, 0, 100)));
	}
	
	@Test
	public void test_attatch_Node_SpitRatioSolver_HAMBURGER_node() throws Exception
	{
		config_file = "_smalltest_Hamburger_SplitRatioSolver.xml";
		Scenario scenario = ObjectFactory.createAndLoadScenario(config_folder+config_file);
		if(scenario==null)
			fail("scenario did not load");
		
		double timestep = Defaults.getTimestepFor(config_file);
		double starttime = 0;
		double endtime = 300;
		int numEnsemble = 1;
		String uncertaintymodel = "gaussian";
		String nodeflowsolver = "proportional";
		String nodesrsolver = "HAMBURGER";

		scenario.initialize(timestep, starttime, endtime, numEnsemble, uncertaintymodel,nodeflowsolver,nodesrsolver);
		
		Node node = scenario.getNodeWithId(2);
		
		Field node_sr_solver_field = Node.class.getDeclaredField("node_sr_solver");
		node_sr_solver_field.setAccessible(true);
		Object node_sr_solver = node_sr_solver_field.get(node);
		assertEquals("Test of of node_sr_solver", node_sr_solver.getClass(), Node_SplitRatioSolver_HAMBURGER.class);
	}
	
	@Test
	public void test_attatch_Node_SpitRatioSolver_HAMBURGER_normal_node() throws Exception
	{
		config_file = "_smalltest_Hamburger_SplitRatioSolver.xml";
		Scenario scenario = ObjectFactory.createAndLoadScenario(config_folder+config_file);
		if(scenario==null)
			fail("scenario did not load");
		
		double timestep = Defaults.getTimestepFor(config_file);
		double starttime = 0;
		double endtime = 300;
		int numEnsemble = 1;
		String uncertaintymodel = "gaussian";
		String nodeflowsolver = "proportional";
		String nodesrsolver = "HAMBURGER";

		scenario.initialize(timestep, starttime, endtime, numEnsemble, uncertaintymodel,nodeflowsolver,nodesrsolver);
		
		Node node = scenario.getNodeWithId(5);
		
		Field node_sr_solver_field = Node.class.getDeclaredField("node_sr_solver");
		node_sr_solver_field.setAccessible(true);
		Object node_sr_solver = node_sr_solver_field.get(node);
		assertEquals("Test of of node_sr_solver", node_sr_solver.getClass(), Node_SplitRatioSolver_A.class);
	}
	
	@Test
	public void test_splitPerturber2Out() {
		double[][][] split = node.getSplitRatio();
		split[0][0][0] = .8d;
		split[0][1][0] = .2d;
		double[][][] splitPerturbed = Node.perturb2DSplitForTest(split);
		assertTrue(split[0][0][0]!=splitPerturbed[0][0][0]);
		assertTrue(split[0][1][0]!=splitPerturbed[0][1][0]);
	}
}
