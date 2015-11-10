package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsFormatter;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by matt on 10/8/15.
 */
public class Node_SplitRatioSolver_Balancing_Test {
	private static String config_folder = "data/config/";
	private static String output_folder = "data/test/output/";
	private static String logger_prefix = "balancingSRTestOutput";
	private static String fixture_folder = "data/test/fixture/";

	@Before
	public void createOutput(){
		File file = new File(output_folder);
		if(!file.exists())
			file.mkdir();
	}

	@After
	public void clearOutput(){
		File file = new File(output_folder);
		String[] myFiles;
		if(file.isDirectory()){
			myFiles = file.list();
			for (int i=0; i<myFiles.length; i++) {
				File myFile = new File(file, myFiles[i]);
				myFile.delete();
			}
		}
	}


	@Test
	public void HOVInterchangeOneTimestepTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml(config_folder + "_2x2_twotypes_incompletesplits.xml");
			// initialize
			double timestep = 1;
			double starttime = 0;
			double endtime = 1;
			int numEnsemble = 1;
			scenario.initialize(timestep, starttime, endtime, Double.POSITIVE_INFINITY, "", "", 1, numEnsemble,
					"gaussian", "general", "balancing", "", "normal", output_folder + logger_prefix, 1d, null );
			scenario.reset();

			scenario.run();

			ArrayList<ArrayList<Double>> result = BeatsFormatter.readCSV(output_folder+logger_prefix +"-5.txt","\t");
			ArrayList<ArrayList<Double>> expected = BeatsFormatter.readCSV(fixture_folder+logger_prefix+"-5.txt","\t");

			assertTrue(BeatsMath.equals2D(result,expected));

		}
		catch (BeatsException e) {
			fail(e.getMessage());
		}

	}
}
