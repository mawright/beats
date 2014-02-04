package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorCMS;
import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.jaxb.DemandProfile;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.Actuator;
import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.DemandSet;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Parameters;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.ScenarioElement;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/31/14.
 */
public class Controller_SR_Generator extends Controller {

    protected List<NodeData> node_data;

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_SR_Generator(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.SR_Generator);
    }

    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbobject) {

        // load offramp flow information
        Parameters param = (Parameters) ((edu.berkeley.path.beats.jaxb.Controller)jaxbobject).getParameters();
        String configfilename = param.get("fr_flow_file");

        JAXBContext context;
        Unmarshaller u = null;

        // create unmarshaller .......................................................
        try {
            //Reset the classloader for main thread; need this if I want to run properly
            //with JAXB within MATLAB. (luis)
            Thread.currentThread().setContextClassLoader(ObjectFactory.class.getClassLoader());
            context = JAXBContext.newInstance("edu.berkeley.path.beats.jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
            System.err.print("Failed to create context for JAXB unmarshaller");
        }

        // schema assignment ..........................................................
        try{
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            ClassLoader classLoader = ObjectFactory.class.getClassLoader();
            Schema schema = factory.newSchema(classLoader.getResource("beats.xsd"));
            u.setSchema(schema);
        } catch(SAXException e){
            System.err.print("Schema not found");
        }

        // process configuration file name ...........................................
        if(!configfilename.endsWith(".xml"))
            configfilename += ".xml";

        // read and return ...........................................................
        DemandSet demand_set = null;
        try {
            ObjectFactory.setObjectFactory(u, new JaxbObjectFactory());
            demand_set = (DemandSet) u.unmarshal( new FileInputStream(configfilename) );
        } catch( JAXBException je ) {
            System.err.print("JAXB threw an exception when loading the configuration file");
            System.err.println(je);
        } catch (FileNotFoundException e) {
            System.err.print("Configuration file not found");
        }

        if(demand_set==null)
            return;

        node_data = new ArrayList<NodeData>();
        for(Actuator act:actuators){
            ScenarioElement se = (ScenarioElement) act.getScenarioElement();

            if(se.getMyType().compareTo(ScenarioElement.Type.node)!=0)
                continue;

            node_data.add(new NodeData(demand_set,(Node) se.getReference()));
        }
    }

    @Override
    protected void validate() {

        // check node data
        for(Actuator act:actuators){
            ScenarioElement se = (ScenarioElement) act.getScenarioElement();
            if(se.getMyType().compareTo(ScenarioElement.Type.node)!=0)
                BeatsErrorLog.addError("In Controller_SR_Generator, all actuators must be on nodes.");
        }

        for(NodeData nd : node_data){
            if(nd.link_fw_dn.size()!=1)
                BeatsErrorLog.addError("In Controller_SR_Generator, ,ust have exactly one downstream mainline link.");
            if(nd.link_fw_up.size()!=1)
                BeatsErrorLog.addError("In Controller_SR_Generator, ,ust have exactly one upstream mainline link.");
            if(nd.link_fr.size()<1)
                BeatsErrorLog.addError("In Controller_SR_Generator, ,ust have at least one offramp link.");
        }

    }

    @Override
    protected void reset() {
        super.reset();
    }

    @Override
    protected void update() throws BeatsException {

        for(int i=0;i<node_data.size();i++){
            NodeData nd = node_data.get(i);
            double fr_flow = nd.get_fr_flow_at_time(myScenario.getClock().getStartTime());
            double ml_up_demand = BeatsMath.sum(nd.link_fw_up.get(0).getOutflowDemand(0));
            double ml_dn_supply = 0d;
            for(Link link : nd.link_fr)
                ml_dn_supply += link.getSpaceSupply(0);
            double ml_up_flow = Math.min( ml_up_demand , ml_dn_supply + fr_flow );

//            double beta;
//            for(Link link : nd.link_fr){
//
//            }
//
//
//
//            double beta = Math.min( fr_flow / ml_up_flow , 1d );
//
//
//
//
//
//            for(VehicleType vt : myScenario.getVehicleTypeSet().getVehicleType()){
//                ((ActuatorCMS)actuators.get(i)).set_split( nd.link_fw_up.get(0).getId() ,
//                                              nd.link_fr.getId(),
//                                              vt.getId(),
//                                              beta);
//            }
        }
    }

    class NodeData {

        int step_initial_abs;
        boolean isdone;
        double current_value;

        private List<BeatsTimeProfile> fr_flow;	// [veh] demand profile per vehicle type

        protected List<Link> link_fw_up;
        protected List<Link> link_fw_dn;
        protected List<Link> link_fr;

        public NodeData(DemandSet demand_set,Node myNode){

            link_fw_up = new ArrayList<Link>();
            for(Link link : myNode.getInput_link())
                if(link.isFreeway())
                    link_fw_up.add(link);

            link_fw_dn = new ArrayList<Link>();
            link_fr = new ArrayList<Link>();
            for(Link link : myNode.getOutput_link()){
                if(link.isFreeway())
                    link_fw_dn.add(link);
                if(link.isOfframp())
                    link_fr.add(link);
            }

            // find the demand profile for the offramps
            fr_flow = new ArrayList<BeatsTimeProfile>();
            List<Double> start_time = new ArrayList<Double>();
            for(Link link : link_fr){
                DemandProfile dp = demand_set.get_demand_profile_for_link_id(link.getId());
                fr_flow.add(new BeatsTimeProfile(dp.getDemand().get(0).getContent(),true));
                start_time.add(Double.isInfinite(dp.getStartTime()) ? 0d : dp.getStartTime());
            }

            // check all starttimes are the same
            boolean all_same = true;
            if(!start_time.isEmpty()){
                double first = start_time.get(0);
                for(Double d : start_time)
                    if(d!=first)
                        all_same = false;
            }
            if(!all_same)
                start_time = null;
            else{
                step_initial_abs = BeatsMath.round(start_time.get(0)/myScenario.getSimdtinseconds());
                isdone = false;
                current_value = 0d;
            }

        }

        protected double [] get_fr_flow_at_time(double time_in_seconds){

            if( !isdone && myScenario.getClock().is_time_to_sample_abs(samplesteps, step_initial_abs)){

                // REMOVE THESE
                int n = fr_flow.getNumTime()-1;
                int step = myScenario.getClock().sample_index_abs(samplesteps,step_initial_abs);

                // demand is zero before step_initial_abs
                if(myScenario.getClock().getAbsoluteTimeStep()< step_initial_abs)
                    current_value = 0d;

                // sample the profile
                if(step<n){
                    current_value = fr_flow.get(myScenario.getClock().sample_index_abs(samplesteps,step_initial_abs));
                }

                // last sample
                if(step>=n && !isdone){
                    isdone = true;
                    current_value = fr_flow.get(n);
                }
            }

            current_value = Math.abs(current_value);

            return current_value;
        }


    }

}
