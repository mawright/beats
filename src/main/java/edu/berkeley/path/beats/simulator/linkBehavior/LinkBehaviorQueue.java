package edu.berkeley.path.beats.simulator.linkBehavior;

import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

/**
 * Created by gomes on 3/12/14.
 */
public class LinkBehaviorQueue extends LinkBehaviorCTM implements Serializable {

    private static final long serialVersionUID = -4492099505782736550L;

    public LinkBehaviorQueue(Link link){
        super(link);
    }

    /////////////////////////////////////////////////////////////////////
    // LinkBehaviorInterface
    /////////////////////////////////////////////////////////////////////

    // UPDATE

    // same as ctm case with vf = 1
    @Override
    public void update_outflow_demand(double external_max_speed, double external_max_flow){

        int numVehicleTypes = myScenario.get.numVehicleTypes();

        double totaldensity;
        double totaloutflow;
        FundamentalDiagram FD;

        for(int e=0;e<myScenario.get.numEnsemble();e++){

            FD = myLink.currentFD(e);

            totaldensity = myLink.getTotalDensityInVeh(e);

            // case empty link
            if( BeatsMath.lessorequalthan(totaldensity, 0d) ){
                flow_demand[e] =  BeatsMath.zeros(numVehicleTypes);
                continue;
            }

            // compute total flow leaving the link in the absence of flow control
            totaloutflow = Math.min(totaldensity*Math.min(1d,external_max_speed),FD._getCapacityInVeh());

            // capacity profile
            if(myLink.myCapacityProfile!=null)
                totaloutflow = Math.min( totaloutflow , myLink.myCapacityProfile.getCurrentValue() );

            // flow controller
            totaloutflow = Math.min( totaloutflow , external_max_flow );

            // flow uncertainty model
            if(myScenario.get.has_flow_unceratinty()){

                double delta_flow=0.0;
                double std_dev_flow = myScenario.get.std_dev_flow();

                switch(myScenario.get.uncertaintyModel()){
                    case uniform:
                        delta_flow = BeatsMath.sampleZeroMeanUniform(std_dev_flow);
                        break;

                    case gaussian:
                        delta_flow = BeatsMath.sampleZeroMeanGaussian(std_dev_flow);
                        break;
                }

                totaloutflow = Math.max( 0d , totaloutflow + delta_flow );
                totaloutflow = Math.min( totaloutflow , totaldensity );
            }

            // split among types
            double alpha = totaloutflow/totaldensity;
            for(int j=0;j<myScenario.get.numVehicleTypes();j++)
                flow_demand[e][j] = get_density_in_veh(e, j)*alpha;

        }

        return;
    }

    // same as ctm case with w = 1
//    @Override
//    public void update_total_space_supply(){
//        double totaldensity;
//        FundamentalDiagram FD;
//        for(int e=0;e<myScenario.getNumEnsemble();e++){
//            FD = myLink.currentFD(e);
//            totaldensity = myLink.getTotalDensityInVeh(e);
//            total_space_supply[e] = Math.min(FD._getDensityJamInVeh()-totaldensity,FD._getCapacityInVeh());
//
//            // flow uncertainty model
//            if(myScenario.isHas_flow_unceratinty()){
//                double delta_flow=0.0;
//                double std_dev_flow = myScenario.getStd_dev_flow();
//                switch(myScenario.getUncertaintyModel()){
//                    case uniform:
//                        delta_flow = BeatsMath.sampleZeroMeanUniform(std_dev_flow);
//                        break;
//
//                    case gaussian:
//                        delta_flow = BeatsMath.sampleZeroMeanGaussian(std_dev_flow);
//                        break;
//                }
//                total_space_supply[e] = Math.max( 0d , total_space_supply[e] + delta_flow );
//                total_space_supply[e] = Math.min( total_space_supply[e] , FD._getDensityJamInVeh() - totaldensity);
//            }
//        }
//    }

    // COMPUTE

    // same as ctm with vf = 1
    @Override
    public double compute_speed_in_mps(int ensemble){
        try{
            if(myScenario.get.clock().getRelativeTimeStep()==0)
                return Double.NaN;
            double totaldensity = BeatsMath.sum(density[ensemble]);
            double speed = BeatsMath.greaterthan(totaldensity,0d) ?
                           BeatsMath.sum(myLink.outflow[ensemble])/totaldensity : 1d;
            return speed * myLink._length / myScenario.get.simdtinseconds();
        } catch(Exception e){
            return Double.NaN;
        }
    }

    // same as ctm with vf = 1
    @Override
    public double compute_delay_in_veh(int ensemble, int vt_index){
        double n = get_density_in_veh(ensemble, vt_index);
        double f = myLink.getOutflowInVeh(ensemble, vt_index);
        return Math.max(0d,n-f);
    }

}
