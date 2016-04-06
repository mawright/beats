package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

/**
 * Created by matt on 4/5/16.
 */
public class GatedHOVSplitRatioGenTest {

	private static Scenario scenario;
	private static long offramp_link_id = 7;
	private static long upstream_hov_id = 33;
	private static long downstream_freeway_id = 4;

	private static long node_id = 5;

	private static int sov_id = 0;

	private static Link offramp_link, upstream_hov_link;
	private static Node node;
	private static String outprefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "test";

	@BeforeClass
	public static void setup() {
		try {
			scenario = Jaxb.create_scenario_from_xml("data" + File.separator + "config" + File.separator + "_smalltest_gated_HOV.xml");
			String split_logger_prefix = "data" + File.separator + "test" + File.separator + "output" + File.separator + "testSplits";

			scenario.initialize(5, 0, 3600, 5, "text", outprefix, 1, 1, "gaussian", "general", "balancing", null, "fw_fr_split_output_hov", split_logger_prefix, 5d, null);

			offramp_link = scenario.get.linkWithId(offramp_link_id);
			upstream_hov_link = scenario.get.linkWithId(upstream_hov_id);

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
	public void testGatedHov() {
		try {
			scenario.advanceNSeconds(300);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		double offrampSplitRatio = node.getSplitRatio(node.getInputLinkIndex(downstream_freeway_id), node.getOutputLinkIndex(offramp_link_id), sov_id);

		double switchedVehicles = upstream_hov_link.getInflowInVeh(0, 2);

		assertTrue(switchedVehicles > 0d);

		double totalOfframpFlow = offramp_link.getTotalInflowInVeh(0);
//		assertEquals(25d * 5d / 3600d, totalOfframpFlow, 1e-3);

	}
}
