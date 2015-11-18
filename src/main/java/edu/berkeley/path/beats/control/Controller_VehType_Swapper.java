package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorVehType;
import edu.berkeley.path.beats.jaxb.Parameter;
import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileDouble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gomes on 6/4/2015.
 */
public class Controller_VehType_Swapper extends Controller {

    private HashMap<Link,LinkRef> LinkRefs;
    private Link myLink;

    private List<Integer> VehTypesIn;
    private List<Integer> VehTypesOut;
    private List<SwitchRatio> jaxbSwitchRatios;
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

        jaxbSwitchRatios = getJaxbController().getSwitchRatio();

        VehTypesIn = new ArrayList<Integer>(jaxbSwitchRatios.size());
        VehTypesOut = new ArrayList<Integer>(jaxbSwitchRatios.size());
        switchRatioContent = new ArrayList<BeatsTimeProfileDouble>(jaxbSwitchRatios.size());


        // extract switch ratio info
        for(int i = 0; i< jaxbSwitchRatios.size(); i++) {
            SwitchRatio sr = jaxbSwitchRatios.get(i);
            VehTypesIn.add( myScenario.get.vehicleTypeIndexForId( sr.getVehicleTypeIn() ) );
            VehTypesOut.add( myScenario.get.vehicleTypeIndexForId( sr.getVehicleTypeOut() ) );
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

    }

    public List<Integer> getVehTypesIn() {
        return VehTypesIn;
    }

    public List<Integer> getVehTypesOut() {
        return VehTypesOut;
    }

    protected class LinkRef {
        private Controller_VehType_Swapper myController;
        private Link myLink;
        private ActuatorVehType myActuator;

        private ArrayList<BeatsTimeProfileDouble> proportionSeries;
        private ArrayList<Integer> comm_in;
        private ArrayList<Integer> comm_out;
        private ArrayList<List<Double>> amount_to_switch;


        private LinkRef(Controller_VehType_Swapper parent, Link link, int this_comm_in, int this_comm_out,
                        String prop_string, double dt, double start_time) {
            myController = parent;
            myLink = link;

            comm_in = new ArrayList<Integer>();
            comm_out = new ArrayList<Integer>();
            amount_to_switch = new ArrayList<List<Double>>();
            proportionSeries = new ArrayList<BeatsTimeProfileDouble>();

            comm_in.add(this_comm_in);
            comm_out.add(this_comm_out);
            proportionSeries.add(new BeatsTimeProfileDouble(prop_string, ",", dt, start_time,
                    myScenario.get.simdtinseconds()));

            // create the actuator
            edu.berkeley.path.beats.jaxb.Actuator jaxbA = new edu.berkeley.path.beats.jaxb.Actuator();
            edu.berkeley.path.beats.jaxb.ScenarioElement se = new edu.berkeley.path.beats.jaxb.ScenarioElement();
            edu.berkeley.path.beats.jaxb.ActuatorType at = new edu.berkeley.path.beats.jaxb.ActuatorType();
            se.setId(myLink.getId());
            se.setType("link");
            at.setId(-1);
            at.setName("commodity");
            jaxbA.setId(-1);
            jaxbA.setScenarioElement(se);
            jaxbA.setActuatorType(at);
            myActuator = new ActuatorVehType(myScenario,jaxbA,new BeatsActuatorImplementation(jaxbA,myScenario));
            myActuator.populate(null,null);
            myActuator.setMyController(parent);

        }

        private void addSeries(int this_comm_in, int this_comm_out, String prop_string, double dt, double start_time) {
            comm_in.add(this_comm_in);
            comm_out.add(this_comm_out);
            proportionSeries.add(new BeatsTimeProfileDouble(prop_string,",", dt, start_time,
                    myScenario.get.simdtinseconds()));
        }

        public void validate(){

        }

        public void reset() {
            for( BeatsTimeProfileDouble series : proportionSeries)
                series.reset();
            try {
                myActuator.reset();
            } catch( Exception e) {
                e.printStackTrace();
            }
        }

        public void update(Clock clock) {
            for( BeatsTimeProfileDouble series : proportionSeries) {
                series.sample(false, clock);
            }
        }

        public void deploy(double current_time_in_seconds) {
            int numswitches = proportionSeries.size();
            int ensembleSize = myScenario.get.numEnsemble();

            for( int i=0; i<numswitches; i++) {
                double switchProportion = proportionSeries.get(i).getCurrentSample();
            }
        }
    }
}
