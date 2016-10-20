package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.jaxb.Demand;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsFormatter;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.*;

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
	@Ignore
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

	// this test has a scenario where the split ratio profile is defined like: 1, -1, 1, such that for part of the
	// timeseries a movement is fully defined and another part it is solved via the SRsolver
	@Test
	public void PartlyUndefinedSRProfileTest() {

		try {
			Scenario scenario = Jaxb.create_scenario_from_xml(config_folder + "_2x2_twotypes_incompletesplits_timeseries.xml");
			// initialize

			double timestep = 1;
			double starttime = 0;
			double endtime = 1000;
			int numEnsemble = 1;
			scenario.initialize(timestep, starttime, endtime, Double.POSITIVE_INFINITY, "", "", 1, numEnsemble,
					"gaussian", "general", "balancing", "", "normal", output_folder + logger_prefix, 1d, null );

			scenario.reset();

			scenario.advanceNSeconds(200);

			// SOVs not allowed in the HOV lane in first 300 seconds
			assertEquals( scenario.get.linkWithId(4).getInflowInVeh(0, 0), 0d, 1e-3 );

			scenario.advanceNSeconds(300);

			// SOVs are allowed in the HOV lane in the next 300 seconds
			assertTrue( scenario.get.linkWithId(4).getInflowInVeh(0, 0) > 0d);

			scenario.advanceNSeconds(400);

			// SOVs not allowed in the HOV lane afterwards
			assertEquals( scenario.get.linkWithId(4).getInflowInVeh(0, 0), 0d, 1e-3 );


		} catch (BeatsException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void HOVonlyTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_SRcontrol_HOV.xml");
			String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";
			String split_logger_prefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "testSplits";

			long sov_veh_id = 0;
			long hov_veh_id = 1;

			long[] gp_link_ids = {2, 3, 4, 5};
			long[] hov_link_ids = {22, 33, 44, 55};

			// get rid of all sov demand
			for (edu.berkeley.path.beats.jaxb.DemandProfile dp : scenario.getDemandSet().getDemandProfile()) {
				Link link = scenario.get.linkWithId(dp.getLinkIdOrg());
				if (!link.isSink()) {
					for (Demand demand : dp.getDemand())
						if (demand.getVehicleTypeId() == sov_veh_id)
							demand.setContent("0");
				}
			}

			scenario.initialize(5, 0, 3600, 5, "text", outprefix, 1, 1, "gaussian", "general", "balancing", null, "normal", split_logger_prefix, 5d, null);

			// ensure no sovs anywhere
			for (edu.berkeley.path.beats.jaxb.Link link : scenario.getNetworkSet().getNetwork().get(0).getLinkList().getLink()) {
				if (((Link) link).getDensityInVeh(0)[0] > 0d)
					fail();
			}

			for (int t=0; t < 6; t++) {
				scenario.advanceNSeconds(100d);

				// there should be equal utilization of both links
				for (int i = 0; i < gp_link_ids.length; i++) {
					Link hov_link = scenario.get.linkWithId(hov_link_ids[i]);
					Link gp_link = scenario.get.linkWithId(gp_link_ids[i]);

					double hov_occ_ratio = hov_link.getDensityInVeh(0, 1) / hov_link.getCapacityInVeh(0);
					double gp_occ_ratio = gp_link.getDensityInVeh(0, 1) / gp_link.getCapacityInVeh(0);

//				System.out.println("HOV link " + hov_link_ids[i] +" ratio " + hov_occ_ratio + " GP link " + gp_link_ids[i] + " ratio " + gp_occ_ratio);
//				assertEquals(hov_occ_ratio, gp_occ_ratio, 1e-3);
				}
//			Link offramp_link = scenario.get.linkWithId(7);
//			System.out.println("Offramp link 7 (downstream of 3/33) ratio " + offramp_link.getDensityInVeh(0,1) / offramp_link.getCapacityInVeh(0));
			}

		}
		catch (BeatsException ex) {
			fail(ex.getMessage());
		}
	}

	/*this test is ignored because there has been a design decision to encode a statement that a certain commodity
	 is not allowed into a link by explicitly writing it into the sr profile, i.e. in the xml*/
	@Test
	@Ignore
	public void noSOVInHOVLaneTest() {
		try {
			Scenario scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_SRcontrol_HOV.xml");
			String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";
			String split_logger_prefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "testSplits";

			scenario.initialize(5, 0, 3600, 5, "text", outprefix, 1, 1, "gaussian", "general", "balancing", null, "fw_fr_split_output_hov", split_logger_prefix, 5d, null);
			scenario.advanceNSeconds(100d);

			long hov_link_id = 33;

			Link hov_link = scenario.get.linkWithId(hov_link_id);

			assertEquals( 0d, hov_link.getDensityInVeh(0, 0), 1e-6);

		}
		catch (BeatsException ex) {
			ex.printStackTrace();
			fail();
		}
	}
}
