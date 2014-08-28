package edu.berkeley.path.beats;

import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 4/16/14.
 */
public class Tester {

    public static void main(String[] args) {


        try{

            //String xml_file = "/home/matt/workspace_L0/L0-estimation/classes/210W/210W_v13_stochastic.xml";
        	//String xml_file = "/home/matt/workspace_L0/beats/demo.xml";
        	String xml_file = "/home/matt/workspace_L0/L0-estimation/classes/tests/configs/_smalltest_noisy_splits.xml";
        	Scenario scenario = (Scenario) ObjectFactory.createAndLoadScenario(xml_file);

            double simulation_dt = 5d;
            double start_time = 0d;
            double end_time = 86400d;
            int numEnsembles = 10;

            scenario.initialize(simulation_dt, start_time, end_time, numEnsembles);

//            scenario.run();
            scenario.advanceNSeconds(1200);
            
            double [][] X = scenario.getTotalDensity(scenario.getNetworkSet().getNetwork().get(0).getId());

            double [][] density_state = new double [7][10];
            for(int i=0;i<7;i++)
                for(int j=0;j<numEnsembles;j++)
                    density_state[i][j] = i+j;

//            scenario.setTotalDensity(density_state);

//            System.out.println(X);

            // scenario.setTotalDensity(3);

            // Y = scenario.getTotalDensity(-1);
        }
        catch(Exception e){
            System.err.print(e);
        }

    }


}
