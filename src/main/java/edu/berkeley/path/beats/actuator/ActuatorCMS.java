package edu.berkeley.path.beats.actuator;

import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActuatorCMS extends Actuator  implements Serializable {

    private static final long serialVersionUID = 4603644362503732870L;
    protected List<Splitratio> splits;

    /////////////////////////////////////////////////////////////////////
    // actuation command
    /////////////////////////////////////////////////////////////////////

    public void turn_off(){
        splits = new ArrayList<Splitratio>();
    }

    public void set_split(Long in_link_id,Long out_link_id,Long vehicle_type_id,double sr){

        // override if I have it
        for(Splitratio S : splits){
            if(S.getLinkIn()==in_link_id && S.getLinkOut()==out_link_id && S.getVehicleTypeId()==vehicle_type_id){
                S.setContent(String.format("%f",sr));
                return;
            }
        }

        // otherwise add it
        Splitratio newsr = (new JaxbObjectFactory()).createSplitratio();
        newsr.setLinkIn(in_link_id);
        newsr.setLinkOut(out_link_id);
        newsr.setVehicleTypeId(vehicle_type_id);
        newsr.setContent(String.format("%f",sr));
        splits.add(newsr);
    }

    public void set_split(Long in_link_id,Long out_link_id,double sr){
        for (VehicleType vt : getMyController().getMyScenario().getVehicleTypeSet().getVehicleType())
            set_split(in_link_id,out_link_id, vt.getId(),sr);
    }


	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

    public ActuatorCMS(Scenario myScenario,edu.berkeley.path.beats.jaxb.Actuator jaxbA,ActuatorImplementation act_implementor){
        super(myScenario,jaxbA,act_implementor);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset / deploy
    /////////////////////////////////////////////////////////////////////

    @Override
    public void populate(Object jaxbobject,Scenario myScenario) {
        splits = new ArrayList<Splitratio>();
    }

    @Override
    public void reset() throws BeatsException {
        splits = new ArrayList<Splitratio>();
        super.reset();
    }

    @Override
    public void deploy(double current_time_in_seconds) {
        this.implementor.deploy_cms_split(splits);
    }

    @Override
    protected void deploy_off_signal(){
        implementor.deploy_cms_split(null);
    };

    @Override
    public boolean register() {
        return ((Node)implementor.get_target()).register_split_controller();
    }
}
