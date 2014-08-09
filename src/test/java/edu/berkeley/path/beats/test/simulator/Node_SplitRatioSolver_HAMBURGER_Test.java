package edu.berkeley.path.beats.test.simulator;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import edu.berkeley.path.beats.jaxb.LinkType;
import edu.berkeley.path.beats.jaxb.NodeType;
import edu.berkeley.path.beats.jaxb.VehicleType;
import edu.berkeley.path.beats.jaxb.VehicleTypeSet;
import edu.berkeley.path.beats.simulator.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.Defaults;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.LinkBehaviorCTM;
import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Node_SplitRatioSolver_A;
import edu.berkeley.path.beats.simulator.Node_SplitRatioSolver_HAMBURGER;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Parameters;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.BeatsErrorLog.BeatsError;

public class Node_SplitRatioSolver_HAMBURGER_Test {

	// Evaluation fields.
	private static ArrayList<BeatsError> log;
	private Method validateCondition;
	private Method getActualValue;
	private static Field description;
	private static Field errorLog;
	
	// Test environment fields.
	private int nr_of_ensembles;	
	private Node node = null;
	private Node_SplitRatioSolver_HAMBURGER split_ratio_solver;
	
	/* Initiation */
	@BeforeClass 
	public static void reflectErrorLog() throws Exception 
	{
		// Gives access to the error log.
		errorLog = BeatsErrorLog.class.getDeclaredField("error");
		errorLog.setAccessible(true);
		
		// Storing a reference to the error log.
		log = (ArrayList<BeatsError>) errorLog.get(BeatsErrorLog.class);
		
		// Gives access to the error message.
		description = BeatsError.class.getDeclaredField("description");
		description.setAccessible(true);
	}
	
	/* Preparing for test */
	@Before
	public void resetTest() throws Exception 
	{
		// Clearing Error log.
		validateCondition = null;
		BeatsErrorLog.clearErrorMessage();
		
		// Reset environmental fields.
		nr_of_ensembles = 1;
		node = null;
		split_ratio_solver = null;	
	}

	/* Test of the validation method */							 
	// Test: validation number of input links (2-to-2).
	@Test
	public void test_validation_of_nr_of_inputLinks_2to2() throws Exception 
	{
		String test_configuration = "Test: validation number of input links (2-to-2).";
		
		// Build test environment.
		generateValidationEnvironment(test_configuration);
		
		// Evaluate output.
		assertTrue(test_configuration, description.get(log.get(0)).equals("Incorrect number of incomming links at node ID = 0 , total number of incomming links are 2 it must be 1."));
	}
	
	// Test: validation number of output links (1-to-1).
	@Test
	public void test_validation_of_nr_of_outputLinks_1to1() throws Exception 
	{
		String test_configuration = "Test: validation number of output links (1-to-1).";
		
		// Build test environment.
		generateValidationEnvironment(test_configuration);
		
		// Evaluate output.
		assertTrue(test_configuration, description.get(log.get(0)).equals("Incorrect number of outgoing links at node ID = 0 , total number of outgoing links are 1 it must be 2."));
	}
	
	// Test: validation number of output links (1-to-3).
	@Test
	public void test_validation_of_nr_of_outputLinks_1to3() throws Exception 
	{		
		String test_configuration = "Test: validation number of output links (1-to-3).";
			
		// Build test environment.
		generateValidationEnvironment(test_configuration);
		
		// Evaluate output.
		assertTrue(test_configuration, description.get(log.get(0)).equals("Incorrect number of outgoing links at node ID = 0 , total number of outgoing links are 3 it must be 2."));
	}
	
	// Test: validation of link type on the downstream link.
	@Test
	public void test_validation_of_downstream_link_type() throws Exception 
	{
		String test_configuration = "Test: validation of link type on the downstream link.";
		
		// Build test environment.
		generateValidationEnvironment(test_configuration);
		
		// Evaluate output.
		assertTrue(test_configuration, description.get(log.get(0)).equals("Missing downstream link of type Freeway at node ID = 0 ,  it must be exactly one link downstream of type Freeway."));
	}
	
