/**
 * Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 **/

package edu.berkeley.path.beats.simulator;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import edu.berkeley.path.beats.Jaxb;
import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.output.OutputWriterBase;
import edu.berkeley.path.beats.simulator.output.OutputWriterFactory;
import edu.berkeley.path.beats.simulator.scenarioUpdate.ScenarioUpdaterACTM;
import edu.berkeley.path.beats.simulator.scenarioUpdate.ScenarioUpdaterAbstract;
import edu.berkeley.path.beats.simulator.scenarioUpdate.ScenarioUpdaterFrFlow;
import edu.berkeley.path.beats.simulator.scenarioUpdate.ScenarioUpdaterStandard;
import edu.berkeley.path.beats.simulator.utils.*;
import org.apache.log4j.Logger;

import edu.berkeley.path.beats.calibrator.FDCalibrator;
import edu.berkeley.path.beats.data.DataFileReader;
import edu.berkeley.path.beats.data.FiveMinuteData;
import edu.berkeley.path.beats.sensor.DataSource;
import edu.berkeley.path.beats.sensor.SensorLoopStation;

//@SuppressWarnings("restriction")
public class Scenario extends edu.berkeley.path.beats.jaxb.Scenario implements Serializable {

    private static final long serialVersionUID = 1840966378758409866L;

    public enum RunMode {NORMAL,ACTM,FRDEMANDS}
    protected String configfilename;
    public Clock clock;
    protected int numVehicleTypes;			// number of vehicle types

    // run parameters
    protected RunParameters runParam;
    public RunMode runMode;
    protected boolean initialized = false;
    protected boolean scenario_locked=false;				// true when the simulation is running

    // output
    public static Logger logger = Logger.getLogger(Scenario.class);
    protected String split_logger_prefix;
    protected Double split_logger_dt;
    public Cumulatives cumulatives;
    protected boolean started_writing;

    // performance calculator
    public PerformanceCalculator perf_calc;

    // demands
//    public DemandSet demandset = new DemandSet();
    protected double global_demand_knob;	// scale factor for all demands

    // sensors
    public SensorSet sensorset = new SensorSet();
    protected boolean sensor_data_loaded = false;

    // actuators
    public ActuatorSet actuatorset = new ActuatorSet();

    // controllers
    public ControllerSet controllerset = new ControllerSet();
    protected HashMap<String,Properties> aux_props;

    // events
    public EventSet eventset = new EventSet();	// holds time sorted list of events

    // scenario update
    protected ScenarioUpdaterAbstract updater;

	// Model uncertainty
	protected TypeUncertainty uncertaintyModel;
	protected double std_dev_flow = 0.0d;	// [veh]
	protected boolean has_flow_unceratinty;

    // api
    public ScenarioGetApi get;
    public ScenarioSetApi set;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate
	/////////////////////////////////////////////////////////////////////

	protected void populate() throws BeatsException {

	    // initialize scenario attributes ..............................................
		this.global_demand_knob = 1d;
		this.has_flow_unceratinty = BeatsMath.greaterthan(get.std_dev_flow(), 0.0);

		this.numVehicleTypes = 1;
		if(getVehicleTypeSet()!=null && getVehicleTypeSet().getVehicleType()!=null)
			numVehicleTypes = getVehicleTypeSet().getVehicleType().size();

		// network list
		if(networkSet!=null)
			for( edu.berkeley.path.beats.jaxb.Network network : networkSet.getNetwork() )
				((Network) network).populate(this);

		// sensors
		sensorset.populate(this);

		// signals
//		if(signalSet!=null)
//			for(edu.berkeley.path.beats.jaxb.Signal signal : signalSet.getSignal())
//				((ActuatorSignal) signal).populate(this);

        // actuators
        actuatorset.populate(this);

		// split ratio profile set (must follow network)
		if(splitRatioSet!=null)
			((SplitRatioSet) splitRatioSet).populate(this);

		// boundary capacities (must follow network)
		if(downstreamBoundaryCapacitySet!=null)
            ((CapacitySet)downstreamBoundaryCapacitySet).populate(this);
//			for( edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacityProfile capacityProfile : downstreamBoundaryCapacitySet.getDownstreamBoundaryCapacityProfile() )
//				((CapacityProfile) capacityProfile).populate(this);

		if(demandSet!=null)
			((DemandSet) demandSet).populate(this);
//        demandset.populate(this);

		// fundamental diagram profiles
		if(fundamentalDiagramSet!=null)
			for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fd : fundamentalDiagramSet.getFundamentalDiagramProfile())
				((FundamentalDiagramProfile) fd).populate(this);

