package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorCommodity;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsTimeProfileDouble;
import edu.berkeley.path.beats.simulator.utils.Table;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gomes on 6/4/2015.
 */
public class Controller_Commodity_Swapper extends Controller {

    private HashMap<Link,LinkRef> LinkRefs;

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_Commodity_Swapper(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.Commodity_Swapper);
    }


    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbO) {

        super.populate(jaxbO);

        LinkRefs = new HashMap<Link, LinkRef>();

        Table commodityTable = getTables().get("swap_profiles");
        for(Table.Row row : commodityTable.getRows()){
            Integer comm_in_index = Integer.parseInt(row.get_value_for_column_name("comm_in"));
            Integer comm_out_index = Integer.parseInt(row.get_value_for_column_name("comm_out"));
            Double dt = Double.parseDouble(row.get_value_for_column_name("dt"));
            Long link_id = Long.parseLong(row.get_value_for_column_name("link_id"));
            String proportion_string = row.get_value_for_column_name("proportion_string");

            String start_time_string = row.get_value_for_column_name("start_time");
            // start time defaults to 0 if none specified
            Double start_time = start_time_string == null ? 0 : Double.parseDouble(start_time_string);

            Link link = myScenario.get.linkWithId(link_id);

            if (!proportion_string.isEmpty() && link != null && !Double.isNaN(dt) && Double.isNaN(start_time)
                    && myScenario.get.numVehicleTypes() > comm_in_index && myScenario.get.numVehicleTypes() > comm_out_index) {
                if (!LinkRefs.containsKey(link)) {
                    LinkRef linkref = new LinkRef(this, link, comm_in_index, comm_out_index, proportion_string, dt, start_time);
                    LinkRefs.put(link, linkref);
                } else {
                    LinkRefs.get(link).addSeries(comm_in_index, comm_out_index, proportion_string, dt, start_time);
                }
            }

        }
    }

    @Override
    protected void validate() {
        super.validate();
        for (LinkRef lr : LinkRefs.values())
            lr.validate();
    }

    @Override
    protected void reset()  {
        super.reset();
    }

    protected class LinkRef {
        private Controller_Commodity_Swapper myController;
        private Link myLink;
        private ActuatorCommodity myActuator;

        private ArrayList<BeatsTimeProfileDouble> proportionSeries;
        private ArrayList<Integer> comm_in;
        private ArrayList<Integer> comm_out;


        private LinkRef(Controller_Commodity_Swapper parent, Link link, int this_comm_in, int this_comm_out,
                        String prop_string, double dt, double start_time) {
            myController = parent;
            myLink = link;

            comm_in = new ArrayList<Integer>();
            comm_out = new ArrayList<Integer>();
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
            myActuator = new ActuatorCommodity(myScenario,jaxbA,new BeatsActuatorImplementation(jaxbA,myScenario));
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
            int numswitches = proportionSeries.size();
            for( int i=0;i<numswitches;i++) {
                proportionSeries.get(i).sample(false,clock);


            }
        }
    }

}
