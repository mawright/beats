package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorCMS;
import edu.berkeley.path.beats.jaxb.Column;
import edu.berkeley.path.beats.jaxb.Row;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.*;

import java.io.Serializable;
import java.util.*;

/**
 * HOVs and GP feed the offramp
 */
public class Controller_SR_Generator_new extends Controller implements Serializable {

    private static final long serialVersionUID = 1775010600579864073L;

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
                node_data.put(link.getId(), createNodeData(link, demandString, knob, dpdt, start_time));
        }
    }

    protected NodeData createNodeData(Link link, String demandString, double knob, double dpdt, double start_time) {
        return new NodeData(this, link, demandString, knob, dpdt, start_time, myScenario);
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

    class NodeData implements Serializable {

        protected Node myNode;
        protected ActuatorCMS cms;
        protected double knob;
        protected double beta;
        protected ArrayList<Boolean> is_feed;
        protected ArrayList<Boolean> is_measured;
        protected BeatsTimeProfileDouble measured_flow_profile_veh;
        protected Double measured_flow_veh;

        public NodeData(Controller parent,Link profileLink,String demandStr,Double knob,Double dpdt, Double start_time, Scenario scenario) {

            this.knob = knob;
            this.myNode = profileLink.getBegin_node();
            is_feed = new ArrayList<Boolean>();
            is_measured = new ArrayList<Boolean>();

            //  incoming link sets
            for (int i = 0; i < myNode.getnIn(); i++) {
                Link link = myNode.getInput_link()[i];
                is_feed.add( link.isFreeway() || link.isHov() );
            }

            // outgoing link sets
            boolean have_measured = false;
            for (int j = 0; j < myNode.getnOut(); j++) {
                Link link = myNode.getOutput_link()[j];
                is_measured.add(link==profileLink);
                have_measured |= link==profileLink;
            }

            if (!have_measured)
                return;

            // find the demand profile for the offramp
            measured_flow_profile_veh = new BeatsTimeProfileDouble(demandStr, ",", dpdt, start_time, scenario.get.simdtinseconds());
            measured_flow_profile_veh.multiplyscalar(scenario.get.simdtinseconds());

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
//            if(not_meas.size()!=1 )
//                BeatsErrorLog.addError("This case is not correctly implemented yet.");
        }

        public void reset() {
            measured_flow_profile_veh.reset();
            measured_flow_veh = 0d;
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
                measured_flow_veh = measured_flow_profile_veh.getCurrentSample()*knob;

            if(BeatsMath.equals(measured_flow_veh, 0d)){
                beta = 0d;
                return;
            }

            // Collect demands from feeding and non-feeding input links
            double [] input_demand = new double[myNode.nIn];
            double input_demand_feed = 0d;
            for(i=0;i<myNode.nIn;i++) {
                double Si = BeatsMath.sum(myNode.input_link[i].get_out_demand_in_veh(e));
                input_demand[i] = Si;
                if(is_feed.get(i))
                    input_demand_feed += Si;
            }

            if(BeatsMath.equals(input_demand_feed, 0d)){
                beta = 0d;
                return;
            }

            double beta_freeflow = measured_flow_veh / input_demand_feed;

            // compute demand to non-measured, total $+phi, and from feed links phi
            ArrayList<Double> beta_array = new ArrayList<Double>();

            // freeflow case
            beta_array.add(0d);
            beta_array.add(beta_freeflow);

            // rest
            for (j = 0; j < myNode.nOut; j++) {
                Link outlink = myNode.output_link[j];
                if(!is_measured.get(j)) {

                    double output_dem_feed = 0d; // demand on j from feeding links
                    double output_dem_non_feed = 0d; // demand on j from non-feeding links

                    for(i=0;i<myNode.nIn;i++) {
                        double Sij = myNode.getSplitRatio(i,j)*input_demand[i];
                        if (is_feed.get(i))
                            output_dem_feed += Sij;
                        else
                            output_dem_non_feed += Sij;
                    }

                    Double R = outlink.get_available_space_supply_in_veh(e);
                    double num = beta_freeflow*(output_dem_non_feed+output_dem_feed);
                    double den = R + beta_freeflow*output_dem_feed;

                    beta_array.add( den>0 ? num / den : Double.POSITIVE_INFINITY );
                }
            }

            beta = Math.min( BeatsMath.max(beta_array) , 1d );
        }

        public void deploy(double current_time_in_seconds){
            int i,j;
            for(i=0;i<myNode.nIn;i++){
                Link inlink = myNode.input_link[i];
                if(is_feed.get(i)){
                    for(j=0;j<myNode.nOut;j++){
                        Link outlink = myNode.output_link[j];
                        if(is_measured.get(j)) // measured gets beta
                            cms.set_split(inlink.getId(),outlink.getId(),beta);
                        else    // not measured scaled to 1-beta
                            cms.set_split(inlink.getId(), outlink.getId(), myNode.getSplitRatio(i, j)*(1d-beta));
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