		// initial density profile
		if(initialDensitySet!=null)
			((InitialDensitySet) initialDensitySet).populate(this);

        // routes
        if(routeSet!=null)
            for(edu.berkeley.path.beats.jaxb.Route route : routeSet.getRoute())
                ((Route) route).populate(this);

        // populate controllers
		controllerset.populate(this);

		// populate events
		eventset.populate(this);

		cumulatives = new Cumulatives(this);

        if(perf_calc!=null)
            perf_calc.populate(this);

        // create the updater
        updater.populate();

	}

	public static void validate(Scenario S) {

		if(S.get.simdtinseconds()<=0)
			BeatsErrorLog.addError("Non-positive simulation step size (" + S.get.simdtinseconds() + ").");

		// should have a network
		if(S.networkSet==null || S.networkSet.getNetwork().isEmpty())
			BeatsErrorLog.addError("Scenario does not contain a network.");

		// validate network
		if( S.networkSet!=null)
			for(edu.berkeley.path.beats.jaxb.Network network : S.networkSet.getNetwork())
				((Network)network).validate();

		// sensor list
		S.sensorset.validate();

		// validate actuators
		S.actuatorset.validate();

//		// signal list
//		if(S.signalSet!=null)
//			for (edu.berkeley.path.beats.jaxb.Signal signal : S.signalSet.getSignal())
//				((ActuatorSignal) signal).validate();

		// NOTE: DO THIS ONLY IF IT IS USED. IE DO IT IN THE RUN WITH CORRECT FUNDAMENTAL DIAGRAMS
		// validate initial density profile
//		if(getInitialDensityProfile()!=null)
//			((_InitialDensityProfile) getInitialDensityProfile()).validate();

		// validate capacity profiles
		if(S.downstreamBoundaryCapacitySet!=null)
            ((CapacitySet)S.downstreamBoundaryCapacitySet).validate();

		// validate demand profiles
		if(S.demandSet!=null)
			((DemandSet)S.demandSet).validate();
//        S.demandset.validate();

		// validate split ratio profiles
		if(S.splitRatioSet!=null)
			((SplitRatioSet)S.splitRatioSet).validate();

		// validate fundamental diagram profiles
		if(S.fundamentalDiagramSet!=null)
			for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fd : S.fundamentalDiagramSet.getFundamentalDiagramProfile())
				((FundamentalDiagramProfile)fd).validate();

        // validate routes
        if(S.routeSet!=null)
            for(edu.berkeley.path.beats.jaxb.Route route:S.routeSet.getRoute())
                ((Route)route).validate();

		// validate controllers
		S.controllerset.validate();

        // validate performance calculator
        if(S.perf_calc!=null)
            S.perf_calc.validate();

	}

	/** Prepare scenario for simulation:
	 * set the state of the scenario to the initial condition
	 * sample profiles
	 * open output files
	 * @return success		A boolean indicating whether the scenario was successfuly reset.
	 */
	public void reset() throws BeatsException {

		started_writing = false;
	    global_demand_knob = 1d;

		// reset the clock
		clock.reset();

		// reset network
		if(networkSet!=null)
			for(edu.berkeley.path.beats.jaxb.Network network : networkSet.getNetwork())
				((Network)network).reset();

		// sensor list
		sensorset.reset();

		// reset actuators
		actuatorset.reset();

		// reset demand profiles
		if(demandSet!=null)
			((DemandSet)demandSet).reset();
//        demandset.reset();

		// reset split ratios
		if(splitRatioSet!=null)
			((SplitRatioSet)splitRatioSet).reset();

		// reset fundamental diagrams
		if(fundamentalDiagramSet!=null)
			for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fd : fundamentalDiagramSet.getFundamentalDiagramProfile())
				((FundamentalDiagramProfile)fd).reset();

		// reset controllers
		controllerset.reset();

        // controllers may initialize their actuators
        for(Controller controller : controllerset.get_Controllers())
            if(controller.ison)
                controller.initialize_actuators();

		// reset events
		eventset.reset();

		cumulatives.reset();

        if(perf_calc!=null)
            perf_calc.reset();

	}

    // override by subclass to close down external loggers
    protected void close() throws BeatsException {
        if(perf_calc!=null)
            perf_calc.close_output();
        for(edu.berkeley.path.beats.jaxb.Node jnode : this.getNetworkSet().getNetwork().get(0).getNodeList().getNode()){
            Node node = (Node) jnode;
            if(node.split_ratio_logger !=null)
                try{ node.split_ratio_logger.close(); }
                catch(IOException e){
                    throw new BeatsException(e.getMessage());
                }
        }
        DebugLogger.close_all();
    }

	/////////////////////////////////////////////////////////////////////
	// initialization
	/////////////////////////////////////////////////////////////////////

    public void initialize_with_properties(BeatsProperties props) throws BeatsException {
        initialize( props.sim_dt ,
                props.start_time ,
                props.start_time + props.duration ,
                props.output_dt ,
                props.output_format,
                props.output_prefix,
                props.num_reps,
                props.ensemble_size ,
                props.uncertainty_model ,
                props.node_flow_model ,
                props.split_ratio_model ,
                props.performance_config ,
                props.run_mode,
                props.split_logger_prefix,
                props.split_logger_dt,
                props.aux_props,
                props.use_actm );
    }

	public void initialize(double timestep,double starttime,double endtime,int numEnsemble) throws BeatsException {
		initialize(timestep,starttime,endtime,Double.POSITIVE_INFINITY,"","",1,numEnsemble,
                "gaussian",
                "proportional",
                "A",
                "",
                "normal",
                "",
                Double.NaN,
                null,
                false);
	}

    public void initialize(double timestep,double starttime,double endtime,int numEnsemble,String uncertaintymodel,String nodeflowsolver,String nodesrsolver) throws BeatsException {
        initialize(timestep,starttime,endtime,Double.POSITIVE_INFINITY,"","",1,numEnsemble,uncertaintymodel,nodeflowsolver,nodesrsolver,
                "",
                "normal",
                "",
                Double.NaN,
                null,
                false);
    }

    public void initialize(double timestep,double starttime,double endtime, double outdt, String outtype,String outprefix, int numReps, int numEnsemble) throws BeatsException {
        initialize(timestep,starttime,endtime,outdt,outtype,outprefix,numReps,numEnsemble,
                "gaussian",
                "proportional",
                "A",
                "",
                "normal",
                "",
                Double.NaN,
                null,
                false);
    }

    public void initialize(double timestep,double starttime,double endtime, double outdt, String outtype,String outprefix,
                           int numReps, int numEnsemble,String uncertaintymodel,String nodeflowsolver,String nodesrsolver,
                           String performance_config, String run_mode,String split_logger_prefix,Double split_logger_dt ,
                           HashMap<String,Properties> aux_props) throws BeatsException {
        boolean is_actm = false;
        this.initialize(timestep,
                        starttime,
                        endtime,
                        outdt,
                        outtype,
                        outprefix,
                        numReps,
                        numEnsemble,
                        uncertaintymodel,
                        nodeflowsolver,
                        nodesrsolver,
                        performance_config,
                        run_mode,
                        split_logger_prefix,
                        split_logger_dt ,
                        aux_props,
                        is_actm);
    }

    public void initialize(double timestep,double starttime,double endtime, double outdt, String outtype,String outprefix,
                           int numReps, int numEnsemble,String uncertaintymodel,String nodeflowsolver,String nodesrsolver,
                           String performance_config, String run_mode,String split_logger_prefix,Double split_logger_dt ,
                           HashMap<String,Properties> aux_props,boolean is_actm) throws BeatsException {

        // set stuff
        set.uncertaintyModel(uncertaintymodel);
        set.splitLoggerPrefix(split_logger_prefix);
        set.splitLoggerDt(split_logger_dt);
        this.aux_props = aux_props;

        // convert units
        if (null == getSettings() || null == getSettings().getUnits())
            logger.warn("Scenario units not specified. Assuming SI");
        else if (!"SI".equalsIgnoreCase(getSettings().getUnits())) {
            logger.info("Converting scenario units from " + getSettings().getUnits() + " to SI");
            edu.berkeley.path.beats.util.UnitConverter.process(this);
        }

        // create scenario updater
        if(is_actm){
            logger.info("Running in ACTM mode.");
            runMode = RunMode.ACTM;
            updater = new ScenarioUpdaterACTM(this);
        }
        else{
            if(run_mode.compareToIgnoreCase("fw_fr_split_output")==0) {
                logger.info("Running in offramp demands mode.");
                runMode = RunMode.FRDEMANDS;
                updater = new ScenarioUpdaterFrFlow(this, nodeflowsolver, nodesrsolver);

                // generate split ratio controllers
                if(controllerSet==null)
                    controllerSet = new edu.berkeley.path.beats.jaxb.ControllerSet();

                controllerSet.getController().add(generate_SR_controllers());
            }
            else {
                logger.info("Running in normal mode.");
                runMode = RunMode.NORMAL;
                updater = new ScenarioUpdaterStandard(this, nodeflowsolver, nodesrsolver);
            }
        }

        // create performance calculator
        if(performance_config!=null && !performance_config.isEmpty())
            set.performance_calculator(Jaxb.create_performance_calculator(performance_config));

		// create run parameters object
		boolean writeoutput = outtype!=null && !outtype.isEmpty() && outprefix!=null && !outprefix.isEmpty();
		runParam = new RunParameters( timestep,
									  starttime,
									  endtime,
									  outdt,
									  writeoutput,
									  outtype,
									  outprefix,
									  numReps,
									  numEnsemble );

		// validate the run parameters
		runParam.validate();

		// lock the scenario
		scenario_locked = true;

		// populate and validate the scenario
		populate_validate();

		// compute the initial state by running the simulator to the start time
		set.initial_state();

		// create the clock
		clock = new Clock(runParam.t_start_output,runParam.t_end_output,runParam.dt_sim);

        // reset the simulation
        reset();

		// it's initialized
        initialized = true;

	}

	/**
	 * Processes a scenario loaded by JAXB.
	 * Converts units to SI, populates the scenario,
	 * registers signals and controllers,
	 * and validates the scenario.
	 * @return the updated scenario or null if an error occurred
	 * @throws BeatsException
	 */
	protected void populate_validate() throws BeatsException {

	    // populate the scenario ....................................................
	    populate();

	    // register signals with their targets ..................................
//	    boolean registersuccess = true;
//		if(getSignalSet()!=null)
//	    	for(edu.berkeley.path.beats.jaxb.ActuatorSignal signal: getSignalSet().getSignal())
//	    		registersuccess &= ((ActuatorSignal)signal).register();
//	    if(!registersuccess){
//	    	throw new BeatsException("ActuatorSignal registration failure");
//	    }

	    if(get.controllerset()!=null)
	    	if(!get.controllerset().register()){
	    		throw new BeatsException("Controller registration failure");
		    }

	    // print messages and clear before validation
		if (BeatsErrorLog.hasmessage()) {
			BeatsErrorLog.print();
			BeatsErrorLog.clearErrorMessage();
		}

		// validate scenario ......................................
	    Scenario.validate(this);

		if(BeatsErrorLog.haserror()){
			BeatsErrorLog.print();
			throw new ScenarioValidationError();
		}

		if(BeatsErrorLog.haswarning()) {
			BeatsErrorLog.print();
			BeatsErrorLog.clearErrorMessage();
		}

	}

	/////////////////////////////////////////////////////////////////////
	// start-to-end run
	/////////////////////////////////////////////////////////////////////

	public void run() throws BeatsException{

        if(!initialized)
            throw new BeatsException("Initialize first.");

		logger.info("Simulation period: [" + runParam.t_start_output + ":" + runParam.dt_sim + ":" + runParam.t_end_output + "]");
		logger.info("Output period: [" + runParam.t_start_output + ":" + runParam.dt_output + ":" + runParam.t_end_output + "]");

		// output writer properties
		Properties owr_props = new Properties();
		if (null != runParam.outprefix)
			owr_props.setProperty("prefix", runParam.outprefix);
		if (null != runParam.outtype)
			owr_props.setProperty("type",runParam.outtype);

		// loop through simulation runs ............................
		for(int i=0;i<runParam.numReps;i++){

			OutputWriterBase outputwriter = null;
			if (runParam.writefiles){
				outputwriter = OutputWriterFactory.getWriter(this, owr_props, runParam.dt_output, runParam.outsteps, runParam.t_start_output);
				outputwriter.open(i);
			}

            try{

				// advance to end of simulation
				while( advanceNSteps_internal(1,runParam.writefiles,outputwriter) ){}

			}

            finally {
                if (null != outputwriter)
                    outputwriter.close();
                close();
			}
		}
        scenario_locked = false;

	}

	/////////////////////////////////////////////////////////////////////
	// step-by-step run
	/////////////////////////////////////////////////////////////////////

    /** Advance the simulation <i>nsec</i> seconds.
	 *
	 * <p> Move the simulation forward <i>nsec</i> seconds and stops.
	 * Returns <code>true</code> if the operation completes succesfully. Returns <code>false</code>
	 * if the end of the simulation is reached.
	 * @param nsec Number of seconds to advance.
	 * @throws BeatsException
	 */
	public boolean advanceNSeconds(double nsec) throws BeatsException{

		if(!scenario_locked)
			throw new BeatsException("Run not initialized. Use initialize_run() first.");

		if(!BeatsMath.isintegermultipleof(nsec,runParam.dt_sim))
			throw new BeatsException("nsec (" + nsec + ") must be an interger multiple of simulation dt (" + runParam.dt_sim + ").");
		int nsteps = BeatsMath.round(nsec/runParam.dt_sim);
		return advanceNSteps_internal(nsteps,false,null);
	}

    public boolean advanceNSeconds(double nsec,OutputWriterBase outputwriter) throws BeatsException{

        if(!scenario_locked)
            throw new BeatsException("Run not initialized. Use initialize_run() first.");

        if(!BeatsMath.isintegermultipleof(nsec,runParam.dt_sim))
            throw new BeatsException("nsec (" + nsec + ") must be an interger multiple of simulation dt (" + runParam.dt_sim + ").");
        int nsteps = BeatsMath.round(nsec/runParam.dt_sim);
        return advanceNSteps_internal(nsteps,true,outputwriter);
    }

    protected boolean advanceNSteps_internal(int n,boolean writefiles,OutputWriterBase outputwriter) throws BeatsException{

        if(DebugFlags.time_print>0 && get.currentTimeInSeconds()%DebugFlags.time_print == 0)
            System.out.println(get.currentTimeInSeconds());

        // advance n steps
        for(int k=0;k<n;k++){

            // export initial condition
            if(outputwriter!=null && !started_writing && BeatsMath.equals(clock.getT(),outputwriter.outStart) ){
                recordstate(writefiles,outputwriter,false);
                started_writing = true;
            }

            // update scenario
            updater.update();

            if(outputwriter!=null && started_writing && clock.getRelativeTimeStep()%outputwriter.outSteps == 0 )
                recordstate(writefiles,outputwriter,true);

            if(clock.expired())
                return false;
        }

        return true;
    }

	/////////////////////////////////////////////////////////////////////
	// serialization
	/////////////////////////////////////////////////////////////////////

	public void saveToXML(String filename) throws BeatsException{
        try {
        	
        	//Reset the classloader for main thread; need this if I want to run properly
            //with JAXB within MATLAB. (luis)
        	Thread.currentThread().setContextClassLoader(Scenario.class.getClassLoader());
	
        	JAXBContext context = JAXBContext.newInstance("edu.berkeley.path.beats.jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(this,new FileOutputStream(filename));
        } catch( JAXBException je ) {
        	throw new BeatsException(je.getMessage());
        } catch (FileNotFoundException e) {
        	throw new BeatsException(e.getMessage());
        }
	}

    /////////////////////////////////////////////////////////////////////
    // data / calibration
    /////////////////////////////////////////////////////////////////////

    public void loadSensorData() throws BeatsException {

		if(sensorset.getSensors().isEmpty())
			return;
		
		if(sensor_data_loaded)
			return;

		HashMap <Integer,FiveMinuteData> data = new HashMap <Integer,FiveMinuteData> ();
		ArrayList<DataSource> datasources = new ArrayList<DataSource>();
		ArrayList<String> uniqueurls  = new ArrayList<String>();
		
		// construct list of stations to extract from datafile 
		for(Sensor sensor : sensorset.getSensors()){
			if(sensor.getMyType()!=Sensor.Type.loop)
				continue;
			SensorLoopStation S = (SensorLoopStation) sensor;
			int myVDS = S.getVDS();				
			data.put(myVDS, new FiveMinuteData(myVDS,true));	
			for(edu.berkeley.path.beats.sensor.DataSource d : S.get_datasources()){
				String myurl = d.getUrl();
				int indexOf = uniqueurls.indexOf(myurl);
				if( indexOf<0 ){
					DataSource newdatasource = new DataSource(d);
					newdatasource.add_to_for_vds(myVDS);
					datasources.add(newdatasource);
					uniqueurls.add(myurl);
				}
				else{
					datasources.get(indexOf).add_to_for_vds(myVDS);
				}
			}
		}
		
		// Read 5 minute data to "data"
		DataFileReader P = new DataFileReader();
		P.Read5minData(data,datasources);
		
		// distribute data to sensors
		for(Sensor sensor : sensorset.getSensors()){
			
			if(sensor.getMyType()!=Sensor.Type.loop)
				continue;

			SensorLoopStation S = (SensorLoopStation) sensor;
			
			// attach to sensor
			S.set5minData(data.get(S.getVDS()));
		}
		
		sensor_data_loaded = true;
		
	}

	protected void calibrate_fundamental_diagrams() throws BeatsException {
		FDCalibrator.calibrate(this);
	}

    /////////////////////////////////////////////////////////////////////
    // writer
    /////////////////////////////////////////////////////////////////////

    protected void recordstate(boolean writefiles,OutputWriterBase outputwriter,boolean exportflows) throws BeatsException {
		if(writefiles)
			outputwriter.recordstate(clock.getT(),exportflows,outputwriter.outSteps);
		cumulatives.reset();
	}

    /////////////////////////////////////////////////////////////////////
    // predictors
    /////////////////////////////////////////////////////////////////////

//    public SplitRatioSet predict_split_ratios(double time_current,double sample_dt,double horizon){
//
//        Network network = (Network) getNetworkSet().getNetwork().get(0);
//        JaxbObjectFactory factory = new JaxbObjectFactory();
//        SplitRatioSet split_ratio_set = (SplitRatioSet) factory.createSplitRatioSet();
//        for(edu.berkeley.path.beats.jaxb.Node jaxbN : network.getListOfNodes()){
//            Node N = (Node) jaxbN;
//
//            if(N.istrivialsplit())
//                continue;
//
//            SplitRatioProfile sr_profile = N.getSplitRatioProfile();
//            SplitRatioProfile srp = (SplitRatioProfile) factory.createSplitRatioProfile();
//            split_ratio_set.getSplitRatioProfile().add(srp);
//
//            double srp_sample_dt = Double.isNaN(sample_dt) ? sr_profile.getDt() : sample_dt;
//            int horizon_steps = BeatsMath.round(horizon/srp_sample_dt);
//
//            srp.setDt(srp_sample_dt);
//            srp.setNodeId(N.getId());
//            for(Input in : N.getInputs().getInput()){
//                for(Output out : N.getOutputs().getOutput()){
//                    for(int v=0;v<get.numVehicleTypes();v++)    {
//
//                        Splitratio splitratio = factory.createSplitratio();
//
//                        // set values
//                        splitratio.setLinkIn(in.getLinkId());
//                        splitratio.setLinkOut(out.getLinkId());
//                        splitratio.setVehicleTypeId(get.vehicleTypeIdForIndex(v));
//                        double [] sr = sr_profile.predict(
//                                in.getLinkId(),
//                                out.getLinkId(),
//                                v,time_current, srp_sample_dt, horizon_steps);
//
//                        if(sr==null)
//                            continue;
//
//                        srp.getSplitratio().add(splitratio);
//                        splitratio.setContent(BeatsFormatter.csv(sr, ","));
//                    }
//                }
//            }
//        }
//        return split_ratio_set;
//    }

//    public DemandSet predict_demands_si(double time_current, double sample_dt, double horizon){
//
//        Network network = (Network) getNetworkSet().getNetwork().get(0);
//        JaxbObjectFactory factory = new JaxbObjectFactory();
//
//        DemandSet demand_set = (DemandSet) factory.createDemandSet();
//        for(edu.berkeley.path.beats.jaxb.Link jaxbL : network.getListOfLinks()){
//            Link L = (Link) jaxbL;
//            if(L.isSource()){
//                DemandProfile dem_profile = L.getDemandProfile();
//
//                // add demands to demand_set
//                DemandProfile dp = (DemandProfile) factory.createDemandProfile();
//                demand_set.getDemandProfile().add(dp);
//
//                double dp_sample_dt= Double.isNaN(sample_dt) ? dem_profile.getDt() : sample_dt;
//                int horizon_steps = BeatsMath.round(horizon/dp_sample_dt);
//
//                dp.setLinkIdOrg(L.getId());
//                dp.setDt(dp_sample_dt);
//                for(int v=0;v<get.numVehicleTypes();v++){
//                    Demand dem = factory.createDemand();
//                    dp.getDemand().add(dem);
//
//                    // set values
//                    dem.setVehicleTypeId(get.vehicleTypeIdForIndex(v));
//                    double [] x = dem_profile.predict_in_VPS(v, time_current, dp_sample_dt, horizon_steps);
//                    dem.setContent(BeatsFormatter.csv(x, ","));
//                }
//            }
//        }
//        return demand_set;
//    }

    /////////////////////////////////////////////////////////////////////
    // inner classes
    /////////////////////////////////////////////////////////////////////

    public class RunParameters implements Serializable {

        private static final long serialVersionUID = 4449540657191375227L;

        // prescribed
        public double dt_sim;				// [sec] simulation time step
        public double t_start_output;		// [sec] start outputing data
        public double t_end_output;			// [sec] end of the simulation
        public double dt_output;				// [sec] output sampling time

        public boolean writefiles;
        public String outtype;
        public String outprefix;
        public int numReps;
        public int numEnsemble;

        // derived
        public int outsteps;				// [-] number of simulation steps per output step

        // input parameter outdt [sec] output sampling time
        public RunParameters(double simdt,double tstart,double tend,double outdt, boolean dowrite,String outtype,String outprefix,int numReps,int numEnsemble) throws BeatsException{

            // round to the nearest decisecond
            simdt = round(simdt);
            tstart = round(tstart);
            tend = round(tend);
            outdt = round(outdt);

            this.dt_sim = simdt;
            this.t_start_output = tstart;
            this.t_end_output = tend;
            this.numReps = numReps;
            this.numEnsemble = numEnsemble;

            this.writefiles = Double.isInfinite(outdt)||Double.isNaN(outdt) ? false : dowrite;

            if(writefiles){
                this.outtype = outtype;
                this.outprefix = outprefix;
                this.outsteps = BeatsMath.round(outdt/simdt);
                this.dt_output = outsteps*simdt;
            }
            else{
                this.outtype = "";
                this.outprefix = "";
                this.outsteps = -1;
                this.dt_output = Double.POSITIVE_INFINITY;
            }
        }

        public void validate() throws BeatsException{

            // check simdt non-negative
            if( BeatsMath.lessthan(dt_sim,0d))
                throw new BeatsException("Negative time step.");

            // check tstart non-negative
            if( BeatsMath.lessthan(t_start_output,0d))
                throw new BeatsException("Negative start time.");

            // check timestart < timeend
            if( BeatsMath.greaterorequalthan(t_start_output,t_end_output))
                throw new BeatsException("Empty simulation period.");

            // check that outdt is a multiple of simdt
            if(!Double.isInfinite(dt_output) && !BeatsMath.isintegermultipleof(dt_output,dt_sim))
                throw new BeatsException("outdt (" + dt_output + ") must be an interger multiple of simulation dt (" + dt_sim + ").");

            // initial density set time stamp
            double time_ic = getInitialDensitySet()!=null ? getInitialDensitySet().getTstamp() : Double.POSITIVE_INFINITY;  // [sec]

            // check values
            if(BeatsMath.lessthan(t_start_output,time_ic) && BeatsMath.lessthan(time_ic,t_end_output))
                throw new BeatsException("Illegal start/end time: start<i.c.<end is not allowed");

        }

        public RunParameters clone() {
            RunParameters that = null;
            try {
                that = new RunParameters(dt_sim, t_start_output, t_end_output, outsteps*dt_sim,  writefiles, outtype, outprefix, numReps, numEnsemble);
            } catch (BeatsException e) {
                e.printStackTrace();
                return null;
            }
            return that;
        }

        /**
         * Rounds the double value, precision: .1
         * @param val
         * @return the "rounded" value
         */
        protected double round(double val) {
            if(Double.isInfinite(val))
                return val;
            if(Double.isNaN(val))
                return val;
            return BeatsMath.round(val * 10.0) / 10.0;
        }
    }

    public static class Cumulatives implements Serializable {

        private static final long serialVersionUID = -4531632662663725971L;

        Scenario scenario;
        java.util.Map<Long, LinkCumulativeData> links = null;
        private int stepsSinceReset;

        protected static Logger logger = Logger.getLogger(Cumulatives.class);

        public Cumulatives(Scenario scenario) {
            this.scenario = scenario;
        }

        public void storeLinks() {
            if (null == links) {
                links = new java.util.HashMap<Long, LinkCumulativeData>();
                for (edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork()){
                    if(((edu.berkeley.path.beats.simulator.Network) network).isEmpty())
                        continue;
                    for (edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink()) {
                        if (links.containsKey(link.getId()))
                            logger.warn("Duplicate link: ID=" + link.getId());
                        links.put(link.getId(), new LinkCumulativeData((edu.berkeley.path.beats.simulator.Link) link));
                    }
                }
                logger.info("Link cumulative data have been requested");
                stepsSinceReset = 0;
            }
        }

        public void reset() {
            if (null != links) {
                Iterator<LinkCumulativeData> iter = links.values().iterator();
                while (iter.hasNext()) iter.next().reset();
            }
//			if (null != phases) {
//				Iterator<SignalPhases> iter = phases.values().iterator();
//				while (iter.hasNext()) iter.next().reset();
//			}
            stepsSinceReset = 0;
        }

        public void update() throws BeatsException {
            if (null != links) {
                java.util.Iterator<LinkCumulativeData> iter = links.values().iterator();
                while (iter.hasNext()) iter.next().update();
            }
//			if (null != phases) {
//				Iterator<SignalPhases> iter = phases.values().iterator();
//				while (iter.hasNext()) iter.next().update();
//			}
            stepsSinceReset++;
        }

        public int getStepsSinceReset() {
            return stepsSinceReset;
        }

        public LinkCumulativeData get(edu.berkeley.path.beats.jaxb.Link link) throws BeatsException {
            if (null == links) throw new BeatsException("Link cumulative data were not requested");
            return links.get(link.getId());
        }

//		public SignalPhases get(edu.berkeley.path.beats.jaxb.Signal signal) throws BeatsException {
//			if (null == phases) throw new BeatsException("ActuatorSignal phases were not requested");
//			return phases.get(signal.getId());
//		}
    }


    /// TEMPORARY GETTERS FOR THE API CLASSES


//    @Override
//    public edu.berkeley.path.beats.jaxb.DemandSet getDemandSet() {
//        return this.demandset;
//    }

    public List<edu.berkeley.path.beats.jaxb.Network> getNetworks(){
        return this.networkSet.getNetwork();
    }

    @Override
    public edu.berkeley.path.beats.jaxb.InitialDensitySet getInitialDensitySet() {
        return super.getInitialDensitySet();
    }

    @Override
    public DownstreamBoundaryCapacitySet getDownstreamBoundaryCapacitySet() {
        return super.getDownstreamBoundaryCapacitySet();
    }

    @Override
    public void setDownstreamBoundaryCapacitySet(DownstreamBoundaryCapacitySet value) {
        super.setDownstreamBoundaryCapacitySet(value);
    }



    /////////////////////////////////////////////////////////////////////
    // private
    /////////////////////////////////////////////////////////////////////

    private edu.berkeley.path.beats.jaxb.Controller  generate_SR_controllers(){

        edu.berkeley.path.beats.jaxb.Controller contr = new edu.berkeley.path.beats.jaxb.Controller();
        contr.setType("SR_Generator_new");
        contr.setEnabled(true);

        // construct a jaxb table
        edu.berkeley.path.beats.jaxb.Table table = new edu.berkeley.path.beats.jaxb.Table();
        contr.getTable().add(table);
        edu.berkeley.path.beats.jaxb.ColumnNames colnames = new edu.berkeley.path.beats.jaxb.ColumnNames();
        table.setColumnNames(colnames);

        ColumnName col0 = new ColumnName();
        col0.setId(0);
        col0.setName("linkid");
        colnames.getColumnName().add(0,col0);

        ColumnName col1 = new ColumnName();
        col1.setId(1);
        col1.setName("demand");
        colnames.getColumnName().add(1,col1);

        ColumnName col2 = new ColumnName();
        col2.setId(2);
        col2.setName("dt");
        colnames.getColumnName().add(2,col2);

        ColumnName col3 = new ColumnName();
        col3.setId(3);
        col3.setName("knob");
        colnames.getColumnName().add(3,col3);

        ColumnName col4 = new ColumnName();
        col3.setId(4);
        col3.setName("start_time");
        colnames.getColumnName().add(4,col4);

        for(edu.berkeley.path.beats.jaxb.DemandProfile dp : demandSet.getDemandProfile()){
            Link link = get.linkWithId(dp.getLinkIdOrg());
            Node endnode = get.nodeWithId(link.getEnd().getNodeId());
            boolean issink = endnode.getOutputs()==null || endnode.getOutputs().getOutput().isEmpty();

            // create a controller in this case
            if(issink) {

                edu.berkeley.path.beats.jaxb.Row row = new edu.berkeley.path.beats.jaxb.Row();

                // linkid
                Column c0 = new Column();
                c0.setId(0);
                c0.setContent(String.format("%d", dp.getLinkIdOrg()));
                row.getColumn().add(c0);

                // demand
                Column c1 = new Column();
                c1.setId(1);
                c1.setContent(dp.getDemand().get(0).getContent());
                row.getColumn().add(c1);

                // dt
                Column c2 = new Column();
                c2.setId(2);
                c2.setContent(String.format("%f", dp.getDt()));
                row.getColumn().add(c2);

                // knob
                Column c3 = new Column();
                c3.setId(3);
                c3.setContent(String.format("%f",dp.getKnob()));
                row.getColumn().add(c3);

                // start time
                Column c4 = new Column();
                c4.setId(4);
                c4.setContent(String.format("%f",dp.getStartTime()));
                row.getColumn().add(c4);

                table.getRow().add(row);
            }
        }

        return contr;
    }
}
