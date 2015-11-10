package edu.berkeley.path.beats.simulator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by gomes on 2/13/14.
 */
public class SplitRatioLogger implements Serializable {

    private static final long serialVersionUID = 6170765784312344779L;

    Node myNode;
    int dt_steps;
    BufferedWriter writer;

    public SplitRatioLogger(Node node){
        Scenario myScenario = node.myNetwork.getMyScenario();
        myNode = node;
        dt_steps = (int) Math.round(myScenario.split_logger_dt/myScenario.get.simdtinseconds());
        try{
            writer = new BufferedWriter(new FileWriter(myScenario.split_logger_prefix+node.getId()+".txt"));
        } catch (IOException e){
            return;
        }
    }

    public void write(Double [][][] splitratio_applied){

        Scenario myScenario = myNode.getMyNetwork().getMyScenario();

        if(myScenario.get.clock().getRelativeTimeStep()%dt_steps!=0)
            return;

        int i,j,k;
        for(i=0;i<myNode.getnIn();i++)
            for(j=0;j<myNode.getnOut();j++)
                for(k=0;k<myScenario.get.numVehicleTypes();k++){
                    try{
                        writer.write(
                                String.format("%.1f\t%d\t%d\t%d\t%f\n",
                                        myScenario.get.currentTimeInSeconds(),
                                        myNode.getInput_link()[i].getId(),
                                        myNode.getOutput_link()[j].getId(),
                                        myScenario.get.vehicleTypeIdForIndex(k),
                                        splitratio_applied[i][j][k]));
                    } catch(IOException ioe){
                        System.out.println(ioe.getMessage());
                    }
                }
    }

    public void close() throws IOException{
        writer.close();
    }

}