	// Test: validation of link type on the diverging link.
	@Test
	public void test_validation_of_diverging_link_type() throws Exception 
	{
		String test_configuration = "Test: validation of link type on the diverging link.";
		
		// Build test environment
		generateValidationEnvironment(test_configuration);
		
		// Evaluate output.
		assertTrue(test_configuration, description.get(log.get(0)).equals("Missing diverging link of type Off-Ramp/Interconnect at node ID = 0 ,  it must be exactly one diverging link of type Off-Ramp or Interconnect."));
	}
	
	
	/* Test of calculation */	
	// Test: calculation where adjusted sr are negative.
	@Test
	public void test_calculation_sr_is_negative() throws Exception
	{
		String configuration = "Test: calculation where adjusted sr are negative.";
		
		// Generate Expected output .
		Object expected_output = constructDouble3DMatrix(buildExpectedOutput(configuration));;
		
		// Generate test environment and calculate actual output.
		Object actual_output = generateCalculationEnvironment(configuration);
	
		// Evaluate output.
		double[][][] actual_output_double = (double[][][]) getActualValue.invoke(actual_output,null);
		double[][][] expected_output_double = (double[][][]) getActualValue.invoke(expected_output,null);
		
		for(int i = 0; i < actual_output_double.length; i++) 
		{
			for(int o = 0 ; o < actual_output_double[i].length; o++) 
			{
				for(int vt = 0 ; vt < actual_output_double[i][o].length ; vt++)
				{
					assertEquals("test", expected_output_double[i][o][vt], actual_output_double[i][o][vt], 0);
				}
			}			
		}

		assertTrue(configuration, description.get(log.get(0)).equals("Split ratio at node ID = 0 has been adjusted to an illegal ratio (-0.7899999991852997) it has been ceiled to 0."));
	}

	// Test: calculation where adjusted sr are over 1.
	@Test
	public void test_calculation_sr_is_higher_then_one() throws Exception
	{
		String configuration = "Test: calculation where adjusted sr are over 1.";
		
		// Generate Expected output .
		Object expected_output = constructDouble3DMatrix(buildExpectedOutput(configuration));;
		
		// Generate test environment and calculate actual output.
		Object actual_output = generateCalculationEnvironment(configuration);
	
		// Evaluate output.
		double[][][] actual_output_double = (double[][][]) getActualValue.invoke(expected_output,null);
		double[][][] expected_output_double = (double[][][]) getActualValue.invoke(actual_output,null);
		
		for(int i = 0; i < actual_output_double.length; i++) 
		{
			for(int o = 0 ; o < actual_output_double[i].length; o++) 
			{
				for(int vt = 0 ; vt < actual_output_double[i][o].length ; vt++)
				{
					assertEquals("test", expected_output_double[i][o][vt], actual_output_double[i][o][vt], 0);
				}
			}		
		}
		
		assertTrue(configuration, description.get(log.get(0)).equals("Split ratio at node ID = 0 has been adjusted to an illegal ratio (1.8100000008147004) it has been floored to 1."));
	}
	
	// Test: calculation diversion.
	@Test
	public void test_calculation_diversion() throws Exception
	{
		String configuration = "Test: calculation diversion.";
		
		// Generate Expected output .
		Object expected_output = constructDouble3DMatrix(buildExpectedOutput(configuration));;
		
		// Generate test environment and calculate actual output.
		Object actual_output = generateCalculationEnvironment(configuration);
	
		// Evaluate output.
		double[][][] actual_output_double = (double[][][]) getActualValue.invoke(actual_output,null);
		double[][][] expected_output_double = (double[][][]) getActualValue.invoke(expected_output,null);
		
		for(int i = 0; i < actual_output_double.length; i++) 
		{
			for(int o = 0 ; o < actual_output_double[i].length; o++) 
			{
				for(int vt = 0 ; vt < actual_output_double[i][o].length ; vt++)
				{
					assertEquals("Test: calculation diversion: ", expected_output_double[i][o][vt], actual_output_double[i][o][vt], 1);
				}
			}			
		}
	}
	
