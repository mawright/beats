package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Scenario;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by matt on 5/13/16.
 */
public class PriceLogger {

	Controller myController;
	int dt_steps;
	BufferedWriter writer;

	public PriceLogger(Controller controller){
		Scenario myScenario = controller.getMyScenario();
		myController = controller;
		dt_steps = (int) Math.round(myController.getDtinseconds() / myScenario.get.simdtinseconds());
		try{
			writer = new BufferedWriter(new FileWriter(myScenario.get.outputPrefix()+"_price_"+controller.getId()+".txt"));
		} catch (IOException e){
			return;
		}
	}

	public void write(){

		Scenario myScenario = myController.getMyScenario();

		if(myScenario.get.clock().getRelativeTimeStep()%dt_steps!=0)
			return;

		int l,k;
		List<Long> linkIds = ((Controller_HOT_Lookup) myController).getLinkIds();
		for(l=0;l<linkIds.size();l++) {
			for (k = 0; k < myScenario.get.numVehicleTypes(); k++) {
				try {
					writer.write(
							String.format("%.1f\t%d\t%d\t%f\n",
									myScenario.get.currentTimeInSeconds(),
									linkIds.get(l),
									myScenario.get.vehicleTypeIdForIndex(k),
									((Controller_HOT_Lookup) myController).getPriceAtLinkForVehtype(
											linkIds.get(l), myScenario.get.vehicleTypeIdForIndex(k)) ));
				} catch(IOException ioe) {
					System.out.println(ioe.getMessage());
				}
			}
		}
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
