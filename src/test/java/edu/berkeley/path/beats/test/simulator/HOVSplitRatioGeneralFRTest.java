package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.BeforeClass;
import org.junit.Test;
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

	private static long node_id = 4;

	private static int sov_id = 0;
	private static int hov_id = 1;

	private static Link offramp_link, upstream_freeway_link, upstream_hov_link;
	private static Node node;

	@BeforeClass
	public static void setup() {
		try {
			scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_SRcontrol_HOV.xml");
			String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";
			String split_logger_prefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "testSplits";

			scenario.initialize(5, 0, 3600, 5, "text", outprefix, 1, 1, "gaussian", "general", "balancing", null, "fw_fr_split_output", split_logger_prefix, 5d, null);

			offramp_link = scenario.get.linkWithId(offramp_link_id);
			upstream_freeway_link = scenario.get.linkWithId(upstream_freeway_id);
			upstream_hov_link = scenario.get.linkWithId(upstream_hov_id);

			node = scenario.get.nodeWithId(node_id);
		}
		catch (BeatsException ex) {
			ex.printStackTrace();
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
		assertEquals(HOVofframpFlow, 25d*5d / 3600d / 2d, 1e-6); // requested flow: 25 veh per sec, split evenly between hov and sov

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		assertEquals(HOVSR, SOVSR, 1e-6);

	}

	@Test
	public void someHOVsInFreewayTest() {
		Double[] density = upstream_freeway_link.getDensityInVeh(0);
		density[hov_id] += upstream_hov_link.getDensityInVeh(0, hov_id);

		try {
			scenario.advanceNSeconds(5);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double HOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_hov_id), node.getOutputLinkIndex(offramp_link_id), hov_id);
		double SOVSR = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		double HOVSR_freeway = node.getSplitRatio( node.getInputLinkIndex(upstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), hov_id);

	}

	@Test
	public void zeroTest() {
		try {
			scenario.advanceNSeconds(100d);
		}
		catch (BeatsException ex) {
			ex.printStackTrace();
		}

		double HOVdensity = offramp_link.getDensityInVeh(0, hov_id);
		double SOVdensity = offramp_link.getDensityInVeh(0, sov_id);

		assertEquals(0d, HOVdensity, 1e-6);
		assertEquals(0d, SOVdensity, 1e-6);
	}

	@Test
	public void setTest() {

		try {
			scenario.set.demand_for_link_si(offramp_link_id, 50, new double[]{100 / 3600, 200 / 3600});
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