	// Test: calculation no diversion.
	@Test
	public void test_calculation_no_diversion() throws Exception
	{
		String configuration = "Test: calculation no diversion.";
		
		// Generate Expected output.
		Object expected_output = constructDouble3DMatrix(buildExpectedOutput(configuration));;
		
		// Generate test environment and calculate actual output.
		Object actual_output = generateCalculationEnvironment(configuration);
	
		// Access expected data
		Method getExpectedValue = expected_output.getClass().getDeclaredMethod("getData", null);
		getExpectedValue.setAccessible(true);
		// Evaluate output.
		double[][][] actual_output_double = (double[][][]) getActualValue.invoke(actual_output,null);
		double[][][] expected_output_double = (double[][][]) getExpectedValue.invoke(expected_output,null);
		
		for(int i = 0; i < actual_output_double.length; i++) 
		{
			for(int o = 0 ; o < actual_output_double[i].length; o++) 
			{
				for(int vt = 0 ; vt < actual_output_double[i][o].length ; vt++)
				{
					assertEquals("Test: calculation no diversion: ", expected_output_double[i][o][vt], actual_output_double[i][o][vt], 0);
				}
			}			
		}
	}
	
	// Test: calculation with two ensembles.
	@Ignore
	public void test_calculation_two_Ensembles() throws Exception
	{
		fail("Not yet implemented...");
	}
	
