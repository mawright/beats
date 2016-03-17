package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorVehType;
import edu.berkeley.path.beats.jaxb.Parameter;
import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileDouble;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gomes on 6/4/2015.
 */
public class Controller_VehType_Swapper extends Controller implements Serializable {

    private static final long serialVersionUID = -5601769449081489865L;
    private Link myLink;

    private List<Long> VehTypesIn;
    private List<Long> VehTypesOut;
    private ActuatorVehType myActuator;

    private List<BeatsTimeProfileDouble> switchRatioContent;

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_VehType_Swapper(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.Vehicle_Type_Swapper);
    }


    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbO) {

        super.populate(jaxbO);

        List<Parameter> jaxbParams = this.getJaxbController().getParameters().getParameter();
        for( Parameter param : jaxbParams) {
            if( param.getName().toLowerCase().equals("link_id") || param.getName().toLowerCase().equals("link")) {
                myLink = myScenario.get.linkWithId(Long.parseLong(param.getValue()));
                break;
            }
        }

        List<SwitchRatio> jaxbSwitchRatios = getJaxbController().getSwitchRatio();

        VehTypesIn = new ArrayList<Long>(jaxbSwitchRatios.size());
        VehTypesOut = new ArrayList<Long>(jaxbSwitchRatios.size());
        switchRatioContent = new ArrayList<BeatsTimeProfileDouble>(jaxbSwitchRatios.size());


        // extract switch ratio info
        for(int i = 0; i< jaxbSwitchRatios.size(); i++) {
            SwitchRatio sr = jaxbSwitchRatios.get(i);
            VehTypesIn.add( sr.getVehicleTypeIn() );
            VehTypesOut.add( sr.getVehicleTypeOut() );
            switchRatioContent.add( new BeatsTimeProfileDouble( sr.getContent(), ",", sr.getDt(), sr.getStartTime(),
                    myScenario.get.simdtinseconds() ));
        }

        // create the actuator
        edu.berkeley.path.beats.jaxb.Actuator jaxbA = new edu.berkeley.path.beats.jaxb.Actuator();
        edu.berkeley.path.beats.jaxb.ScenarioElement se = new edu.berkeley.path.beats.jaxb.ScenarioElement();
        edu.berkeley.path.beats.jaxb.ActuatorType at = new edu.berkeley.path.beats.jaxb.ActuatorType();
        se.setId(myLink.getId());
        se.setType("link");
        at.setId(-1);
        at.setName("vehtype_changer");
        jaxbA.setId(-1);
        jaxbA.setScenarioElement(se);
        jaxbA.setActuatorType(at);
        myActuator = new ActuatorVehType(myScenario,jaxbA,new BeatsActuatorImplementation(jaxbA,myScenario));
        myActuator.populate(null,null);
        myActuator.setMyController(this);

//        LinkRefs = new HashMap<Link, LinkRef>();
//
//        Table commodityTable = getTables().get("swap_profiles");
//        for(Table.Row row : commodityTable.getRows()){
//            Integer comm_in_index = Integer.parseInt(row.get_value_for_column_name("comm_in"));
//            Integer comm_out_index = Integer.parseInt(row.get_value_for_column_name("comm_out"));
//            Double dt = Double.parseDouble(row.get_value_for_column_name("dt"));
//            Long link_id = Long.parseLong(row.get_value_for_column_name("link_id"));
//            String proportion_string = row.get_value_for_column_name("proportion_string");
//
//            String start_time_string = row.get_value_for_column_name("start_time");
//            // start time defaults to 0 if none specified
//            Double start_time = start_time_string == null ? 0 : Double.parseDouble(start_time_string);
//
//            Link link = myScenario.get.linkWithId(link_id);
//
//            if (!proportion_string.isEmpty() && link != null && !Double.isNaN(dt) && Double.isNaN(start_time)
//                    && myScenario.get.numVehicleTypes() > comm_in_index && myScenario.get.numVehicleTypes() > comm_out_index) {
//                if (!LinkRefs.containsKey(link)) {
//                    LinkRef linkref = new LinkRef(this, link, comm_in_index, comm_out_index, proportion_string, dt, start_time);
//                    LinkRefs.put(link, linkref);
//                } else {
//                    LinkRefs.get(link).addSeries(comm_in_index, comm_out_index, proportion_string, dt, start_time);
//                }
//            }

        }

    @Override
    public boolean register() {
        return myActuator.register();
    }

    @Override
    protected void validate() {
        super.validate();
        for( BeatsTimeProfileDouble src : switchRatioContent)
            src.validate();
    }

    @Override
    protected void reset()  {
        super.reset();
        for( BeatsTimeProfileDouble src : switchRatioContent)
            src.reset();

        try {
            myActuator.reset();
        } catch (BeatsException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        Clock clock = myScenario.get.clock();

        for( BeatsTimeProfileDouble series : switchRatioContent)
            series.sample(false, clock);

        deploy();
    }

    public void deploy() {
        for( int i=0;i<switchRatioContent.size();i++) {
            myActuator.set_switch_ratio(getVehTypesIn().get(i), getVehTypesOut().get(i),
                    switchRatioContent.get(i).getCurrentSample());
        }

        myActuator.deploy(myScenario.get.currentTimeInSeconds());
    }

    public List<Long> getVehTypesIn() {
        return VehTypesIn;
    }

    public List<Long> getVehTypesOut() {
        return VehTypesOut;
    }

    public ActuatorVehType getMyActuator() {
        return myActuator;
    }

    public Link getMyLink() {
        return myLink;
    }
}