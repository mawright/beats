package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorRampMeter;
//import edu.berkeley.path.beats.control.adjoint.RampMeteringPolicyMakerAdjoint;
//import edu.berkeley.path.beats.control.lp.RampMeteringPolicyMakerLp;
import edu.berkeley.path.beats.control.rm_interface.*;
import edu.berkeley.path.beats.simulator.ScenarioElement;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Network;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Properties;

public class Controller_CRM_MPC extends Controller implements Serializable{

    private static final long serialVersionUID = 5762681321621908443L;
    // policy maker
    private RampMeteringPolicyMaker policy_maker;
    private Properties pm_props;
    private RampMeteringPolicySet policy;
    private RampMeteringControlSet controller_parameters;
    private HashMap<Link,Actuator> link_actuator_map;

    private edu.berkeley.path.beats.simulator.Network network;

    // parameters
    private double pm_period;		  // [sec] period for calling the policy maker
    private double pm_horizon;		  // [sec] policy maker time horizon
    private double pm_dt;		 	  // [sec] internal time step for the policy maker

    // variable
    private double time_last_opt;     // [sec] time of last policy maker call

    private static enum PolicyMakerType {tester,adjoint,lp,NULL}

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_CRM_MPC(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.CRM_MPC);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbobject) {

        edu.berkeley.path.beats.simulator.Parameters params = (edu.berkeley.path.beats.simulator.Parameters) getJaxbController().getParameters();

        // controller parameters
//        for(edu.berkeley.path.beats.jaxb.Link jaxbL : network.getLinkList().getLink()){
//            Link L = (Link) jaxbL;
//            if(L.isOnramp()){
//                RampMeteringControl con = new RampMeteringControl();
//                con.link = L;
//                con.max_rate = 900d/3600d;    // veh/sec
//                con.min_rate = 0;    // veh/sec
//                controller_parameters.control.add(con);
//            }
//        }

        // link->actuator map
        link_actuator_map = new HashMap<Link,Actuator>();
        controller_parameters = new RampMeteringControlSet();
        for(Actuator act : actuators){
            ScenarioElement se =  (ScenarioElement) act.getScenarioElement();
            if(se.getMyType()==ScenarioElement.Type.link){
                Link link = myScenario.get.linkWithId(se.getId());
                link_actuator_map.put(link,act);
                Parameters param = (Parameters) act.getParameters();
                RampMeteringControl con = new RampMeteringControl(link);
                if(param!=null)  {
                    if(param.has("max_rate_in_vphpl"))
                        con.max_rate = Double.parseDouble(param.get("max_rate_in_vphpl"))*link.getLanes()/3600d;  // veh/sec
                    if(param.has("min_rate_in_vphpl"))
                        con.min_rate = Double.parseDouble(param.get("min_rate_in_vphpl"))*link.getLanes()/3600d;    // veh/sec
                }
                controller_parameters.control.add(con);
            }
        }

        // read timing parameters
        if(params!=null){
            pm_period = params.readParameter("dt_optimize", getDtinseconds());
            pm_dt = params.readParameter("policy_maker_timestep", getMyScenario().get.simdtinseconds());
            pm_horizon = params.readParameter("policy_maker_horizon",Double.NaN);
        }
        else{
            pm_period = getDtinseconds();
            pm_dt = getMyScenario().get.simdtinseconds();
            pm_horizon = Double.NaN;
        }

        // assign network (it will already be assigned if controller is scenario-less)
        if(network==null && myScenario!=null)
            network = (Network) myScenario.getNetworkSet().getNetwork().get(0);

        // generate the policy maker
        policy_maker = null;
        if (null != params && params.has("policy")){
            PolicyMakerType myPMType;
            try {
                myPMType = PolicyMakerType.valueOf(params.get("policy").toLowerCase());
            } catch (IllegalArgumentException e) {
                myPMType = PolicyMakerType.NULL;
            }

            switch(myPMType){
                case tester:
                    policy_maker = new PolicyMaker_Tester();
                    break;
//                case adjoint:
//                    pm_props = myScenario.get_auxiliary_properties("RAMP_METERING_ADJOINT");
//                    policy_maker = new RampMeteringPolicyMakerAdjoint();
//                    break;
//				case lp:
//                    pm_props = myScenario.get_auxiliary_properties("RAMP_METERING_LP");
//                    double K_cool_seconds = Double.parseDouble(pm_props.getProperty("K_cool_seconds"));
//                    double eta = Double.parseDouble(pm_props.getProperty("eta"));
//                    policy_maker = new RampMeteringPolicyMakerLp(myScenario,pm_horizon,K_cool_seconds,eta);
//					break;
                case NULL:
                    break;
            }
        }

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

//        double time_current = getMyScenario().get.currentTimeInSeconds();
//
//        // if it is time to optimize, update metering rate profile
//        if(BeatsMath.greaterorequalthan(time_current-time_last_opt, pm_period)){
//
//            // call policy maker (everything in SI units)
//            policy = policy_maker.givePolicy( network,
//                    myScenario.get.current_fds_si(time_current),
//                    myScenario.predict_demands_si(time_current, pm_dt, pm_horizon),
//                    myScenario.predict_split_ratios(time_current,pm_dt,pm_horizon),
//                    myScenario.get.current_densities_si(),
//                    controller_parameters,
//                    pm_dt,
//                    pm_props);
//
////            System.out.println(time_current+"\n"+policy);
//
//            // update time keeper
//            time_last_opt = time_current;
//        }
//
//        // .....
//        send_policy_to_actuators(time_current);

    }

    public void send_policy_to_actuators(double time_current){
        if(policy==null)
            return;
        double time_since_last_pm_call = time_current-time_last_opt;
        int time_index = (int) (time_since_last_pm_call/pm_dt);
        for(RampMeteringPolicyProfile rmprofile : policy.profiles){
            ActuatorRampMeter act = (ActuatorRampMeter) link_actuator_map.get(rmprofile.sensorLink);
            if(act!=null){
                int clipped_time_index = Math.min(time_index,rmprofile.rampMeteringPolicy.size()-1);
                double policy = rmprofile.rampMeteringPolicy.get(clipped_time_index)*3600d;
                // System.out.println(policy);
                act.setMeteringRateInVPH( policy);
            }
            else{
                System.out.println("WARNING: Actuator not found!");
            }
        }
    }

}
