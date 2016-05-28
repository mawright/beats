package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.control.ControllerRampMeteringAPI;
import edu.berkeley.path.beats.control.Controller_SR_Generator_new;
import edu.berkeley.path.beats.jaxb.Demand;
import edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacitySet;
import edu.berkeley.path.beats.jaxb.Splitratio;
import edu.berkeley.path.beats.jaxb.VehicleType;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsFormatter;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.ArrayList;

public class ScenarioSetApi implements Serializable {

    private static final long serialVersionUID = -8397613472604860318L;

    Scenario scenario;

    public ScenarioSetApi(Scenario scenario){
        this.scenario = scenario;
    }

    /* SIMULATION PARAMETERS ------------------------------------------ */

    public void configfilename(String configfilename) {
        scenario.configfilename = configfilename;
    }

    public void end_time(double maxt){
        this.scenario.clock.set_end_time(maxt);
    }

    public void output_dt(double dt){
        // make a clone of runParam
        Scenario.RunParameters runParam2 = this.scenario.runParam.clone();
        runParam2.dt_output = dt;
        runParam2.outsteps = BeatsMath.round(dt/runParam2.dt_sim);
        try {
            runParam2.validate();
        } catch (BeatsException e) {
            System.out.println("set.output_dt failed.");
            e.printStackTrace();
            return;
        }
        this.scenario.runParam = runParam2;
    }
//    // set the clock to a specific time
//    public void timeInSeconds(int timeInSecs) throws BeatsException {
//
//        if(!BeatsMath.isintegermultipleof((double) timeInSecs,scenario.runParam.dt_sim))
//            throw new BeatsException("nsec (" + timeInSecs + ") must be an interger multiple of simulation dt (" + runParam.dt_sim + ").");
//        int timestep = BeatsMath.round(timeInSecs/scenario.runParam.dt_sim);
//
//        try{
//            scenario.reset();
//
//            clock.setRelativeTimeStep(timestep);
//
//            if(downstreamBoundaryCapacitySet!=null)
//                for(edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacityProfile capacityProfile : downstreamBoundaryCapacitySet.getDownstreamBoundaryCapacityProfile())
//                    ((CapacityProfile) capacityProfile).update(true);
//
//            if(demandSet!=null){
//                for(edu.berkeley.path.beats.jaxb.DemandProfile dp : ((DemandSet) demandSet).getDemandProfile())
//                    ((DemandProfile) dp).update(true);
//
//                if(splitRatioSet!=null)
//                    for(edu.berkeley.path.beats.jaxb.SplitRatioProfile srp : ((SplitRatioSet) splitRatioSet).getSplitRatioProfile())
//                        ((SplitRatioProfile) srp).update(true);
//
//                if(fundamentalDiagramSet!=null)
//                    for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fdProfile : fundamentalDiagramSet.getFundamentalDiagramProfile())
//                        ((FundamentalDiagramProfile) fdProfile).update(true);
//
//            }
//        } catch( BeatsException bex){
//            bex.printStackTrace();
//            throw bex;
//        }
//    }

    /* PERFORMANCE ----------------------------------------------- */

    public void performance_calculator(PerformanceCalculator pcalc){
        scenario.perf_calc = pcalc;
    }

    /* LOG ------------------------------------------------------ */

    public void splitLoggerPrefix(String split_logger_prefix){
        scenario.split_logger_prefix = split_logger_prefix;
    }

    public void splitLoggerDt(Double split_logger_dt){
        scenario.split_logger_dt = split_logger_dt;
    }

    public void global_control_on(boolean global_control_on) {
        for(Controller c : scenario.controllerset.get_Controllers())
            c.setIson(global_control_on);
    }

    /* DEMAND ------------------------------------------------------ */

    public void global_demand_knob(double global_demand_knob) {
        scenario.global_demand_knob = global_demand_knob;
    }

    public void demand_for_link_si(long link_id, double dt, double[] demands) throws Exception{
        //public void set_demand_for_link_si(long link_id, double dt, HashMap<Long, double[]> demands) throws Exception{

        if(demands.length<=1)
            dt = Double.POSITIVE_INFINITY;

        // put the given demands into a DemandProfile
        DemandProfile dp = new DemandProfile();
        dp.setLinkIdOrg(link_id);
        dp.setDt(dt);
        dp.setKnob(1d);
        dp.setStartTime(scenario.get.currentTimeInSeconds());

        double [] demand_per_vt = BeatsMath.times(demands, 1d / ((double) scenario.get.numVehicleTypes()));
        for(VehicleType vt : scenario.getVehicleTypeSet().getVehicleType()){
            Demand d = new Demand();
            d.setVehicleTypeId(vt.getId());
            d.setContent(BeatsFormatter.csv(demand_per_vt, ","));
            dp.getDemand().add(d);
        }

        // populate, validate, reset
        dp.populate(scenario);
        BeatsErrorLog.clearErrorMessage();
        dp.validate();
        if(BeatsErrorLog.haserror()){
            BeatsErrorLog.print();
            throw new Exception("Failed in set_demand_for_link_si()");
        }
        dp.reset();

        // check wheter I have a demandSet, otherwise create one
        if(scenario.getDemandSet()==null){
            scenario.setDemandSet(new DemandSet());
            ((DemandSet)scenario.getDemandSet()).populate(scenario);
        }

        // add the demand profile to the demand set
        ((DemandSet)scenario.getDemandSet()).add_or_replace_profile(dp);

    }

