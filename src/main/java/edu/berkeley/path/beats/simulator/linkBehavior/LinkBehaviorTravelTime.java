package edu.berkeley.path.beats.simulator.linkBehavior;

import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 3/12/14.
 */
public class LinkBehaviorTravelTime extends LinkBehaviorCTM implements Serializable {

    private static final long serialVersionUID = 3718087443932943108L;
    protected List<CellArray> ensemble;     // ensemble of cell arrays

    public LinkBehaviorTravelTime(Link link){
        super(link);

        Scenario scenario = link.getMyScenario();
        int num_ensemble = scenario.get.numEnsemble();

        double L = link.getLengthInMeters();
        double vf = link.getVfInMPS(0);
        double dt = link.getMyNetwork().getMyScenario().get.simdtinseconds();

        int num_cells = (int) Math.ceil(L/(vf*dt));
        ensemble = new ArrayList<CellArray>();
        for(int e=0;e<num_ensemble;e++)
            ensemble.add(new CellArray(num_cells,scenario.get.numVehicleTypes()));
    }

    /////////////////////////////////////////////////////////////////////
    // LinkBehaviorInterface
    /////////////////////////////////////////////////////////////////////

    // UPDATE

    @Override
    public void update_state(Double [][] inflow,Double [][] outflow){
        for(int e=0;e<ensemble.size();e++)
            ensemble.get(e).update_state(inflow[e]);
    }

    @Override
    public void update_outflow_demand(double external_max_speed, double external_max_flow){
        for(int e=0;e<myScenario.get.numEnsemble();e++)
            flow_demand[e] = ensemble.get(e).cell_array.get(0).n;
    }

    @Override
    public void update_total_space_supply(){
//        double totaldensity;
//        FundamentalDiagram FD;
//        for(int e=0;e<myScenario.getNumEnsemble();e++){
//            FD = myLink.currentFD(e);
//            totaldensity = getTotalDensityInVeh(e);
//            spaceSupply[e] = FD.getWNormalized()*(FD._getDensityJamInVeh() - totaldensity);
//            spaceSupply[e] = Math.min(spaceSupply[e],FD._getCapacityInVeh());
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
//                spaceSupply[e] = Math.max( 0d , spaceSupply[e] + delta_flow );
//                spaceSupply[e] = Math.min( spaceSupply[e] , FD._getDensityJamInVeh() - totaldensity);
//            }
//        }
    }

    // GET / SET / RESET DENSITY

    @Override
    public double get_density_in_veh(int ensemble_index, int vehicletype_index) throws IndexOutOfBoundsException {
        return ensemble.get(ensemble_index).get_density_for_vehicletype_in_veh(vehicletype_index);
    }

    @Override
    public boolean set_density_in_veh(int e,Double [] d){
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
    public void reset_density() {
        for(CellArray ca : ensemble)
            ca.reset();
    }

    // COMPUTE

    @Override
    public double compute_speed_in_mps(int ensemble){
        return myLink.getVfInMPS(ensemble);
    }

    @Override
    public double compute_delay_in_veh(int e, int vt_index){
        return 0d;
    }

    /////////////////////////////////////////////////////////////////////
    // Cell Array class
    /////////////////////////////////////////////////////////////////////

    // vehicles travel from the end of the cell_array to the begining
    private class CellArray implements Serializable{
        private static final long serialVersionUID = 6814558847297173138L;
        public ArrayList<Cell> cell_array;
        public CellArray(int numcell,int num_veh_types){
            cell_array = new ArrayList<Cell>();
            for(int i=0;i<numcell;i++)
                cell_array.add(new Cell(num_veh_types));
        }

        // x is an array over vehicle types
        public boolean set_density_in_veh(Double [] x){
            if(cell_array.isEmpty())
                return false;
            Double [] x_per_cell = BeatsMath.times(x,1d/cell_array.size());
            for(Cell cell : cell_array)
                cell.set_vehicles(x_per_cell);
            return true;
        }
        protected double get_density_for_vehicletype_in_veh(int vtind){
            double val = 0d;
            for(Cell cell : cell_array)
                val += cell.get_density_for_vehicletype_in_veh(vtind);
            return val;
        }
        protected void reset(){
            for(Cell cell : cell_array)
                cell.reset();
        }
        protected void update_state(Double [] inflow){
            cell_array.add(new Cell(inflow));
            cell_array.remove(0);
        }

    }

    /////////////////////////////////////////////////////////////////////
    // Cell class
    /////////////////////////////////////////////////////////////////////

    private class Cell {
        public Double [] n;        // [ve] for each vehicle type
        public Cell(int nVT){
            n=BeatsMath.zeros(nVT);
        }
        public Cell(Double [] no){
            n = BeatsMath.zeros(no.length);
            for(int v=0;v<no.length;v++)
                n[v]=no[v];
        }
        protected void set_vehicles(Double [] x){
            for(int i=0;i<x.length;i++)
                n[i]=x[i];
        }
        protected double get_density_for_vehicletype_in_veh(int vtind){
            return n[vtind];
        }
        protected void reset(){
            int nVT = n.length;
            n = BeatsMath.zeros(nVT);
        }
    }

}
