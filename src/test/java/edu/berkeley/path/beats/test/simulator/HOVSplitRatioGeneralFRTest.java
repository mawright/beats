package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.*;

import static org.junit.Assert.*;

import java.io.File;

/**
 * Created by matt on 2/10/16.
 */
public class HOVSplitRatioGeneralFRTest {

	private static Scenario scenario;
	private static long offramp_link_id = 7;
	private static long upstream_freeway_id = 3;
	private static long upstream_hov_id = 33;
	private static long downstream_freeway_id = 4;
	private static long downstream_hov_id = 44;

	private static long node_id = 4;

	private static int sov_id = 0;
	private static int hov_id = 1;

	private static Link offramp_link, upstream_freeway_link, upstream_hov_link, downstream_freeway_link, downstream_hov_link;
	private static Node node;
	private static String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";

	@BeforeClass
	public static void setup() {
		try {
			scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_SRcontrol_HOV.xml");
			String split_logger_prefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "testSplits";

			scenario.initialize(5, 0, 3600, 5, "text", outprefix, 1, 1, "gaussian", "general", "balancing", null, "fw_fr_split_output_hov", split_logger_prefix, 5d, null);

			offramp_link = scenario.get.linkWithId(offramp_link_id);
			upstream_freeway_link = scenario.get.linkWithId(upstream_freeway_id);
			upstream_hov_link = scenario.get.linkWithId(upstream_hov_id);
			downstream_freeway_link = scenario.get.linkWithId(downstream_freeway_id);
			downstream_hov_link = scenario.get.linkWithId(downstream_hov_id);

			node = scenario.get.nodeWithId(node_id);
		}
		catch (BeatsException ex) {
			ex.printStackTrace();
		}
	}

	@Before
	public void createOutput(){
		File file = new File(outprefix);
		if(!file.exists())
			file.mkdir();
	}

	@After
	public void clearOutput(){
		File file = new File(outprefix);
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
	public void basicTest() {

		try {
			scenario.advanceNSeconds(100);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double HOVofframpFlow = offramp_link.getInflowInVeh(0, hov_id);
		double SOVofframpFlow = offramp_link.getInflowInVeh(0, sov_id);

		assertEquals(HOVofframpFlow, SOVofframpFlow, 1e-6);
		assertEquals(HOVofframpFlow, 25d*5d / 3600d / 2d, .01d); // requested flow: 25 veh per sec, split evenly between hov and sov

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		assertEquals(HOVSR, SOVSR, 1e-6);

	}

	@Test
	public void someHOVsInFreewayTest() {
		Double[] density = upstream_freeway_link.getDensityInVeh(0);
		density[hov_id] += upstream_hov_link.getDensityInVeh(0, hov_id);
		upstream_freeway_link.set_density_in_veh(0,density);

		try {
			scenario.advanceNSeconds(100);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		assertEquals(HOVSR, SOVSR, 1e-3);

		double HOVSR_freeway = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), hov_id);

		assertEquals(HOVSR_freeway, HOVSR, 1e-3); // should get equal portion of vehicles of all three vehtype/link combinations

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);

		assertEquals(25d * 5d / 3600d, totalOfframpFlow, 1e-3);

	}

	@Test
	public void moreHOVsInFreewayTest() {
		Double[] density = upstream_freeway_link.getDensityInVeh(0);
		density[hov_id] += upstream_hov_link.getDensityInVeh(0, hov_id) * 2;
		upstream_freeway_link.set_density_in_veh(0,density);

		try {
			scenario.advanceNSeconds(100);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		assertEquals(HOVSR, SOVSR, 1e-3);

		double HOVSR_freeway = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), hov_id);

		assertEquals(HOVSR_freeway, HOVSR, 1e-3); // should get equal portion of vehicles of all three vehtype/link combinations

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);

		assertEquals(25d * 5d / 3600d, totalOfframpFlow, 1e-3);

	}

	@Test
	public void congestedRoadTest() {
		try { // make the road downstream of the offramp congested
			Double[] new_freeway_density = new Double[2];
			new_freeway_density[0] = downstream_freeway_link.getCapacityInVeh(0) / 2d * .9;
			new_freeway_density[1] = downstream_freeway_link.getCapacityInVeh(0) / 2d * .9;
			downstream_freeway_link.set_density_in_veh(0, new_freeway_density);

			Double[] new_hov_density = new Double[2];
			new_hov_density[0] = 0d;
			new_hov_density[1] = downstream_hov_link.getCapacityInVeh(0) * .9;
			downstream_hov_link.set_density_in_veh(0, new_hov_density);

			scenario.advanceNSeconds(5d);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);
		assertEquals(HOVSR, SOVSR, 1e-3);

		double HOVSR_freeway = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		assertEquals(HOVSR_freeway, HOVSR, 1e-3); // should get equal portion of vehicles of all three vehtype/link combinations

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);
		assertEquals(25d * 5d / 3600d, totalOfframpFlow, 1e-3);
	}

	@Test
	public void zeroDemandTest() {
		scenario.set.demand_knob_for_link_id(offramp_link_id, 0d);

		try {
			scenario.advanceNSeconds(100d);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);

		assertEquals(0d, totalOfframpFlow, 1e-6);
	}

	@Test
	public void setNewDemandTest() {
		scenario.set.demand_knob_for_link_id(offramp_link_id, 2d);

		try {
			scenario.advanceNSeconds(100d);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);

		assertEquals(50d * 5d / 3600d, totalOfframpFlow, .01d);
	}
}
