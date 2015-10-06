package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorCMS;
import edu.berkeley.path.beats.jaxb.Column;
import edu.berkeley.path.beats.jaxb.Parameter;
import edu.berkeley.path.beats.jaxb.Row;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.*;

import java.util.*;

/**
 * HOVs and GP feed the offramp
 */
public class Controller_SR_Generator_new extends Controller {

//    private List<NodeData> node_data;
    private HashMap<Long,NodeData> node_data;

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_SR_Generator_new(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.SR_Generator);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbO) {

        edu.berkeley.path.beats.jaxb.Controller jaxbC = (edu.berkeley.path.beats.jaxb.Controller) jaxbO;

        node_data = new HashMap<Long,NodeData>();

        for( Row row : jaxbController.getTable().get(0).getRow() ){

            Link link = null;
            String demandString = "";
            double dpdt = Double.NaN;
            double knob = Double.NaN;
            double start_time = Double.NaN;

            for( Column col : row.getColumn() ){

                switch( (int) col.getId() ){

                    case 0:
                        link = getMyScenario().get.linkWithId(Integer.parseInt(col.getContent()));
                        break;

                    case 1:
                        demandString = col.getContent();
                        break;

                    case 2:
                        dpdt = Double.parseDouble(col.getContent());
                        break;

                    case 3:
                        knob = Double.parseDouble(col.getContent());
                        break;

                    case 4:
                        start_time = Double.parseDouble(col.getContent());
                }
            }

            if (!demandString.isEmpty() && link != null && !Double.isNaN(knob) && !Double.isNaN(dpdt) && !Double.isNaN(start_time))
                node_data.put(link.getId(), new NodeData(this, link, demandString, knob, dpdt, start_time,myScenario));
        }


    }

    @Override
    public boolean register() {
        for(NodeData nd : node_data.values())
            if(!nd.cms.register())
                return false;
        return true;
    }

    @Override
    protected void validate() {
        super.validate();

        for(NodeData node : node_data.values())
            node.validate();
    }

    @Override
    protected void reset()  {
        super.reset();

        if(node_data==null)
            return;

        for(NodeData node : node_data.values())
            node.reset();
    }

    @Override
    protected void update() throws BeatsException {

        if(node_data==null)
            return;

        for(NodeData nd : node_data.values()){
            nd.update(myScenario.get.clock());
            nd.deploy(myScenario.get.currentTimeInSeconds());
        }
    }

    /////////////////////////////////////////////////////////////////////
    // api
    /////////////////////////////////////////////////////////////////////

    public void set_knob_for_link(Long link_id,double newknob){
        node_data.get(link_id).set_knob(newknob);
    }

    /////////////////////////////////////////////////////////////////////
    // inner class
    /////////////////////////////////////////////////////////////////////

    class NodeData {

        private Node myNode;
        private ActuatorCMS cms;
        private double knob;
        protected double beta;
        private Link meas;                      // the measured outgoing link
        private ArrayList<Link> not_meas;       // the unmeasured outgoing links
        private ArrayList<Link> not_feeds;      // incoming that don't feed meas
        private ArrayList<Link> feeds;          // incoming that feed meas
        private BeatsTimeProfileDouble measured_flow_profile_veh;
        private Double current_flow_veh;
//        private Double [] alpha_tilde; // row sum of splits from feeding to non-measured links

        public NodeData(Controller parent,Link profileLink,String demandStr,Double knob,Double dpdt, Double start_time, Scenario scenario) {

            this.knob = knob;
            this.myNode = profileLink.getBegin_node();
            meas = profileLink;
            feeds = new ArrayList<Link>();
            not_feeds = new ArrayList<Link>();

            //  incoming link sets
            for (int i = 0; i < myNode.getnIn(); i++) {
                Link link = myNode.getInput_link()[i];
                if (link.isFreeway() || link.isHov())
                    feeds.add(link);
                else
                    not_feeds.add(link);
            }

            // outgoing link sets
            not_meas = new ArrayList<Link>();
            for (int j = 0; j < myNode.getnOut(); j++) {
                Link link = myNode.getOutput_link()[j];
                if (meas != link)
                    not_meas.add(link);
            }

            if (meas == null)
                return;

            // find the demand profile for the offramp
            measured_flow_profile_veh = new BeatsTimeProfileDouble(demandStr, ",", dpdt, start_time, scenario.get.simdtinseconds());
            measured_flow_profile_veh.multiplyscalar(scenario.get.simdtinseconds());

//            alpha_tilde = new Double[myNode.nIn];

            // create the actuator
            edu.berkeley.path.beats.jaxb.Actuator jaxbA = new edu.berkeley.path.beats.jaxb.Actuator();
            edu.berkeley.path.beats.jaxb.ScenarioElement se = new edu.berkeley.path.beats.jaxb.ScenarioElement();
            edu.berkeley.path.beats.jaxb.ActuatorType at = new edu.berkeley.path.beats.jaxb.ActuatorType();
            se.setId(myNode.getId());
            se.setType("node");
            at.setId(-1);
            at.setName("cms");
            jaxbA.setId(-1);
            jaxbA.setScenarioElement(se);
            jaxbA.setActuatorType(at);
            cms = new ActuatorCMS(scenario,jaxbA,new BeatsActuatorImplementation(jaxbA,scenario));
            cms.populate(null,null);
            cms.setMyController(parent);
        }

        public void validate(){
            if(not_meas.size()!=1 )
                BeatsErrorLog.addError("This case is not correctly implemented yet.");
        }

        public void reset() {
            measured_flow_profile_veh.reset();
            current_flow_veh = 0d;
            try {
                cms.reset();
            } catch (BeatsException e) {
                e.printStackTrace();
            }
        }

        public void update(Clock clock){

            int i,j;
            int e = 0;

            if(measured_flow_profile_veh.sample(false,clock))
                current_flow_veh = measured_flow_profile_veh.getCurrentSample()*knob;

            if(BeatsMath.equals(current_flow_veh, 0d)){
                beta = 0d;
                return;
            }

            // Sf: total demand from "feeds"
            Double Sf = 0d;
            for(Link link : feeds)
                Sf += BeatsMath.sum(link.get_out_demand_in_veh(e));

            // compute demand to non-measured, total $+phi, and from feed links phi
            ArrayList<Double> beta_array = new ArrayList<Double>();

            // compute sr normalization factor for feeding links
//            for(i=0;i<myNode.getInput_link().length;i++) {
//                if (!feeds.contains( myNode.input_link[i]))
//                    continue;
//                alpha_tilde[i] = 0d;
//                for (j = 0; j < myNode.nOut; j++)
//                    if(not_meas.contains( myNode.output_link[j]))
//                        alpha_tilde[i] += myNode.getSplitRatio(i, j);
//            }

            // freeflow case
            beta_array.add(0d);
            beta_array.add(current_flow_veh / Sf);

            // rest
            for (j = 0; j < myNode.nOut; j++) {
                Link outlink = myNode.output_link[j];

                // case for the measured
                if(not_meas.contains(outlink)) {

                    double dem_non_feed = 0d; // demand on j from non-feeding links
                    double dem_feed = 0d; // demand on j from feeding links

                    for(i=0;i<myNode.nIn;i++) {
                        Link inlink = myNode.input_link[i];
//                        Double alpha_ij = myNode.getSplitRatio(i,j);
                        Double Si = BeatsMath.sum(inlink.get_out_demand_in_veh(e));
                        if (feeds.contains(inlink))
                            dem_feed += Si; //alpha_ij * Si / alpha_tilde[i];
                        else //otherwise add to total
                            dem_non_feed += 0d; //alpha_ij * Si;
                    }

                    Double R = outlink.get_available_space_supply_in_veh(e);

                    double num = current_flow_veh*(dem_non_feed+dem_feed);
                    double den = Sf*R + dem_feed*current_flow_veh;

                    beta_array.add( den>0 ? num / den : Double.POSITIVE_INFINITY );
                }
            }

            beta = Math.min( BeatsMath.max(beta_array) , 1d );
        }

        public void deploy(double current_time_in_seconds){

            int i,j;
            for(i=0;i<myNode.nIn;i++){
                Link inlink = myNode.input_link[i];
                if(feeds.contains(inlink)){

                    for(j=0;j<myNode.nOut;j++){
                        Link outlink = myNode.output_link[j];

                        // measured gets beta
                        if(meas==outlink)
                            cms.set_split(inlink.getId(),meas.getId(),beta);

                        // not measured scaled to 1-beta
                        else
                            cms.set_split(inlink.getId(), outlink.getId(), 1d-beta );
//                            cms.set_split(inlink.getId(), outlink.getId(), myNode.getSplitRatio(i, j)*(1d-beta)/alpha_tilde[i]);
                    }
                }
            }

            cms.deploy(current_time_in_seconds);

        }

        protected void set_knob(double newknob){
            knob = newknob;
        }

    }

}