	// Test: Load real scenario.	
	@Test
	public void test_load_real_scenario() throws Exception
	{
		// Load scenario
		String config_folder = "data/config/";
		String config_file = "_largetest_Hamburger_SplitRatioSolver(I210W).xml";
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

		// Initiate and advance the simulation 120 seconds.
		scenario.initialize(timestep, starttime, endtime, numEnsemble, uncertaintymodel,nodeflowsolver,nodesrsolver);
		scenario.advanceNSeconds(60*timestep);
		
		// Access split ratio solver field
		Field split_ratio_solver_field = Node.class.getDeclaredField("node_sr_solver");
		split_ratio_solver_field.setAccessible(true);
		
		// Assertion of the Hamburger node.
		if(scenario.getNetworkSet()==null)
		{
			fail("Failed to find the network");
		}
		for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
		{
			for(edu.berkeley.path.beats.jaxb.Node node_in_list : network.getNodeList().getNode())
			{
				Node node = (Node) node_in_list;
				if(!node.isTerminal())
				{
					boolean does_threshold_exist = false;
					boolean does_scaling_factor_exist = false;
				
					// Check if nodes without parameters has solver A.
					if(node.getNodeType().getParameters() == null)
					{
						assertEquals("Test: Load real scenario. - verify right SplitRatioSolver",Node_SplitRatioSolver_A.class,split_ratio_solver_field.get(node).getClass());
					}
					else
					{
						for (int p = 0 ; p < node.getNodeType().getParameters().getParameter().size() ; p++)
						{
							if(node.getNodeType().getParameters().getParameter().get(p).getName().equals("threshold"))
							{
								does_threshold_exist = true;
							}
							if(node.getNodeType().getParameters().getParameter().get(p).getName().equals("scaling_factor"))
							{
								does_scaling_factor_exist = true;
							}
						}
					}
					// Check that the split ratio solver is of type Hamburger
					if(does_threshold_exist && does_scaling_factor_exist)
					{
						assertEquals("Test: Load real scenario. - verify right SplitRatioSolver",Node_SplitRatioSolver_HAMBURGER.class,split_ratio_solver_field.get(node).getClass());
						
						// Get split ratio solver
						split_ratio_solver = (Node_SplitRatioSolver_HAMBURGER) split_ratio_solver_field.get(node);
						
						// Access update function in Node and invoke it.
						Method updateNode = Node.class.getDeclaredMethod("update", null);
						updateNode.setAccessible(true);
						updateNode.invoke(node, null);
						
						// Find Off-Ramp index.
						int off_ramp_idx = -1;
						for (int i = 0 ; i < node.getOutput_link().length ; i++)
						{
							if ( node.getOutput_link()[i].isOfframp())
							{
								off_ramp_idx = i;
								break;
							}
						}
						if (off_ramp_idx == -1)
						{
							fail("Error getting Off-Ramp link.");
						}
						
						// Access the split_ratio profile.
						Field sr_local_avg = Node.class.getDeclaredField("splitratio_selected");
						sr_local_avg.setAccessible(true);
						
						// Argument for the computeAppliedSplitRatio;
						Object[] arguments = new Object[3];
						arguments[0] = sr_local_avg.get(node);
						arguments[1] = null;
						arguments[2] = new Integer(0);
						
						// Generate input parameters for computeAppliedSplitRatio
						Class[] parameterType =new Class[3];
						parameterType[0] = sr_local_avg.get(node).getClass();
						parameterType[1] = Class.forName("edu.berkeley.path.beats.simulator.Node_FlowSolver$SupplyDemand");
						parameterType[2] = Integer.TYPE; 
						
						// Invoke computeAppliedSplitRatio
						validateCondition = split_ratio_solver.getClass().getDeclaredMethod("computeAppliedSplitRatio", parameterType);
						validateCondition.setAccessible(true);
						Object actual_output = validateCondition.invoke(split_ratio_solver, arguments);
						getActualValue = actual_output.getClass().getDeclaredMethod("getData", null);
						getActualValue.setAccessible(true);
						
						// Get the new split ratio as a double.
						double[][][] actual_output_double = (double[][][]) getActualValue.invoke(actual_output, null);
						
						// Check if the the new split ratios are calculated corrected with a precision of 8 digits after decimal.
						if(node.getId() == 7L)
						{
							assertEquals("",0.104139155,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 11L)
						{
							assertEquals("",0.07618,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 17L)
						{
							assertEquals("",0.01924,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 21L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 27L )
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 29L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 35L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 37L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 43L )
						{
							assertEquals("",0.09831,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 47L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 51L)
						{
							assertEquals("",0.01585,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 58L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 64L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 72L)
						{
							assertEquals("",0.01059,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 76L)
						{
							assertEquals("",0.05349,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 82L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 84L)
						{
							assertEquals("",0.05515,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 88L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 92L)
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 94L)
						{
							assertEquals("",0.51869,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 96L )
						{
							assertEquals("",0.06206,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 98L )
						{
							assertEquals("",0.08712,actual_output_double[0][off_ramp_idx][0],8);
						}
						else if(node.getId() == 106)
						{
							assertEquals("",0.069209393,actual_output_double[0][off_ramp_idx][0],8);
						}
						else
						{
							fail("Can't find node: " + node.getId() + ".");
						}
						
						

					}
					// Check if split ratio solver are of type A.
					else
					{
						assertEquals("Test: Load real scenario. - verify right SplitRatioSolver",Node_SplitRatioSolver_A.class,split_ratio_solver_field.get(node).getClass());
					}
				}
			}
		}
	}


	/* Utility methods */
	// Generates validation environment and calls the validation method.
	private void generateValidationEnvironment(String configuration) throws Exception
	{
		// Generate density.
		HashMap<Integer, double[][]> density = generateDensity(configuration);
		
		// Generate expected output.
		Object exp_out = buildExpectedOutput(configuration);
		
		// Generate VehicleTypes
		List<VehicleType> list = generateVehicleTypes(configuration);
		
		// Generate Scenario
		Scenario scenario = generateScenario(configuration, list, nr_of_ensembles);
		
		// Generate Network
		Network network = generateNetwork(configuration, scenario);
		
		// Generate Node
		Node node = generateNode(configuration, network, density);
		
		// Creating Node_SplitRatioSolver 
		split_ratio_solver = new Node_SplitRatioSolver_HAMBURGER(node);
		
		// Invoke validation
		validateCondition = split_ratio_solver.getClass().getDeclaredMethod("validate", null);
		validateCondition.setAccessible(true);
		validateCondition.invoke(split_ratio_solver);
	}

	// Generates the test environment and performs calculations.	
	private Object generateCalculationEnvironment(String configuration) throws Exception
	{
		// Generate local split ratio.
		double[][][] sr_local_avg_double = buildLocalSR(configuration);
		Object sr_local_avg = constructDouble3DMatrix(sr_local_avg_double);
		
		// Generate VehicleTypes
		List<VehicleType> list = generateVehicleTypes(configuration);
		
		// Generate density.
		HashMap<Integer, double[][]> density = generateDensity(configuration);
		
		// Generate Scenario
		Scenario scenario = generateScenario(configuration, list, nr_of_ensembles);
		
		// Generate Network
		Network network = generateNetwork(configuration, scenario);
		
		// Generate Node
		Node node = generateNode(configuration, network, density);
				
		// Creating Node_SplitRatioSolver 
		split_ratio_solver = new Node_SplitRatioSolver_HAMBURGER(node);
		Object[] arguments = new Object[3];
		arguments[0] = sr_local_avg;
		arguments[1] = null;
		arguments[2] = new Integer(0);
		
		// Generate input parameters for computeAppliedSplitRatio
		Class[] parameterType =new Class[3];
		parameterType[0] = sr_local_avg.getClass();
		parameterType[1] = Class.forName("edu.berkeley.path.beats.simulator.Node_FlowSolver$SupplyDemand");
		parameterType[2] = Integer.TYPE; 
		
		// Invoke computeAppliedSplitRatio
		validateCondition = split_ratio_solver.getClass().getDeclaredMethod("computeAppliedSplitRatio", parameterType);
		validateCondition.setAccessible(true);
		Object actual_output = validateCondition.invoke(split_ratio_solver, arguments);
		
		// Enables access to getData method
		getActualValue = actual_output.getClass().getDeclaredMethod("getData", null);
		getActualValue.setAccessible(true);
		
		return actual_output;
	}
	
	// Builds Links
	private Link linkBuilder(int link_id, String link_type, Scenario scenario, HashMap<Integer, double[][]> density) throws Exception
	{
		// Access the Link constructor.
		Constructor<Link> constructALink= Link.class.getDeclaredConstructor(null);
        constructALink.setAccessible(true);
        
        // Access fields
        Field scenario_field = Link.class.getDeclaredField("myScenario");
        scenario_field.setAccessible(true); 
        
        Field linkBehavior_field = Link.class.getDeclaredField("link_behavior");
        linkBehavior_field.setAccessible(true);
        
        Field length = Link.class.getDeclaredField("_length");
        length.setAccessible(true);
        Field lanes_field = Link.class.getDeclaredField("_lanes");
        lanes_field.setAccessible(true);
        
        // Construct a Link.
        Link link = constructALink.newInstance(null);		
		
        // Set fields.
        link.setId(link_id);
        link.setLanes(1);
        length.set(link, 1);
        lanes_field.set(link, 1);
        // Set Scenario field.
        scenario_field.set(link, scenario);
        
        // Set LinkBehavior field.
        LinkBehaviorCTM linkBehavior = new LinkBehaviorCTM(link);
        linkBehavior_field.set(link, linkBehavior);
        
        // Set density.
        Field density_field = LinkBehaviorCTM.class.getDeclaredField("density");
        density_field.setAccessible(true);
        density_field.set(linkBehavior, density.get(link_id));
        
        
        // Set LinkType.
        LinkType type = new LinkType();
        type.setName(link_type);
        link.setLinkType(type);
        
		return link;
	}
	
	// Constructs Double3DMatrix objects
	private Object constructDouble3DMatrix(double[][][] data) throws Exception
	{
		// Initiation
		int nr_in = data.length;
		int nr_out = 0;
		int nr_types = 0;
		
		if (nr_in > 0)
		{
			nr_out = data[0].length;
		}
		if (nr_out > 0)
		{
			nr_types = data[0][0].length;
		}
		
		// Access class
		Class test = Class.forName("edu.berkeley.path.beats.simulator.Double3DMatrix"); 
		
		// Constructing a double[][][] as a class
		Object outer_container = Array.newInstance(Array.newInstance(Array.newInstance(java.lang.Double.TYPE, nr_in).getClass(), nr_out).getClass(), nr_types);
		// populates the double[][][] with data.
		for (int v = 0 ; v < nr_types ; v++)
		{
			// Constructs a double[][]
			Object middle_container = Array.newInstance(Array.newInstance(java.lang.Double.TYPE, nr_in).getClass(), nr_out);
			for (int o = 0 ; o < nr_out ; o++)
			{
				// Constructs a double[]
				Object inner_container = Array.newInstance(java.lang.Double.TYPE, nr_in);
				for (int i = 0 ; i < nr_in ; i++)
				{
					// Adding the split ratio
					Array.setDouble(inner_container, i, data[i][o][v]);
				}
				
				Array.set(middle_container, o, inner_container);
			}
			Array.set(outer_container, v, middle_container);
		}
		
		// Creates a constructor for the Double3DMatrix
		Class parameterType = outer_container.getClass();
		Constructor constructor = test.getConstructor(parameterType);
		constructor.setAccessible(true);
		
		// Constructs a Double3DMatrix
		Object double3DMatrix = constructor.newInstance(outer_container);

		return double3DMatrix;
	}
	
	// Generate Scenario
	private Scenario generateScenario(String configuration,List<VehicleType> list, int nr_of_ensembles) throws Exception
	{
		// Create a new Scenario.
		Scenario scenario = new Scenario();
		
		// Access RunParameter class
		Class runParameterClass = Class.forName("edu.berkeley.path.beats.simulator.Scenario$RunParameters"); 
		
		Class[] arguments = new Class[10];
		arguments[0] = Scenario.class;
		arguments[1] = Double.TYPE;
		arguments[2] = Double.TYPE;
		arguments[3] = Double.TYPE;
		arguments[4] = Double.TYPE; 
		arguments[5] = Boolean.TYPE;
		arguments[6] = String.class;
		arguments[7] = String.class;
		arguments[8] = Integer.TYPE;
		arguments[9] = Integer.TYPE;
		
		Constructor runParameterConstructor = runParameterClass.getDeclaredConstructor(arguments);
		runParameterConstructor.setAccessible(true);
		
		// Construct a RunParameter class with ensemble information.
		Object[] parameters = new Object[10];
		
		parameters[0] = scenario;
		parameters[1] = new Double(0);
		parameters[2] = new Double(0);
		parameters[3] = new Double(0);
		parameters[4] = new Double(0);
		parameters[5] = new Boolean(false);
		parameters[6] = new String();
		parameters[7] = new String();
		parameters[8] = new Integer(nr_of_ensembles);
		parameters[9] = new Integer(1);	 // Number of lanes.
		
		// Creates a RunParameter object
		Object runParameters = runParameterConstructor.newInstance(parameters);
		
		// Assign RunParameter to the Scenario.
		Field runParam_field = Scenario.class.getDeclaredField("runParam");
		runParam_field.setAccessible(true);
		
		runParam_field.set(scenario, runParameters);
		
		// Create a VehicleTypeSet.	
		VehicleTypeSet vehicleSet = new VehicleTypeSet();
				
		// Access the vehicleType field in the VehicleTypeSet.
		Field vehicleTypeList = VehicleTypeSet.class.getDeclaredField("vehicleType");
		vehicleTypeList.setAccessible(true);		

		// Assign VehicleType to the VehicleTypeSet.
		vehicleTypeList.set(vehicleSet, list);
				
		// Assign VehicleTypeSet to the Scenario.
		scenario.setVehicleTypeSet(vehicleSet);
		
		// Access numVehicleTypes field
		Field numVehicleTypes_field = Scenario.class.getDeclaredField("numVehicleTypes");
		numVehicleTypes_field.setAccessible(true);
		numVehicleTypes_field.set(scenario, list.size());
		
		return scenario;	
	}
	
	// Generate Network
	private Network generateNetwork(String configuration,Scenario scenario) throws Exception
	{
		// Create new Network.
		Network network = new Network();
		
		// Access Scenario field.
		Field scenario_field = Network.class.getDeclaredField("myScenario");
		scenario_field.setAccessible(true);
		
		// Assign Scenario to the Network.
		scenario_field.set(network, scenario);
		
		return network;
	}
	
	// Generate Node
	private Node generateNode(String configuration, Network network, HashMap<Integer, double[][]> density) throws Exception
	{
		// Access the Node constructor.
		Constructor<Node> constructANode= Node.class.getDeclaredConstructor(null);
		constructANode.setAccessible(true);
		        
		// Access fields
		Field input_links = Node.class.getDeclaredField("input_link");
		input_links.setAccessible(true);
				
		Field output_link = Node.class.getDeclaredField("output_link");
		output_link.setAccessible(true);
				
		Field network_field = Node.class.getDeclaredField("myNetwork");
		network_field.setAccessible(true);
		
		Field nodeType_field = Node.class.getSuperclass().getDeclaredField("nodeType");
		nodeType_field.setAccessible(true);

		// Construct a Node.
		node = constructANode.newInstance(null);
		node.setId(0);
		
		// Construct parameters
		NodeType nodeType = new NodeType();
		Parameters parameters = new Parameters();
		if(configuration.equals("Test: calculation no diversion."))
		{
			parameters.addParameter("threshold", "0.062136995");
			parameters.addParameter("scaling_factor", "1.609347219");
		}
		else
		{
			parameters.addParameter("threshold", "0.043495896");
			parameters.addParameter("scaling_factor", "1.609347219");
		}
		
		nodeType.setParameters(parameters);
		
		// Assign fields
		network_field.set(node, network);
		node.setNodeType(nodeType);
		
		
		// Add links to the Node.
		Link[] input = null;
		Link[] output = null;
		if (configuration.equals("Test: validation number of input links (2-to-2)."))
		{
			// Adding input links
		    input = new Link[2];
		    input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
		    input[1] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
		    
		    // Adding output links
		    output = new Link[2];
		    output[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
		    output[1] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
		    
		}
		else if (configuration.equals("Test: validation number of output links (1-to-1)."))
		{
			// Adding input links
			input = new Link[1];
			input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
		        	
			// Adding output links
			output = new Link[1];
			output[0] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
		        		
		}
		else if (configuration.equals("Test: validation number of output links (1-to-3)."))
		{
			// Adding input links
			input = new Link[1];
			input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
			
			// Adding output links
			output = new Link[3];
			output[0] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
			output[1] = linkBuilder(3,"Freeway", network.getMyScenario(), density);
			output[2] = linkBuilder(4,"Freeway", network.getMyScenario(), density);
			
		}
		else if (configuration.equals("Test: validation of link type on the downstream link."))
		{
			// Adding input links
			input = new Link[1];
			input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
			
			// Adding output links
			output = new Link[2];
			output[0] = linkBuilder(2,"Interconnect", network.getMyScenario(), density);
			output[1] = linkBuilder(3,"Off-Ramp", network.getMyScenario(), density);
		        		
		}
		else if (configuration.equals("Test: validation of link type on the diverging link."))
		{
			// Adding input links
			input = new Link[1];
			input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
			
			// Adding output links
			output = new Link[2];
			output[0] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
			output[1] = linkBuilder(3,"Freeway", network.getMyScenario(), density);
			
		}
		// Build a working node
		else 
		{
			
			// Adding input links
			input = new Link[1];
			input[0] = linkBuilder(1,"Freeway", network.getMyScenario(), density);
			
			// Adding output links
			output = new Link[2];
			output[0] = linkBuilder(2,"Freeway", network.getMyScenario(), density);
			output[1] = linkBuilder(3,"Off-Ramp", network.getMyScenario(), density);
				
		}
		
		// Assign links        
		input_links.set(node, input);
		output_link.set(node, output);
		        
		return node;
	}

	// Builds local split ratio
	private double[][][] buildLocalSR(String configuration)
	{
		// Initiation
		int nr_in = 1;
		int nr_out = 2;
		int nr_types = 1;
		
		double[][][] local_sr = new double[nr_in][nr_out][nr_types];
		
		if (configuration.equals("Test: calculation where adjusted sr are negative."))
		{
			for (int vt = 0; vt < nr_types ; vt++)
			{
				local_sr[0][0][vt] =  1.20;
				local_sr[0][1][vt] = -0.80;
			}
		}
		else if(configuration.equals("Test: calculation where adjusted sr are over 1."))
		{	
			for (int vt = 0; vt < nr_types ; vt++)
			{
				local_sr[0][0][vt] = -0.8;
				local_sr[0][1][vt] =  1.8;
			}
		}
		else if(configuration.equals("Test: calculation no diversion."))
		{
			for (int vt = 0; vt < nr_types ; vt++)
			{
				local_sr[0][0][vt] = 0.65;
				local_sr[0][1][vt] = 0.35;
			}
		}
		else if (configuration.equals("Test: calculation diversion."))
		{
			for (int vt = 0; vt < nr_types ; vt++)
			{
				local_sr[0][0][vt] = 0.81;
				local_sr[0][1][vt] = 0.19;
			}
		}
		// Default split ratio.
		else
		{
			for (int vt = 0; vt < nr_types ; vt++)
			{
				local_sr[0][0][vt] = 0.75;
				local_sr[0][1][vt] = 0.25;
			}
		}
		
		return local_sr;
	}	
	
	// Generate vehicleTypes
	private List<VehicleType> generateVehicleTypes(String configuration)
	{
		List<VehicleType> list = new ArrayList<VehicleType>();
		if (configuration.equals(""))
			{
			
			}
		else
		{
			// Create a VehicleType
			VehicleType vehicleType = new VehicleType();
			vehicleType.setId(1);
				
			// Add the VehicleType
			list.add(vehicleType);
		}
			
		return list;
	}
		
	// Build input density
	private HashMap<Integer, double[][]> generateDensity(String configuration)
	{
		// Initiation
		double[][] density = null;
		HashMap<Integer, double[][]> density_map = new HashMap<Integer, double[][]>();
			
			
		if(configuration.equals("Test: calculation no diversion."))
		{
			// Link 1 Ensemble 1
			density = new double[1][1];
			density[0][0] = 0.037282197;
			density_map.put(1, density.clone());
				
			// Link 2 Ensemble 1
			density[0][0] = 0.037282197;
			density_map.put(2, density.clone());
			
			// Link 3 Ensemble 1
			density[0][0] = 0.037282197;
			density_map.put(3, density.clone());
			
			// Link 4 Ensemble 1
			density[0][0] = 0.037282197;
			density_map.put(4, density.clone());
		}
		// Default split ratio.
		else
		{
			// Link 1 Ensemble 1
			density = new double[1][1];
			density[0][0] = 0.049709596;
			density_map.put(1, density.clone());
				
			// Link 2 Ensemble 1
			density[0][0] = 0.049709596;
			density_map.put(2, density.clone());
			
			// Link 3 Ensemble 1
			density[0][0] = 0.049709596;
			density_map.put(3, density.clone());
			
			// Link 4 Ensemble 1
			density[0][0] = 0.049709596;
			density_map.put(4, density.clone());			
		}
			
		return density_map;
	}
		
	// Builds expected output as a double
	private double[][][] buildExpectedOutput(String configuration)
	{
		double[][][] exp_output = null;
		
		if(configuration.equals("Test: calculation where adjusted sr are negative."))
		{
			exp_output = new double[1][2][1];
			exp_output[0][0][0] = 1.0;
			exp_output[0][1][0] = 0.0;
		}
		else if(configuration.equals("Test: calculation where adjusted sr are over 1."))
		{
			exp_output = new double[1][2][1];
			exp_output[0][0][0] = 0.0;
			exp_output[0][1][0] = 1.0;
		}
		else if (configuration.equals("Test: calculation diversion."))
		{
			exp_output = new double[1][2][1];
			exp_output[0][0][0] = 0.80;
			exp_output[0][1][0] = 0.20;
		}
		else if(configuration.equals("Test: calculation no diversion."))
		{
			exp_output = new double[1][2][1];
			exp_output[0][0][0] = 0.65;
			exp_output[0][1][0] = 0.35;
		}
		


		return exp_output;
	}
}