    public void demand_knob_for_link_id(long link_id,double newknob){
        DemandSet demandSet = (DemandSet) scenario.getDemandSet();
        if(demandSet==null)
            return;
        DemandProfile dp = demandSet.get_demand_profile_for_link_id(link_id);
        if(dp==null) {
            System.out.println("Did not find a demand profile for link " + link_id);
            return;
        }

        if(dp.isSinkDemand && scenario.runMode.compareTo(Scenario.RunMode.FRDEMANDS)==0 ){
            ArrayList<Controller> srcontrs = scenario.controllerset.getControllersOfType("Controller_SR_Generator_new");
            srcontrs.addAll(scenario.controllerset.getControllersOfType("Controller_SR_Generator_HOV_GeneralNode"));
            if(srcontrs==null || srcontrs.size()!=1)
                System.out.println("Error in demand_knob_for_link_id");
            Controller_SR_Generator_new contr = (Controller_SR_Generator_new) srcontrs.get(0);
            contr.set_knob_for_link(link_id,newknob);
        }
        else {
            dp.set_knob(newknob);
        }
    }

    /* SPLITS ------------------------------------------------------ */

    public void splits_for_node(double node_id,double start_time,double dt,edu.berkeley.path.beats.jaxb.Splitratio[] splitratios){

        JaxbObjectFactory jaxb = new JaxbObjectFactory();
        SplitRatioProfile srp = (SplitRatioProfile) jaxb.createSplitRatioProfile();

        srp.setNodeId((long)node_id);
        srp.setStartTime(start_time);
        srp.setDt(dt);
        srp.getSplitratio().addAll(java.util.Arrays.asList(splitratios));

        srp.populate(scenario);
        srp.validate();
        srp.reset();
        srp.update(true);

        SplitRatioSet srset = (SplitRatioSet) this.scenario.getSplitRatioSet();
        srset.set_split_profile_for_node_id(srp.getNodeId(),srp);
    }

    /* EVENTS ------------------------------------------------------ */

    public boolean event(Event E){
        if(scenario.scenario_locked)
            return false;
        if(E==null)
            return false;
        if(E.getMyType()==null)
            return false;

        // add event to list
        scenario.eventset.addEvent(E);
        return true;
    }

    /* FD ------------------------------------------------------ */

    public void capacity_for_sink_si(long link_id,double dt,double [] capacity) throws Exception {

        if(capacity.length<=1)
            dt = Double.POSITIVE_INFINITY;

        // put the given capacity into a profile
        CapacityProfile cp = new CapacityProfile();
        cp.setStartTime(scenario.get.currentTimeInSeconds());
        cp.setDt(dt);
        cp.setLinkId(link_id);
        cp.setContent(BeatsFormatter.csv(capacity,","));

        // populate, validate, reset
        cp.populate(scenario);

        // populate, validate, reset
        cp.populate(scenario);
        BeatsErrorLog.clearErrorMessage();
        cp.validate();
        if(BeatsErrorLog.haserror()){
            BeatsErrorLog.print();
            throw new Exception("Failed in set_demand_for_link_si()");
        }
        cp.reset();

        // check wheter I have a capacity, otherwise create one
        if(scenario.getDownstreamBoundaryCapacitySet()==null){
            DownstreamBoundaryCapacitySet d = new CapacitySet();
            scenario.setDownstreamBoundaryCapacitySet(d);
            ((CapacitySet)d).populate(scenario);
        }

        // add the capacity profile to the capacity set
        DownstreamBoundaryCapacitySet dbcp = scenario.getDownstreamBoundaryCapacitySet();
        ((CapacitySet)dbcp).add_or_replace_profile(cp);

    }

    /* STATE ------------------------------------------------------ */

