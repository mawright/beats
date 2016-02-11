package edu.berkeley.path.beats.simulator.linkBehavior;

import edu.berkeley.path.beats.simulator.Defaults;
import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 3/13/14.
 */
public class LinkBehaviorQueueAndTravelTime extends LinkBehaviorCTM implements Serializable {

    private static final long serialVersionUID = -5202026708817181880L;

    protected List<CellArrayAndQueue> ensemble;     // ensemble of cell arrays

    public LinkBehaviorQueueAndTravelTime(Link link){
        super(link);

        int num_ensemble = myScenario.get.numEnsemble();

        double L = myLink.getLengthInMeters();

        edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fdp = myScenario.get.FDprofileForLinkId(myLink.getId());
        double vf = fdp==null ? Defaults.vf : fdp.getFundamentalDiagram().get(0).getFreeFlowSpeed();
        double dt = myLink.getMyNetwork().getMyScenario().get.simdtinseconds();

        int num_cells = (int) Math.ceil(L/(vf*dt));
        ensemble = new ArrayList<CellArrayAndQueue>();
        for(int e=0;e<num_ensemble;e++)
            ensemble.add(new CellArrayAndQueue(num_cells,myScenario.get.numVehicleTypes()));
    }

    // UPDATE

    @Override
    public void update_state(Double[][] inflow, Double[][] outflow) {
        for(int e=0;e<ensemble.size();e++)
            ensemble.get(e).update_state(inflow[e],outflow[e]);
    }

    @Override
    public void update_outflow_demand(double external_max_speed, double external_max_flow) {

        int numVehicleTypes = myScenario.get.numVehicleTypes();

        double totaloutflow;
        FundamentalDiagram FD;

        for(int e=0;e<ensemble.size();e++){

            CellArrayAndQueue caq = ensemble.get(e);
            FD = myLink.currentFD(e);

            double queue_length = caq.get_total_queue_veh();

            // case empty link
            if( BeatsMath.lessorequalthan(queue_length, 0d) ){
                flow_demand[e] =  BeatsMath.zeros(numVehicleTypes);
                continue;
            }

            // compute total flow leaving the link in the absence of flow control
            totaloutflow = Math.min(queue_length,FD._getCapacityInVeh());

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
                totaloutflow = Math.min( totaloutflow , queue_length );
            }

            // split among types
            double alpha = totaloutflow/queue_length;
            for(int j=0;j<myScenario.get.numVehicleTypes();j++)
                flow_demand[e][j] = caq.get_queue_for_vehicletype_in_veh(j)*alpha;

        }

    }

    // identical to simple queueing code
    @Override
    public void update_total_space_supply() {

        double totaldensity;
        FundamentalDiagram FD;
        for(int e=0;e<ensemble.size();e++){
            FD = myLink.currentFD(e);
            totaldensity = myLink.getTotalDensityInVeh(e);
            total_space_supply[e] = FD._getDensityJamInVeh()-totaldensity;

//            total_space_supply[e] = Math.min(FD._getDensityJamInVeh()-totaldensity,FD._getCapacityInVeh());

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
        }
    }

    // GET / SET / RESET DENSITY

    @Override
    public double get_density_in_veh(int ensemble_index, int vehicletype_index) throws IndexOutOfBoundsException {
        return ensemble.get(ensemble_index).get_density_for_vehicletype_in_veh(vehicletype_index);
    }

    @Override
    public boolean set_density_in_veh(int e, Double[] d) {
        if(e<0 || e>=ensemble.size())
            return false;
        if(d.length!=myScenario.get.numVehicleTypes())
            return false;
        if(!BeatsMath.all_non_negative(d))
            return false;
        for(int v=0;v<d.length;v++)
            ensemble.get(e).set_density_in_veh(d);
        return true;
    }

    @Override
    protected void reset_density() {
        for(CellArrayAndQueue caq : ensemble)
            caq.reset();
    }

    // COMPUTE

    @Override
    public double compute_speed_in_mps(int e) {
        CellArrayAndQueue caq = ensemble.get(e);
        double queue_veh = caq.get_total_queue_veh();
        double queue_speed = BeatsMath.greaterthan( BeatsMath.sum(myLink.outflow[e]) , 0d ) ? 1d : 0d;
        double queue_speed_times_veh = queue_speed * queue_veh;
        double tt_veh = caq.get_total_tt_veh();
        double tt_speed_time_veh = tt_veh;
        double total_veh = queue_veh + tt_veh;
        double avg_norm_speed = BeatsMath.greaterthan(total_veh,0d) ?
                                (queue_speed_times_veh+tt_speed_time_veh)/total_veh :
                                1d;
        return avg_norm_speed * myLink._length / myScenario.get.simdtinseconds();
    }

    @Override
    public double compute_delay_in_veh(int e, int vt_index) {
        double n = ensemble.get(e).get_queue_for_vehicletype_in_veh(vt_index);
        double f = myLink.getOutflowInVeh(e, vt_index);
        return Math.max(0d,n-f);
    }

    /////////////////////////////////////////////////////////////////////
    // Cell Array class
    /////////////////////////////////////////////////////////////////////

    // vehicles travel from the end of the cell_array to the begining
    private class CellArrayAndQueue implements Serializable {
        private static final long serialVersionUID = -7238499783874925536L;

        public Double [] queue;
        public ArrayList<Cell> cell_array;
        public CellArrayAndQueue(int numcell, int num_veh_types){
            queue = BeatsMath.zeros(num_veh_types);
            cell_array = new ArrayList<Cell>();
            for(int i=0;i<numcell;i++)
                cell_array.add(new Cell(num_veh_types));
        }

        // x is an array over vehicle types
        public boolean set_density_in_veh(Double [] x){
            queue = BeatsMath.copy(x);
            for(Cell cell : cell_array)
                cell.reset();
            return true;
        }
        protected double get_total_tt_veh(){
            double val = 0d;
            for(Cell cell : cell_array)
                val += BeatsMath.sum(cell.n);
            return val;
        }
        protected double get_queue_for_vehicletype_in_veh(int vtind){
            return queue[vtind];
        }
        protected double get_density_for_vehicletype_in_veh(int vtind){
            double val = queue[vtind];
            for(Cell cell : cell_array)
                val += cell.n[vtind];
            return val;
        }
        protected void reset(){
            int nVT = queue.length;
            queue = BeatsMath.zeros(nVT);
            for(Cell cell : cell_array)
                cell.reset();
        }
        protected void update_state(Double[] inflow, Double[] outflow){

            // update the queue
            Double [] flow_into_queue = cell_array.get(0).n;
            for(int j=0;j<queue.length;j++)
               queue[j] += flow_into_queue[j] - outflow[j];

            // shift the cell array
            cell_array.add(new Cell(inflow));
            cell_array.remove(0);
        }

        protected double get_total_queue_veh(){
            return BeatsMath.sum(queue);
        }

        @Override
        public String toString() {
            String str = String.format("%.2f",queue[0]);
            for(Cell cell:cell_array)
                str += String.format("\t%.2f",cell.n[0]);
            return str;
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Cell class
    /////////////////////////////////////////////////////////////////////

    private class Cell implements Serializable{
        private static final long serialVersionUID = 1648787740270540440L;
        public Double [] n;        // [ve] for each vehicle type
        public Cell(int nVT){
            n=BeatsMath.zeros(nVT);
        }
        public Cell(Double [] no){
            n = new Double[no.length];
            for(int v=0;v<no.length;v++)
                n[v]=no[v];
        }
        protected void reset(){
            int nVT = n.length;
            n = BeatsMath.zeros(nVT);
        }
    }

}
