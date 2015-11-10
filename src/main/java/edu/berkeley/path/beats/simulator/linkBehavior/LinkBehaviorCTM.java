package edu.berkeley.path.beats.simulator.linkBehavior;

import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

/**
 * Created by gomes on 3/12/14.
 */
public class LinkBehaviorCTM implements Serializable {

    private static final long serialVersionUID = 4172754141077034414L;

    public Link myLink;
    public Scenario myScenario;
    public Double [][] density;                  // [veh] numEnsemble x numVehTypes
    public Double [] total_space_supply;         // [veh]	numEnsemble (typically njam-n)
    public Double [] available_space_supply;     // [veh]	numEnsemble (typically min(w*(njam-n),F))
    public Double [][] flow_demand;              // [veh] 	numEnsemble x numVehTypes (typically min(vn,F,c))

    public LinkBehaviorCTM(Link link){
        this.myLink = link;
        this.myScenario = myLink.getMyScenario();
    }

    public void reset(Double [] initial_density) {
        int n1 = myScenario.get.numEnsemble();
        int n2 = myScenario.get.numVehicleTypes();
        flow_demand = BeatsMath.zeros(n1, n2);
        total_space_supply = BeatsMath.zeros(n1);
        available_space_supply = BeatsMath.zeros(n1);
        reset_density();
        for(int e=0;e<n1;e++)
            set_density_in_veh(e,initial_density);
    }

    public void update_outflow_demand(double external_max_speed, double external_max_flow){

        int numVehicleTypes = myScenario.get.numVehicleTypes();

        double totaldensity;
        double totaloutflow;
        FundamentalDiagram FD;

        for(int e=0;e<myScenario.get.numEnsemble();e++){

            FD = myLink.currentFD(e);

            totaldensity = myLink.getTotalDensityInVeh(e);

            // case empty link
            if( BeatsMath.lessorequalthan(totaldensity,0d) ){
                flow_demand[e] =  BeatsMath.zeros(numVehicleTypes);
                continue;
            }

            // compute total flow leaving the link in the absence of flow control
            boolean apply_vertical_onramp_model = false;
            if(apply_vertical_onramp_model && myLink.issource){
                totaloutflow = Math.min( totaldensity , FD._getCapacityInVeh() );
            }
            else {
                if( totaldensity < FD.getDensityCriticalInVeh() )
                    totaloutflow = totaldensity * Math.min(FD.getVfNormalized(),external_max_speed);
                else{
                    totaloutflow = Math.max(FD._getCapacityInVeh()-FD._getCapacityDropInVeh(),0d);
                    totaloutflow = Math.min(totaloutflow,external_max_speed*FD.getDensityCriticalInVeh());
                }
            }

            // capacity profile
            if(myLink.myCapacityProfile!=null)
                totaloutflow = Math.min( totaloutflow , myLink.myCapacityProfile.getCurrentValue() );

            // flow controller
            totaloutflow = Math.min( totaloutflow , external_max_flow );

            // flow uncertainty model (unless controller wants zero flow)
            if(myScenario.get.has_flow_unceratinty() && BeatsMath.greaterthan(external_max_flow,0d) ){

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
            if(myScenario.get.numVehicleTypes()==1)
                flow_demand[e][0] = totaloutflow;
            else{
                double alpha = totaloutflow/totaldensity;
                for(int j=0;j<myScenario.get.numVehicleTypes();j++)
                    flow_demand[e][j] = get_density_in_veh(e, j)*alpha;
            }

        }

        return;
    }

    public void update_total_space_supply(){

        if(myLink.issink) {
            for (int e = 0; e < myScenario.get.numEnsemble(); e++)
                total_space_supply[e] = Double.POSITIVE_INFINITY;
            return;
        }
        for(int e=0;e<myScenario.get.numEnsemble();e++)
            total_space_supply[e] = myLink.currentFD(e)._getDensityJamInVeh() - myLink.getTotalDensityInVeh(e);
    }

    public void update_available_space_supply(){
        for(int e=0;e<myScenario.get.numEnsemble();e++){
            FundamentalDiagram FD = myLink.currentFD(e);

            if(myLink.issink)
                available_space_supply[e] = FD._getCapacityInVeh();
            else
                available_space_supply[e] = Math.min( FD.getWNormalized()*total_space_supply[e] , FD._getCapacityInVeh() );

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
                available_space_supply[e] = Math.max( 0d , available_space_supply[e] + delta_flow );
                available_space_supply[e] = Math.min( available_space_supply[e] , total_space_supply[e] );
            }
        }
    }

    protected void reset_density(){
        int n1 = myScenario.get.numEnsemble();
        int n2 = myScenario.get.numVehicleTypes();
        density = BeatsMath.zeros(n1,n2);
    }

    // UPDATE

    public void update_state(Double [][] inflow,Double [][] outflow){
        int e,j;
        for(e=0;e<myScenario.get.numEnsemble();e++)
            for(j=0;j<myScenario.get.numVehicleTypes();j++)
                density[e][j] += inflow[e][j] - outflow[e][j];
    }

    // GET / SET / RESET DENSITY

    public double get_density_in_veh(int ensemble_index, int vehicletype_index) throws IndexOutOfBoundsException {
        return density[ensemble_index][vehicletype_index];
    }

    public boolean set_density_in_veh(int e,Double [] d){
        if(density==null)
            return false;
        if(e<0 || e>=density.length)
            return false;
        if(d.length!=myScenario.get.numVehicleTypes())
            return false;
        if(!BeatsMath.all_non_negative(d))
            return false;
        for(int v=0;v<d.length;v++)
            density[e][v] = d[v];
        return true;
    }

    // COMPUTE

    public double compute_speed_in_mps(int ensemble){
        try{
            if(myScenario.get.clock().getRelativeTimeStep()==0)
                return Double.NaN;
            double totaldensity = BeatsMath.sum(density[ensemble]);
            double speed = BeatsMath.greaterthan(totaldensity,0d) ?
                    BeatsMath.sum(myLink.outflow[ensemble])/totaldensity :
                    myLink.currentFD(ensemble).getVfNormalized();
            return speed * myLink._length / myScenario.get.simdtinseconds();
        } catch(Exception e){
            return Double.NaN;
        }
    }

    public double compute_delay_in_veh(int ensemble, int vt_index){
        double n = get_density_in_veh(ensemble, vt_index);
        double f = myLink.getOutflowInVeh(ensemble, vt_index);
        double vf = myLink.getNormalizedVf(ensemble);
        return Math.max(0d,vf*n-f);
    }

}
