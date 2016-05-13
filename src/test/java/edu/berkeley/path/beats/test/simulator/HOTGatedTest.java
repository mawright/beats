package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.control.Controller_HOT_Lookup;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;

/**
 * Created by matt on 5/11/16.
 */
public class HOTGatedTest {

	private static Scenario scenario;
	private static long onramp_id = 222;
	private static long upstream_hot_id = 22;
	private static long upstream_gp_id = 2;

	private static long downstream_hot_id = 33;
	private static long downstream_gp_id = 3;

	private static int sov_index = 0;
	private static int rtp_index = 2;

	private static Link onramp_link, upstream_hot_link, upstream_gp_link, downstream_hot_link, downstream_gp_link;
	private static Controller_HOT_Lookup controller;

	private static String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";

	@BeforeClass
	public static void setup() {
		try {
			scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_gated_HOT.xml");

			scenario.initialize(5, 0, 3600, 1, "gaussian", "general", "balancing");

			onramp_link = scenario.get.linkWithId(onramp_id);
			downstream_hot_link = scenario.get.linkWithId(downstream_hot_id);
			onramp_link = scenario.get.linkWithId(onramp_id);
			upstream_hot_link = scenario.get.linkWithId(upstream_gp_id);
			upstream_gp_link = scenario.get.linkWithId(upstream_gp_id);
			downstream_gp_link = scenario.get.linkWithId(downstream_gp_id);

			controller = (Controller_HOT_Lookup) scenario.get.controllerWithId(0L);
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
	public void testLoading() {
		assertNotNull(controller);
	}

	@Test
	public void testUpdatePrice() {
		try {
			scenario.advanceNSeconds(5d);
		} catch (BeatsException ex) {
			ex.printStackTrace();
		}

		assertEquals(0.25d, controller.getPriceAtLinkForVehtype(downstream_hot_id, scenario.get.vehicleTypeIdForIndex(sov_index)), 1e-3);
	}

	@Test
	public void testActuation() {
		try {
			scenario.advanceNSeconds(300d);
		} catch (BeatsException ex) {
			ex.printStackTrace();
		}

		assertTrue( downstream_hot_link.getDensityInVeh(0, rtp_index) > 0d);
		assertTrue( upstream_gp_link.getDensityInVeh(0, rtp_index) > 0d );
		assertTrue( onramp_link.getDensityInVeh(0, rtp_index) > 0d);
	}

	@Test
	public void testStatusInCongestion() {
		try {
			scenario.advanceNSeconds(300d);
		} catch (BeatsException ex) {
			ex.printStackTrace();
		}

		assertTrue( 0.25d < controller.getPriceAtLinkForVehtype(downstream_hot_id, scenario.get.vehicleTypeIdForIndex(sov_index)) );
	}
}
