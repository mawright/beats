package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.actuator.ActuatorSignal;
import edu.berkeley.path.beats.actuator.NEMA;
import edu.berkeley.path.beats.jaxb.OutputRequest;
import edu.berkeley.path.beats.jaxb.SimulationOutput;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PerformanceCalculator {

    protected OutputRequest output_request;
    protected enum Quantity { veh_time,
                              veh_distance,
                              delay,
                              travel_time,
                              speed_contour,
                              signal_events };
    protected Scenario myScenario;
    protected List<Logger> loggers = new ArrayList<Logger>();

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public PerformanceCalculator(OutputRequest output_request){
        this.output_request = output_request;
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset / update
    /////////////////////////////////////////////////////////////////////

    public void populate(Scenario myScenario){

        this.myScenario = myScenario;
        for(SimulationOutput sim_out : output_request.getSimulationOutput()){
            Quantity cpm = Quantity.valueOf(sim_out.getQuantity());
            switch(cpm){
                case delay:
                case veh_distance:
                case veh_time:
                case speed_contour :
                    loggers.add( new CumulativeMeasure(
                                    myScenario,
                                    myScenario.getOutputPrefix() + "_" + sim_out.getFile(),
                                    sim_out.getDt(),
                                    sim_out.isAggTime(),
                                    sim_out.isAggLinks(),
                                    sim_out.isAggEnsemble(),
                                    sim_out.isAggVehicleType(),
                                    myScenario.getRouteWithId(sim_out.getRouteId()),
                                    myScenario.getVehicleTypeIndexForId(sim_out.getVehicleTypeId()),
                                    cpm));
                    break;
                case travel_time:
                    break;
                case signal_events:
                    loggers.add( new SignalLogger( myScenario,
                                                   myScenario.getOutputPrefix() + "_" + sim_out.getFile(),
                                                   sim_out.getDt(),
                                                   sim_out.isAggTime() ) );
            }
        }
    }

    protected void validate() {
        for(Logger log : loggers)
            log.validate();
    }

    public void reset() {
        for(Logger log : loggers)
            log.reset();
    }

    protected void update(){
        for(Logger log : loggers){
            log.update();
            if(!log.agg_time && myScenario.getClock().getRelativeTimeStep()%log.dt_steps==0)
                log.write_output(myScenario.getClock().getT());
        }
    }

    protected void close_output(){
        for(Logger log : loggers){
            if(log.agg_time)
                log.write_output(myScenario.getClock().getT());
            log.close_output_file();
        }
    }

    /////////////////////////////////////////////////////////////////////
    // inner class
    /////////////////////////////////////////////////////////////////////

    protected abstract class Logger {

        protected boolean agg_time;
        protected int dt_steps;
        protected int num_sample;
        protected double sim_dt;
        protected String filename;
        protected Writer filewriter;
        protected Scenario myScenario;

        public Logger(boolean agg_t,String fname,Scenario myScenario,Double out_dt){

            this.myScenario = myScenario;
            agg_time = agg_t;

            // file
            if(fname.contains("."))
                fname = fname.split("\\.")[0];
            filename = fname+".txt";

            // dt
            sim_dt = myScenario.getSimdtinseconds();
            dt_steps = out_dt==null?1:BeatsMath.round(out_dt/sim_dt);
        }

        protected final void open_output_file() {
            if(filewriter!=null)
                return;
            try{
                filewriter = new FileWriter(new File(filename));
            }
            catch(IOException e){
                // DO SOMETHING?
                filewriter = null;
            }
        }

        protected final void close_output_file(){
            try{
                filewriter.close();
            }
            catch(IOException e){
                // DO SOMETHING?
            }
            finally{
                filewriter = null;
            }
        }

        protected final void write_output(double time){
            try{
                for(String str : make_output_string(time))
                    filewriter.write(str+"\n");
            }
            catch(IOException e)
            {
                System.out.println("Unable to write");
            }
            reset_value();
        }

        protected final void reset(){
            open_output_file();
            reset_value();
        }

        protected abstract ArrayList<String> make_output_string(double time);

        protected abstract void reset_value();

        protected abstract void validate();

        protected abstract void update();
    }

    protected class CumulativeMeasure extends Logger {

        // value.get(k)[e][l][v] is pm for time k, link l, vehicle type v,ensemble e.
        protected double [][] value;
        protected Quantity pm;

        // flags for aggregating the various dimensions
        protected boolean agg_links;
        protected boolean agg_ensemble;
        protected boolean agg_vehicle_type;

        // vehicle type index. negative => aggregate over all
        protected int vt_index;

        // dimension of the stored data
        protected int nE;       // ensemble dimension
        protected int nL;       // link dimension

        protected int num_ensemble;

        // list of links to include
        protected List<Link> link_list;
        protected boolean isroute;

        public CumulativeMeasure(Scenario scenario,String fname,Double out_dt,boolean agg_t,boolean agg_l,boolean agg_e,boolean agg_vt,Route route,int vehicle_type_index,Quantity pmname){

            super(agg_t,fname,scenario,out_dt);

            agg_links = agg_l;
            agg_ensemble = agg_e;
            agg_vehicle_type = agg_vt;
            pm = pmname;
            num_ensemble = scenario.getNumEnsemble();
            vt_index = vehicle_type_index;

            // special for speed contour
            if(pm== Quantity.speed_contour){
                agg_time = false;
                agg_links = false;
                agg_ensemble = false;
                agg_vehicle_type = true;
            }

            // links to consider
            link_list = new ArrayList<Link>();
            isroute = route!=null;
            if(isroute)
                for(Link link : route.ordered_links)
                    link_list.add(link);
            else
                for(edu.berkeley.path.beats.jaxb.Link link : scenario.getNetworkSet().getNetwork().get(0).getLinkList().getLink())
                    link_list.add((Link)link);

            // dimension variables
            nE = agg_ensemble ? 1 : num_ensemble;
            nL = agg_links ? 1 : link_list.size();
        }

        @Override
        protected ArrayList<String> make_output_string(double time){
            int ee,ii;
            switch(pm){
                case veh_time:
                case delay:
                    for(ee=0;ee<value.length;ee++)
                        for(ii=0;ii<value[ee].length;ii++)
                            value[ee][ii] *= sim_dt;
                    break;
                case speed_contour:
                    for(ee=0;ee<value.length;ee++)
                        for(ii=0;ii<value[ee].length;ii++)
                            value[ee][ii] /= num_sample;
                    break;
            }

            ArrayList<String> strlist = new ArrayList<String>();
            for(ee=0;ee<value.length;ee++){
                String str = String.format("%f\t%d",time,ee);
                for(ii=0;ii<value[ee].length;ii++)
                    str += String.format("\t%f",value[ee][ii]);
                strlist.add(str);
            }

            return strlist;

        }

        @Override
        protected void reset_value(){
            value = new double [nE][nL];
            num_sample = 0;
        }

        @Override
        protected void validate(){

            if(!agg_vehicle_type && vt_index<0)
                BeatsErrorLog.addError("Performance calculator: Please specify a vehicle type.");

            // speed contours require a route
            if(pm== Quantity.speed_contour && !isroute)
                BeatsErrorLog.addError("Performance calculator: Please specify a route for the speed contour.");

        }

        @Override
        protected void update(){

            int i,e;
            int ii,ee;

            // dimensions of the saved data
            double [][] X = new double[nE][nL];

            // gather information
            for(i=0;i<link_list.size();i++){
                Link link = link_list.get(i);
                ii = agg_links ? 0 : i;
                for(e=0;e<num_ensemble;e++){
                    ee = agg_ensemble ? 0 : e;
                    switch(pm){
                        case veh_time:
                            X[ee][ii] += agg_vehicle_type ? link.getTotalDensityInVeh(e) :
                                                           link.getDensityInVeh(e,vt_index);                            break;
                        case veh_distance:
                            X[ee][ii] += ( agg_vehicle_type ? link.getTotalOutflowInVeh(e) :
                                                           link.getOutflowInVeh(e,vt_index) ) *
                                            link.getLengthInMeters();
                            break;
                        case delay:
                            X[ee][ii] += agg_vehicle_type ? link.computeTotalDelayInVeh(e) :
                                                           link.computeDelayInVeh(e,vt_index);
                            break;
                        case speed_contour:
                            X[ee][ii] = link.computeSpeedInMPS(e);
                            break;
                    }
                }
            }

            // do mean if agg_ensemble
            if(agg_ensemble){
                if(agg_links)
                    X[0][0] /= num_ensemble;
                else{
                    for(i=0;i<link_list.size();i++){
                        ii = agg_links ? 0 : i;
                        X[0][ii] /=  num_ensemble;
                    }
                }
            }

            // add to running aggregate
            for(ii=0;ii<nL;ii++)
                for(ee=0;ee<nE;ee++)
                    value[ee][ii] += X[ee][ii];
            num_sample++;

        }
    }

    public class SignalLogger extends Logger {

        private List<SignalEvent> signal_events = new ArrayList<SignalEvent>();

        public SignalLogger(Scenario scenario,String fname,Double out_dt,boolean agg_t){
            super(agg_t,fname,scenario,out_dt);
            for( Actuator actuator : scenario.get_signal_actuators())
                ((ActuatorSignal) actuator).register_event_logger(this);
        }

        public void send_event(long signal_id,NEMA.ID nema, ActuatorSignal.BulbColor bulbcolor){
            double timestamp = myScenario.getCurrentTimeInSeconds();
            signal_events.add(new SignalEvent(timestamp,signal_id,nema,bulbcolor));
        }

        @Override
        protected ArrayList<String> make_output_string(double time){
            ArrayList<String> strlist = new ArrayList<String>();
            for(SignalEvent signal_event : signal_events)
                strlist.add(signal_event.toString());
            return strlist;
        }

        @Override
        protected void reset_value(){
            signal_events.clear();
        }

        @Override
        protected void validate(){};

        @Override
        protected void update(){};
    }

    public class SignalEvent {
        private double timestamp;
        private long signal_id;
        private NEMA.ID nema;
        private ActuatorSignal.BulbColor bulbcolor;
        public SignalEvent(double timestamp,long signal_id,NEMA.ID nema, ActuatorSignal.BulbColor bulbcolor){
            this.timestamp = timestamp;
            this.signal_id = signal_id;
            this.nema = nema;
            this.bulbcolor = bulbcolor;
        }
        @Override
        public String toString() {
            return String.format("%.2f\t%d\t%d\t%d",timestamp,
                    signal_id,
                    NEMA.nema_to_int(nema),
                    ActuatorSignal.color_to_int(bulbcolor));
        }

    }

}
