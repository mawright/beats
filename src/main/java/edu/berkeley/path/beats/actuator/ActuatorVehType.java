package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.control.Controller_VehType_Swapper;
import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.ActuatorImplementation;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 6/4/2015.
 */
public class ActuatorVehType extends Actuator {

    protected List<Double> currentSwitchRatio;
    protected List<Integer> VehTypeIn;
    protected List<Integer> VehTypeOut;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public ActuatorVehType(Scenario myScenario, edu.berkeley.path.beats.jaxb.Actuator jaxbA, ActuatorImplementation act_implementor){
        super(myScenario,jaxbA,act_implementor);
    }

    @Override
    public void populate(Object jaxbObject, Scenario scenario) {
        currentSwitchRatio = new ArrayList<Double>();
    }

    public void set_switch_ratio(int vehTypeIn, int vehTypeOut, double value) {

        // overwrite if it exists

    }

    @Override
    public boolean register() {
        return ((Link)implementor.get_target()).register_density_controller();
    }

    @Override
    public void reset() throws BeatsException {
        super.reset();
        VehTypeIn = ((Controller_VehType_Swapper) getMyController()).getVehTypesIn();
        VehTypeOut = ((Controller_VehType_Swapper) getMyController()).getVehTypesOut();
    }


    @Override
    public void deploy(double current_time_in_seconds) {
        for(int i=0; i<currentSwitchRatio.size();i++) {
            this.implementor.deploy_switch_ratio(VehTypeIn, VehTypeOut, currentSwitchRatio);
        }
    }
}