    public void initial_state() throws BeatsException {

        // initial density set time stamp
        edu.berkeley.path.beats.jaxb.InitialDensitySet ids = scenario.getInitialDensitySet();
        double time_ic = ids!=null ? ids.getTstamp() : Double.POSITIVE_INFINITY;  // [sec]

        // determine the simulation mode and sim_start time
        double sim_start;
        TypeMode simulationMode;
        if(BeatsMath.equals(scenario.runParam.t_start_output,time_ic)){
            sim_start = scenario.runParam.t_start_output;
            simulationMode = TypeMode.on_init_dens;
        }
        else{
            // it is a warmup. we need to decide on start and end times
            if(BeatsMath.lessthan(time_ic, scenario.runParam.t_start_output) ){	// go from ic to timestart
                sim_start = time_ic;
                simulationMode = TypeMode.right_of_init_dens;
            }
            else{

                // find earliest demand profile ...
                double demand_start = Double.POSITIVE_INFINITY;
                if(scenario.getDemandSet()!=null)
                    for(edu.berkeley.path.beats.jaxb.DemandProfile D : scenario.getDemandSet().getDemandProfile())
                        demand_start = Math.min(demand_start,D.getStartTime());
                if(Double.isInfinite(demand_start))
                    demand_start = 0d;

                // ... start simulation there or at output start time
                sim_start = Math.min(scenario.runParam.t_start_output,demand_start);
                simulationMode = TypeMode.left_of_init_dens;

            }
        }

        // copy InitialDensityState to initial_state if starting from or to the right of InitialDensitySet time stamp
        if(simulationMode!=TypeMode.left_of_init_dens && ids!=null){
            for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworks())
                for(edu.berkeley.path.beats.jaxb.Link jlink:network.getLinkList().getLink()){
                    Double [] density = ((InitialDensitySet)ids).getDensityForLinkIdInVeh(network.getId(),jlink.getId());
                    if(density!=null)
                        ((Link) jlink).set_initial_state(density);
                    else
                        ((Link) jlink).set_initial_state(BeatsMath.zeros(scenario.numVehicleTypes));
                }
        }
        else {
            for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworks())
                for(edu.berkeley.path.beats.jaxb.Link jlink:network.getLinkList().getLink())
                    ((Link) jlink).set_initial_state(BeatsMath.zeros(scenario.numVehicleTypes));
        }

        // warmup

        // temporary warmup clock
        scenario.clock = new Clock(sim_start,scenario.runParam.t_end_output,scenario.runParam.dt_sim);

        // advance a point ensemble to start_output time
        int original_numEnsemble = scenario.runParam.numEnsemble;
        scenario.runParam.numEnsemble = 1;

        // reset the simulation (copy initial_density to density)
        scenario.reset();

        // advance to start of output time
        while( BeatsMath.lessthan(scenario.get.currentTimeInSeconds(),scenario.runParam.t_start_output) )
            scenario.updater.update();

        // copy the result to the initial density
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworks())
            for(edu.berkeley.path.beats.jaxb.Link link:network.getLinkList().getLink())
                ((Link) link).copy_state_to_initial_state();

        // revert numEnsemble
        scenario.runParam.numEnsemble = original_numEnsemble;

        // delete the warmup clock
        scenario.clock = null;

    }

    // set density in [veh] indexed by [link][ensemble]
    public boolean totalDensity(double [][] d){

        if(scenario.getNetworks().size()>1){
            System.err.println("This method works only with single network scenarios.");
            return false;
        }

        if(scenario.get.numVehicleTypes()>1){
            System.err.println("This method works only with single vehicle type scenarios.");
            return false;
        }

        Network network = (Network) scenario.getNetworks().get(0);
        int numLinks = network.getLinkList().getLink().size();

        if(numLinks!=d.length){
            System.err.println("The 1st dimension of the input should equal the number of links in the scenario.");
            return false;
        }

        if(scenario.get.numEnsemble()!=d[0].length){
            System.err.println("The 2nd dimension of the input should equal the number of links in the scenario.");
            return false;
        }

        int i,e;
        boolean success = true;
        for(i=0;i<numLinks;i++)
            for(e=0;e<scenario.get.numEnsemble();e++){
                Double [] val = new Double[1];
                val[0] = d[i][e];
                success &= ((Link)network.getLinkList().getLink().get(i)).set_density_in_veh(e,val);
            }
        return success;
    }

    public void uncertaintyModel(String uncertaintyModel) {
        scenario.uncertaintyModel = uncertaintyModel==null ? TypeUncertainty.gaussian : TypeUncertainty.valueOf(uncertaintyModel);
    }

    /* CONTROL ------------------------------------------------------ */
    public void ramp_metering_rate(int controller_id,int act_id,double rate_in_vph){
        Controller controller = scenario.controllerset.getControllerWithId(controller_id);
        if(controller==null)
            return;
        if(controller.getMyType()!= Controller.Algorithm.RMAPI)
            return;
        ((ControllerRampMeteringAPI)controller).set_metering_rate_in_vph(act_id,rate_in_vph);
    }

}
