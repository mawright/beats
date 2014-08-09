package edu.berkeley.path.beats.test.simulator;

import edu.berkeley.path.beats.simulator.DemandProfile;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by gomes on 6/9/2014.
 */
public class SetProfileTest {

    private static Scenario scenario;
    private static String config_folder = "data/config/";
    private static String config_file = "three_link.xml";
    private long source_link_id = 1;
    private long sink_link_id = 3;

    @BeforeClass
    public static void setUp() throws Exception {
        scenario = ObjectFactory.createAndLoadScenario(config_folder + config_file);
        if(scenario==null)
            fail("scenario did not load");
        double timestep = 5;
        double starttime = 0;
        double endtime = 3600;
        int numEnsemble = 1;
        scenario.initialize(timestep, starttime, endtime, numEnsemble);
    }

    @Test
    public void test_setDemandProfile() {

        HashMap<Long,double []> hmap;
        ArrayList<Double> inflow = new ArrayList();
        Link link = scenario.getLinkWithId(source_link_id);

        try {

            // set demands to 100,200 vph with dt=50 seconds
            scenario.set_demand_for_link_si(source_link_id,50d,new double [] {100d/3600d,200d/3600d});

            // advance 120 seconds
            for(int i=0;i<24;i++){
                scenario.advanceNSeconds(5d);
                inflow.add(link.getInflowInVeh(0)[0]*3600d/5d);
            }

            // change demands to constant 300 vph
            scenario.set_demand_for_link_si(source_link_id,0d, new double [] {300d/3600d});

            // advance 120 seconds
            for(int i=0;i<24;i++){
                scenario.advanceNSeconds(5d);
                inflow.add(link.getInflowInVeh(0)[0]*3600d/5d);
            }

        } catch ( Exception exp){
            assertTrue(false);
        }

        // inflow should be 100 vph for 10 steps, then 200 vph for 14 steps, then 300 for 24 steps
        for(int i=0;i<10;i++)
            assertEquals(inflow.get(i),100d,1E-2);
        for(int i=10;i<24;i++)
            assertEquals(inflow.get(i),200d,1E-2);
        for(int i=24;i<48;i++)
            assertEquals(inflow.get(i),300d,1E-2);
    }

    @Test
    public void test_setCapcityProfile() {

        ArrayList<Double> outflow = new ArrayList();
        Link link = scenario.getLinkWithId(sink_link_id);

        try{

            // add a demand of 1800 vph to source link
            scenario.set_demand_for_link_si(source_link_id,0d,new double [] {1800d/3600d});

            // block the sink
            scenario.set_capacity_for_link_si(sink_link_id,0d,new double[]{0d});

            // advance 120 seconds
            for(int i=0;i<24;i++){
                scenario.advanceNSeconds(5d);
                outflow.add(link.getOutflowInVeh(0)[0]*3600d/5d);
            }

            // unblock the sink
            scenario.set_capacity_for_link_si(sink_link_id,3600d,new double[]{100d});

            // advance 120 seconds
            for(int i=0;i<24;i++){
                scenario.advanceNSeconds(5d);
                outflow.add(link.getOutflowInVeh(0)[0]*3600d/5d);
            }

        } catch ( Exception exp){
            System.err.println(exp);
            assertTrue(false);
        }

        // outflow should be 0 vph for 24 steps, then 1800 for 24 steps
        for(int i=0;i<24;i++)
            assertEquals(outflow.get(i),0d,1E-2);
        for(int i=24;i<48;i++)
            assertEquals(outflow.get(i),1800d,1E-2);

    }

}
