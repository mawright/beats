package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.control.Controller_VehType_Swapper;
import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 6/4/2015.
 */
public class ActuatorVehType extends Actuator {

    protected List<SwitchRatio> switchRatios;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////

    public ActuatorVehType(Scenario myScenario, edu.berkeley.path.beats.jaxb.Actuator jaxbA, ActuatorImplementation act_implementor){
        super(myScenario,jaxbA,act_implementor);
    }

    @Override
    public void populate(Object jaxbObject, Scenario scenario) {
        switchRatios = new ArrayList<SwitchRatio>();
    }

    public void set_switch_ratio(long vehTypeIn, long vehTypeOut, double value) {

        // overwrite if it exists
        for( SwitchRatio S : switchRatios) {
            if( S.getVehicleTypeIn() == vehTypeIn && S.getVehicleTypeOut() == vehTypeOut) {
                S.setContent(String.format("%f",value));
                return;
            }
        }

        // otherwise append it
        SwitchRatio newSwitchRatio = (new JaxbObjectFactory()).createSwitchRatio();
        newSwitchRatio.setVehicleTypeIn(vehTypeIn);
        newSwitchRatio.setVehicleTypeOut(vehTypeOut);
        newSwitchRatio.setContent(String.format("%f",value));
        switchRatios.add(newSwitchRatio);
    }

    @Override
    public boolean register() {
        return ((Link)implementor.get_target()).register_density_controller();
    }

    @Override
    public void reset() throws BeatsException {
        super.reset();
        switchRatios = new ArrayList<SwitchRatio>();
    }


    @Override
    public void deploy(double current_time_in_seconds) {
        for(int i=0; i<switchRatios.size();i++) {
            this.implementor.deploy_switch_ratio(switchRatios);
        }
    }
}
