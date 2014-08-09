package edu.berkeley.path.beats.control;

import java.lang.Math;

import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;

//public class Controller_SIG_CycleDistMP extends Controller_SIG {
//
//    private Node myNode;
//    private Link [] inputLinks;
//	private Link [] outputLinks;
//	private int nInputs;
//	private int nOutputs;
//	private int[] satFlows;
//	private int nStages;
//    private int [][] controlMat;
//    private double[] minGreens;
//
//    public double [] green_splits;          // [sums to 1] in the order of stages.
//
//
//    /////////////////////////////////////////////////////////////////////
//    // Construction
//    /////////////////////////////////////////////////////////////////////
//
//    public Controller_SIG_CycleDistMP(Scenario myScenario,edu.berkeley.path.beats.jaxb.Controller c,Controller.Algorithm myType) {
//        super(myScenario,c,myType);
//    }
//
//    /////////////////////////////////////////////////////////////////////
//    // populate / validate / reset  / update
//    /////////////////////////////////////////////////////////////////////
//
//    // assign values to your controller-specific variables
//	@Override
//	protected void populate(Object jaxbobject) {
////		super.populate(jaxbobject);
////        myNode = myScenario.getNodeWithId(signal.getNodeId());
////        inputLinks = myNode.getInput_link();
////    	outputLinks = myNode.getOutput_link();
////    	nInputs = inputLinks.length;
////		nOutputs = outputLinks.length;
////
////      //construct control matrices and get min greens
////        nStages = stages.length;
////        controlMat = new int [nStages][nInputs]; // initializes to filled with 0
////        minGreens = new double [nStages];
////        for(int s=0; s<nStages; s++){
////        	minGreens[s] = Math.min(stages[s].phaseA.getMingreen(), stages[s].phaseB.getMingreen());
////        	for (Link a: stages[s].phaseA.getTargetlinks()){
////        		for (int i=0;i<nInputs;i++){
////        			if (inputLinks[i].getId()==a.getId()){
////        				controlMat[s][i]=1;
////        				break;
////        			}
////        		}
////        	}
////        	for (Link b: stages[s].phaseB.getTargetlinks()){
////        		for (int i=0;i<nInputs;i++){
////        			if (inputLinks[i].getId()==b.getId()){
////        				controlMat[s][i]=1;
////        				break;
////        			}
////        		}
////        	}
////        }
////
////        //get sat flow information from links
////		satFlows = new int[nInputs];
////		for(int i=0;i<nInputs;i++){
////            satFlows[i] = (int) inputLinks[i].getCapacityInVeh(0);
////        }
//	}
//
//	// validate the controller parameters.
//	// use BeatsErrorLog.addError() or BeatsErrorLog.addWarning() to register problems
//	@Override
//	protected void validate() {
//		super.validate();
//	}
//
//	// called before simulation starts. Set controller state to initial values.
//	@Override
//	protected void reset() {
//		super.reset();
//	}
//
//	// main controller update function, called every controller dt.
//	// use this.sensors to get information, and this.actuators to apply the control command.
//	@Override
//	protected void update() throws BeatsException {
//		super.update();
//
//		//get counts from links (no sensor needed)
//		int[] inputCounts = new int[nInputs];
//        int[] outputCounts = new int[nOutputs];
//		for(int i=0;i<nInputs;i++){
//            inputCounts[i]=(int) Math.round(inputLinks[i].getTotalDensityInVeh(0));
//        }
//		for(int j=0;j<nOutputs;j++){
//            inputCounts[j]=(int) Math.round(outputLinks[j].getTotalDensityInVeh(0));
//        }
//
//		// get splits from node
//        double[][] splits = new double[nInputs][nOutputs];
//		for(int i=0;i<nInputs;i++){
//			for(int j=0;j<nOutputs;j++){
//				splits[i][j]=myNode.getSplitRatio(i, j, 0);
//			}
//		}
//
//        //calculate SIMPLE weights
//        //later this will be changed, to calculate max/min/average/etc weights, and to include minimum green time constraints.
//        double[] weights = new double [nInputs];
//        for (int i=0; i<nInputs; i++){
//        	weights[i]=inputCounts[i];
//        	for (int e=0; e<nOutputs; e++){
//        		weights[i]-=splits[i][e]*outputCounts[e];
//        	}
//        }
//
//        //calculate pressure for all stages
//        double[] pressures = new double [nStages];
//        for (int s=0; s<nStages;s++){
//        	pressures[s] = 0;
//        	for (int i=0; i<nInputs; i++){
//        		pressures[s] += controlMat[s][i]*weights[i]*satFlows[i];
//        	}
//        }
//
//        //determine max pressure stage
//        int mpStage = 0;
//        for (int s=1;s<nStages;s++){
//        	if (pressures[s]>pressures[mpStage]){mpStage = s;}
//        }
//
//      //calculate green_splits for signal cycle
//        double flexTime = 0;
//        for (double m : minGreens)
//        	flexTime += m;
//        flexTime = cycle_time - flexTime;
//        green_splits = new double[nStages];
//        for (int s=0; s<nStages;s++){
//        	int isMP =  (s==mpStage)? 1 : 0;
//        	green_splits[s]= minGreens[s]/cycle_time + (flexTime*isMP);
//        }
//
//	}
//}
