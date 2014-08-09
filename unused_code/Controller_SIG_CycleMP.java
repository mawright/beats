package edu.berkeley.path.beats.control;

import java.lang.Math;

import edu.berkeley.path.beats.actuator.ActuatorSignalStageSplits;
import edu.berkeley.path.beats.actuator.StageSplit;
import edu.berkeley.path.beats.simulator.*;

public class Controller_SIG_CycleMP extends Controller_SIG {

    private Node myNode;
    private Link [] inputLinks;
	private Link [] outputLinks;
	private int nInputs;
	private int nOutputs;
	private double [] satFlows;
	private int nStages;
    private int [][] controlMat;

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_SIG_CycleMP(Scenario myScenario,edu.berkeley.path.beats.jaxb.Controller c,Controller.Algorithm myType) {
        super(myScenario,c,myType);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    // assign values to your controller-specific variables
	@Override
	protected void populate(Object jaxbobject) {
		super.populate(jaxbobject);
        myNode = myScenario.getNodeWithId(signal.getNodeId());

        inputLinks = myNode.getInput_link();
        outputLinks = myNode.getOutput_link();
        nInputs = inputLinks.length;
        nOutputs = outputLinks.length;

        //construct control matrices
        nStages = stages.length;
        controlMat = new int [nStages][nInputs]; // initializes to filled with 0
        for(int s=0; s<nStages; s++){
            for (Link a: stages[s].phaseA.getTargetlinks()){
                for (int i=0;i<nInputs;i++){
                    if (inputLinks[i].getId()==a.getId()){
                        controlMat[s][i]=1;
                        break;
                    }
                }
            }
            for (Link b: stages[s].phaseB.getTargetlinks()){
                for (int i=0;i<nInputs;i++){
                    if (inputLinks[i].getId()==b.getId()){
                        controlMat[s][i]=1;
                        break;
                    }
                }
            }
        }

	}

    // validate the controller parameters.
	// use BeatsErrorLog.addError() or BeatsErrorLog.addWarning() to register problems
	@Override
	protected void validate() {
		super.validate();

        if(!BeatsMath.equals(dtinseconds,cycle_time)){
            BeatsErrorLog.addError("Controller_SIG_CycleMP requires dt==cycle_time");
        }
	}

	// main controller update function, called every controller dt.
	// use this.sensors to get information, and this.actuators to apply the control command.
	@Override
	protected void update() throws BeatsException {
		super.update();

        //get sat flow information from links
        satFlows = new double [nInputs];
        for(int i=0;i<nInputs;i++){
            satFlows[i] = inputLinks[i].getCapacityInVeh(0);
        }

		//get counts from links (no sensor needed)
		double [] inputCounts = new double[nInputs];
        double [] outputCounts = new double[nOutputs];
		for(int i=0;i<nInputs;i++){
            inputCounts[i]= Math.round(inputLinks[i].getTotalDensityInVeh(0));
        }
		for(int j=0;j<nOutputs;j++){
            outputCounts[j]= Math.round(outputLinks[j].getTotalDensityInVeh(0));
        }

		// get splits from node:  CAN THIS BE MOVED TO THE POPULATE FUNCTION? Assuming time invariant...
        double[][] splits = new double[nInputs][nOutputs];
		for(int i=0;i<nInputs;i++){
			for(int j=0;j<nOutputs;j++){
				splits[i][j]=myNode.getSplitRatio(i, j, 0);
			}
		}

        //calculate SIMPLE weights
        //later this will be changed, to calculate max/min/average/etc weights, and to include minimum green time constraints.
        double[] weights = new double [nInputs];
        for (int i=0; i<nInputs; i++){
        	weights[i]=inputCounts[i];
        	for (int e=0; e<nOutputs; e++){
        		weights[i]-=splits[i][e]*outputCounts[e];
        	}
        }

        //calculate pressure for all stages
        double[] pressures = new double [nStages];
        for (int s=0; s<nStages;s++){
        	pressures[s] = 0;
        	for (int i=0; i<nInputs; i++){
        		pressures[s] += controlMat[s][i]*weights[i]*satFlows[i];
        	}
        }

        System.out.println(this.getId());
        for(double s : pressures)
            System.out.print(s+"\t");
        System.out.print("\n");


        //determine max pressure stage
        int mpStage = 0;
        for (int s=1;s<nStages;s++){
        	if (pressures[s]>pressures[mpStage]){
                mpStage = s;
            }
        }

        StageSplit [] green_splits = new StageSplit[nStages];
        for (int s=0; s<nStages;s++)
            green_splits[s] = new StageSplit(stages[s],0d);
        green_splits[mpStage].split = 1d;

        ((ActuatorSignalStageSplits)actuators.get(0)).setStageSplits(green_splits);


    }
}
