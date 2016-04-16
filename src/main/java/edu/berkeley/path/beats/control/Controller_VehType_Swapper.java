package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorVehType;
import edu.berkeley.path.beats.jaxb.Parameter;
import edu.berkeley.path.beats.jaxb.SwitchRatio;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
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
    private ActuatorVehType myActuator;

    private List<switchRatioAbstract> switchRatios;

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

        switchRatios = new ArrayList<switchRatioAbstract>(jaxbSwitchRatios.size());

        // extract switch ratio info
        for(int i = 0; i< jaxbSwitchRatios.size(); i++) {
            SwitchRatio sr = jaxbSwitchRatios.get(i);
            if(sr.getReferenceInlink() != null && sr.getReferenceOutlink() != null && sr.getReferenceVehtype() != null) {
                switchRatios.add( new switchRatioFromReferenceSplitRatio(sr));
            }
            else {
                switchRatios.add( new switchRatioFromProfile(sr));
            }
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

    protected abstract class switchRatioAbstract {

        protected long vehTypeIn;
        protected long vehTypeOut;

        protected abstract void validate();
        protected abstract void reset();
        protected abstract void update(Clock clock);
        protected abstract double getSwitchRatioValue();

        public long getVehTypeOut() {
            return vehTypeOut;
        }

        public long getVehTypeIn() {
            return vehTypeIn;
        }
    }

    protected class switchRatioFromProfile extends switchRatioAbstract {

        protected BeatsTimeProfileDouble content;

        public switchRatioFromProfile(SwitchRatio jaxbSwitchRatio) {

            vehTypeIn = jaxbSwitchRatio.getVehicleTypeIn();
            vehTypeOut = jaxbSwitchRatio.getVehicleTypeOut();

            content = new BeatsTimeProfileDouble( jaxbSwitchRatio.getContent(), ",", jaxbSwitchRatio.getDt(),
                    jaxbSwitchRatio.getStartTime(), myScenario.get.simdtinseconds());

        }

        protected void validate() {
            content.validate();
        }

        protected void reset() {
            content.reset();
        }

        protected void update(Clock clock) {
            content.sample(false, clock);
        }

        protected double getSwitchRatioValue() {
            return content.getCurrentSample();
        }
    }

    protected class switchRatioFromReferenceSplitRatio extends switchRatioAbstract {

        protected int refInLinkIndex;
        protected int refOutLinkIndex;
        protected int refVehTypeIndex;

        protected Node refNode;

        public switchRatioFromReferenceSplitRatio(SwitchRatio jaxbSwitchRatio) {
            vehTypeIn = jaxbSwitchRatio.getVehicleTypeIn();
            vehTypeOut = jaxbSwitchRatio.getVehicleTypeOut();

            long refInLink = Long.parseLong(jaxbSwitchRatio.getReferenceInlink());
            long refOutLink = Long.parseLong(jaxbSwitchRatio.getReferenceOutlink());
            long refVehType = Long.parseLong(jaxbSwitchRatio.getReferenceVehtype());

            refNode = myScenario.get.linkWithId(refInLink).getEnd_node();

            refInLinkIndex = refNode.getInputLinkIndex(refInLink);
            refOutLinkIndex = refNode.getOutputLinkIndex(refOutLink);
            refVehTypeIndex = myScenario.get.vehicleTypeIndexForId(refVehType);

        }

        protected void reset() {} // nothing to reset

        protected void validate() {
            if(refInLinkIndex==-1 || refOutLinkIndex==-1) { // means getInputLinkIndex or getOutputLinkIndex could not find the correct link
                BeatsErrorLog.addError("Bad input/output reference link pair in switch ratio for controller id="
                        + Controller_VehType_Swapper.this.getId() + ".");
            }

            if(refVehTypeIndex == -1) {
                BeatsErrorLog.addError("Bad reference vehicle type index in switch ratio for controller id="
                        + Controller_VehType_Swapper.this.getId() + ".");
            }
        }

        protected void update(Clock clock) {}

        protected double getSwitchRatioValue() {
            refNode.sample_split_ratio_profile();
            refNode.sample_split_controller();
            Double[][][] splitratio = BeatsMath.copy(refNode.getSplitRatio());

            if( Double.isNaN(splitratio[refInLinkIndex][refOutLinkIndex][refVehTypeIndex]))
                splitratio = refNode.node_behavior.sr_solver.computeAppliedSplitRatio(splitratio, 0);

            return splitratio[refInLinkIndex][refOutLinkIndex][refVehTypeIndex];
        }
    }



    @Override
    public boolean register() {
        return myActuator.register();
    }

    @Override
    protected void validate() {
        super.validate();
        for( switchRatioAbstract src : switchRatios)
            src.validate();
    }

    @Override
    protected void reset()  {
        super.reset();
        for( switchRatioAbstract src : switchRatios)
            src.reset();

        try {
            myActuator.reset();
        } catch (BeatsException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        Clock clock = myScenario.get.clock();

        for( switchRatioAbstract sr : switchRatios)
            sr.update(clock);

        deploy();
    }

    public void deploy() {
        for( int i=0;i<switchRatios.size();i++) {
            myActuator.set_switch_ratio(
                    switchRatios.get(i).getVehTypeIn(), switchRatios.get(i).getVehTypeOut(),
                    switchRatios.get(i).getSwitchRatioValue());
        }

        myActuator.deploy(myScenario.get.currentTimeInSeconds());
    }

    public ActuatorVehType getMyActuator() {
        return myActuator;
    }

    public Link getMyLink() {
        return myLink;
    }

    public List<switchRatioAbstract> getSwitchRatios() {
        return switchRatios;
    }
}