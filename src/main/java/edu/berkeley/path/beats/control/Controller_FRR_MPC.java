package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorCMS;
import edu.berkeley.path.beats.control.adjoint_glue.AdjointReroutesPolicyMaker;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.ScenarioElement;

import java.util.HashMap;
import java.util.Properties;

public class Controller_FRR_MPC extends Controller {

    // policy maker
    private ReroutePolicyMaker policy_maker;
    private Properties policy_maker_properties;
    private ReroutePolicySet policy;
    private HashMap<Long,Actuator> node_actuator_map;

    private edu.berkeley.path.beats.simulator.Network network;

    // parameters
	private double pm_period;		  // [sec] period for calling the policy maker
	private double pm_horizon;		  // [sec] policy maker time horizon
    private double pm_dt;		 	  // [sec] internal time step for the policy maker

    // variable
    private double time_last_opt;     // [sec] time of last policy maker call

    private static enum PolicyMakerType {adjoint,NULL}


    // derived
//    private int pm_horizon_steps;     // pm_horizon/pm_dt

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_FRR_MPC(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(myScenario,c,Algorithm.CRM_MPC);
	}

	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset
	/////////////////////////////////////////////////////////////////////

	@Override
	protected void populate(Object jaxbobject) {

		// generate the policy maker
		edu.berkeley.path.beats.simulator.Parameters params = (edu.berkeley.path.beats.simulator.Parameters) getJaxbController().getParameters();
		policy_maker = null;
        policy_maker_properties = null;
		if (null != params && params.has("policy")){
            PolicyMakerType myPMType;
	    	try {
				myPMType = PolicyMakerType.valueOf(params.get("policy").toLowerCase());
			} catch (IllegalArgumentException e) {
				myPMType = PolicyMakerType.NULL;
			}

			switch(myPMType){
				case adjoint:
					policy_maker = new AdjointReroutesPolicyMaker();
                    policy_maker_properties = myScenario.get_auxiliary_properties("REROUTES_ADJOINT");
					break;
				case NULL:
					break;
			}
		}


        // link->actuator map
        node_actuator_map = new HashMap<Long,Actuator>();
        for(Actuator act : actuators){
            ScenarioElement se = (ScenarioElement) act.getScenarioElement();
            if(se.getMyType()==ScenarioElement.Type.node)
                node_actuator_map.put(new Long(se.getId()),act);
        }

		// read timing parameters
        if(params!=null){
            pm_period = params.readParameter("dt_optimize",getDtinseconds());
            pm_dt = params.readParameter("policy_maker_timestep",getMyScenario().getSimdtinseconds());
            pm_horizon = params.readParameter("policy_maker_horizon",Double.NaN);
        }
        else{
            pm_period = getDtinseconds();
            pm_dt = getMyScenario().getSimdtinseconds();
            pm_horizon = Double.NaN;
        }

//        pm_horizon_steps = BeatsMath.round(pm_horizon/pm_dt);

        // assign network (it will already be assigned if controller is scenario-less)
        if(network==null && myScenario!=null)
            network = (Network) myScenario.getNetworkSet().getNetwork().get(0);

        // controller parameters
//        for(edu.berkeley.path.beats.jaxb.Link jaxbL : network.getLinkList().getLink()){
//            Link L = (Link) jaxbL;
//            if(L.isSource()){
//            	RerouteControl con = new RerouteControl();
//            }
//        }

    }

	@Override
	protected void validate() {

		super.validate();

		if(policy_maker==null)
			BeatsErrorLog.addError("Control algorithm undefined.");

		if(Double.isNaN(pm_horizon))
			BeatsErrorLog.addError("Optimization horizon undefined.");

        // opt_horizon is a multiple of pm_dt
        if(!Double.isNaN(pm_horizon) && !BeatsMath.isintegermultipleof(pm_horizon, pm_dt))
            BeatsErrorLog.addError("pm_horizon is a multiple of pm_dt.");

        // opt_horizon is greater than opt_period
        if(!Double.isNaN(pm_horizon) && !BeatsMath.greaterorequalthan(pm_horizon,pm_period) )
            BeatsErrorLog.addError("pm_horizon is less than pm_period.");

        // validations below this make sensor only in the context of a scenario
        if(getMyScenario()==null)
            return;

        // opt_period is a multiple of dtinseconds
		if(!BeatsMath.isintegermultipleof(pm_period,getDtinseconds()))
			BeatsErrorLog.addError("pm_period is not a a multiple of dtinseconds.");

		// dtinseconds is a multiple of pm_dt
		if(!BeatsMath.isintegermultipleof(getDtinseconds(), pm_dt))
			BeatsErrorLog.addError("dtinseconds ("+getDtinseconds()+") is not a multiple of pm_dt ("+pm_dt+").");

	}

	@Override
	protected void reset() {
		super.reset();
		time_last_opt = Double.NEGATIVE_INFINITY;
	}

	/////////////////////////////////////////////////////////////////////
	// update
	/////////////////////////////////////////////////////////////////////

	@Override
	protected void update() throws BeatsException {

		double time_current = getMyScenario().getCurrentTimeInSeconds();

		// if it is time to optimize, update metering rate profile
		if(BeatsMath.greaterorequalthan(time_current-time_last_opt, pm_period)){

			// call policy maker (everything in SI units)
            policy = policy_maker.givePolicy( network,
                                              myScenario.gather_current_fds(time_current),
                                              myScenario.predict_demands(time_current,Double.NaN,pm_horizon,false),
                                              myScenario.predict_split_ratios(time_current,Double.NaN,pm_horizon),
                                              myScenario.gather_current_densities(),
                                              myScenario.getRouteSet(),
                                              pm_dt,
                                              pm_horizon,
                                              policy_maker_properties );

            // update time keeper
			time_last_opt = time_current;
		}

        // send policy to actuators
        if(policy!=null){
            double time_since_last_pm_call = time_current-time_last_opt;
            int time_index = (int) (time_since_last_pm_call/pm_dt);
            for(ReroutePolicyProfile rrprofile : policy.profiles){
//                ActuatorCMS act = (ActuatorCMS) node_actuator_map.get(rrprofile.actuatorNode.getId());

                // This is a hack. Really the actuator should be passed to and returned by
                // the policy maker. Idea is to replace RouteSet with Hasmap<actuator id,RouteSet> in
                // givePolicy parameters.
                ActuatorCMS act = (ActuatorCMS) actuators.get(0);

                if(act!=null){

                    Link first_link=null;
                    Link second_link=null;

                    Route route = myScenario.getRouteWithId(rrprofile.route_id);

                    if(route!=null) {
                        first_link = route.get_link_number(0);
                        second_link = route.get_link_number(1);
                    }

                    if(first_link!=null & second_link!=null){
                        int clipped_time_index = Math.min(time_index,rrprofile.reroutePolicy.size()-1);
                        // this needs to be modified to account for change in rrprofile //
                        act.set_split(
                                first_link.getId(),
                                second_link.getId(),
                                rrprofile.compliant_vehicle_type_id,
                                rrprofile.reroutePolicy.get(clipped_time_index) );
                    }

                }
            }
        }

	}

}
