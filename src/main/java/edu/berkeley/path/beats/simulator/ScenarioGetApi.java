package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.actuator.ActuatorSignal;
import edu.berkeley.path.beats.jaxb.Density;
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ScenarioGetApi implements Serializable {

    private static final long serialVersionUID = 1182354562760868158L;

    Scenario scenario;

    public ScenarioGetApi(Scenario scenario){
        this.scenario = scenario;
    }

    /* SIMULATION PARAMETERS ------------------------------------ */

    public String outputPrefix(){
        return scenario.runParam.outprefix;
    }

    public TypeUncertainty uncertaintyModel() {
        return  scenario.uncertaintyModel;
    }

    public double global_demand_knob() {
        return  scenario.global_demand_knob;
    }

    public double std_dev_flow() {
        return  scenario.std_dev_flow;
    }

    public boolean has_flow_unceratinty() {
        return  scenario.has_flow_unceratinty;
    }

    public double currentTimeInSeconds() {
        if( scenario.clock==null)
            return Double.NaN;
        return  scenario.clock.getT();
    }

    public double timeElapsedInSeconds() {
        if( scenario.clock==null)
            return Double.NaN;
        return  scenario.clock.getTElapsed();
    }

    public int numEnsemble() {
        return  scenario.runParam.numEnsemble;
    }

    public double simdtinseconds() {
        return scenario.runParam.dt_sim;
    }

    public double timeStart() {
        if(scenario.clock==null)
            return Double.NaN;
        return scenario.clock.getStartTime();
    }

    public double timeEnd() {
        if(scenario.clock==null)
            return Double.NaN;
        return scenario.clock.getEndTime();
    }

    public String configFilename() {
        return scenario.configfilename;
    }

    public String [] vehicleTypeNames(){
        int numVehTypes = this.scenario.get.numVehicleTypes();
        String [] vehtypenames = new String [numVehTypes];
        if(scenario.getVehicleTypeSet()==null || scenario.getVehicleTypeSet().getVehicleType()==null)
            vehtypenames[0] = Defaults.vehicleType;
        else
            for(int i=0;i<numVehTypes;i++)
                vehtypenames[i] = scenario.getVehicleTypeSet().getVehicleType().get(i).getName();
        return vehtypenames;
    }

    public Clock clock() {
        return scenario.clock;
    }

    public Properties auxiliary_properties(String group_name){
        return scenario.aux_props.get(group_name);
    }

    /*  VEHICLE TYPES ------------------------------------------- */

    public int numVehicleTypes() {
        return  scenario.numVehicleTypes;
    }

    public int vehicleTypeIndexForName(String name){
        if(name==null)
            return -1;
        if(scenario.getVehicleTypeSet()==null)
            return 0;
        if(scenario.getVehicleTypeSet().getVehicleType()==null)
            return 0;
        for(int i=0;i<scenario.getVehicleTypeSet().getVehicleType().size();i++)
            if(scenario.getVehicleTypeSet().getVehicleType().get(i).getName().equals(name))
                return i;
        return -1;
    }

    public int vehicleTypeIndexForId(long id){
        if(scenario.getVehicleTypeSet()==null)
            return 0;
        if(scenario.getVehicleTypeSet().getVehicleType()==null)
            return 0;
        for(int i=0;i<scenario.getVehicleTypeSet().getVehicleType().size();i++)
            if(scenario.getVehicleTypeSet().getVehicleType().get(i).getId()==id)
                return i;
        return -1;
    }

    public long vehicleTypeIdForIndex(int index){
        if(scenario.getVehicleTypeSet()==null)
            return 0;
        if(scenario.getVehicleTypeSet().getVehicleType()==null)
            return 0;
        return scenario.getVehicleTypeSet().getVehicleType().get(index).getId();
    }

    /*  NETWORK ------------------------------------------------- */

    public Network networkWithId(long id){
        List<edu.berkeley.path.beats.jaxb.Network> networks = scenario.getNetworks();
        if(networks==null)
            return null;
        if(networks.size()>1)
            return null;
        for(edu.berkeley.path.beats.jaxb.Network network : networks){
            if(network.getId()==id)
                return (Network) network;
        }
        return null;
    }

    public Link linkWithId(long id) {
        if(scenario.getNetworkSet()==null)
            return null;
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
            for(edu.berkeley.path.beats.jaxb.Link link : network.getLinkList().getLink())
                if(link.getId()==id)
                    return (Link) link;
        return null;
    }

    public Node nodeWithId(long id) {
        if(scenario.getNetworkSet()==null)
            return null;
        for(edu.berkeley.path.beats.jaxb.Network network : scenario.getNetworkSet().getNetwork())
            for(edu.berkeley.path.beats.jaxb.Node node : network.getNodeList().getNode())
                if(node.getId()==id)
                    return (Node) node;
        return null;
    }

    /*  SENSORS ------------------------------------------------- */

    public Sensor sensorWithLinkId(long link_id){
        if(scenario.sensorset==null)
            return null;
        for(edu.berkeley.path.beats.simulator.Sensor sensor : scenario.sensorset.getSensors() ){
            if(sensor.getMyLink()!=null && sensor.getMyLink().getId()==link_id)
                return sensor;
        }
        return null;
    }

    public Sensor sensorWithId(long id) {
        if(scenario.sensorset==null)
            return null;
        for(edu.berkeley.path.beats.simulator.Sensor sensor : scenario.sensorset.getSensors() ){
            if(sensor.getId()==id)
                return sensor;
        }
        return null;
    }

    public List<Sensor> sensors(){
        return scenario.sensorset==null ?
                null :
                scenario.sensorset.getSensors();
    }

    public Sensor sensorWithVDS(int vds) {
        if(scenario.sensorset==null)
            return null;
        for(edu.berkeley.path.beats.simulator.Sensor sensor : scenario.sensorset.getSensors() )
            if(sensor.get_VDS()==vds)
                return sensor;
        return null;
    }

    /*  ACTUATORS ------------------------------------------------ */

    public Actuator actuatorWithId(long id) {
        if(scenario.actuatorset==null)
            return null;
        for(edu.berkeley.path.beats.simulator.Actuator actuator : scenario.actuatorset.getActuators() ){
            if(actuator.getId()==id)
                return actuator;
        }
        return null;
    }

    public ActuatorSignal signal_for_node(long node_id){
        if(scenario.actuatorset==null)
            return null;
        for(Actuator actuator : scenario.actuatorset.getActuators()){
            if(actuator.myType==Actuator.Type.signal){
                ActuatorSignal signal = (ActuatorSignal) actuator;
                Long signal_node_id = signal.get_node_id();
                if(signal_node_id!=null && node_id==signal_node_id)
                    return signal;
            }
        }
        return null;
    }

    public List<Actuator> signal_actuators(){
        List<Actuator> x = new ArrayList<Actuator>();
        if(scenario.actuatorset==null)
            return x;
        for(Actuator actuator : scenario.actuatorset.getActuators()){
            if(actuator.myType==Actuator.Type.signal)
                x.add(actuator);
        }
        return x;
    }

    /*  EVENTS -------------------------------------------------- */

    public Event eventWithId(long id) {
        if(!scenario.initialized){
            scenario.logger.error("Initialize the scenario before calling this method.");
            return null;
        }
        if(scenario.eventset==null)
            return null;
        for(Event e : scenario.eventset.getSortedevents()){
            if(e.getId()==id)
                return e;
        }
        return null;
    }

    /*  CONTROLLERS --------------------------------------------- */

    public Controller controllerWithId(long id) {
        if(!scenario.initialized){
            scenario.logger.error("Initialize the scenario before calling this method.");
            return null;
        }
        if(scenario.controllerset==null)
            return null;
        for(Controller c : scenario.controllerset.get_Controllers()){
            if(c.getId()==id)
                return c;
        }
        return null;
    }

    public edu.berkeley.path.beats.simulator.ControllerSet controllerset() {
        return scenario.controllerset;
    }

    /*  STATE -------------------------------------------------- */

    public double [][] densityForNetwork(long network_id,int ensemble){

        if(ensemble<0 || ensemble>=scenario.runParam.numEnsemble)
            return null;
        Network network = networkWithId(network_id);
        if(network==null)
            return null;

        int numVehTypes = this.scenario.get.numVehicleTypes();
        double [][] density = new double [network.getLinkList().getLink().size()][numVehTypes];

        int i,j;
        for(i=0;i<network.getLinkList().getLink().size();i++){
            Link link = (Link) network.getLinkList().getLink().get(i);
            Double [] linkdensity = link.getDensityInVeh(ensemble);
            if(linkdensity==null)
                for(j=0;j<numVehTypes;j++)
                    density[i][j] = 0d;
            else
                for(j=0;j<numVehTypes;j++)
                    density[i][j] = linkdensity[j];
        }
        return density;

    }

    public double [][] totalDensity(long network_id){

        Network network;
        if(scenario.getNetworks().size()==1){
            network = (Network) scenario.getNetworks().get(0);
        }
        else {
            network = networkWithId(network_id);
            if(network==null)
                return null;
        }

        int numEnsemble = this.scenario.get.numEnsemble();
        double [][] density = new double [network.getLinkList().getLink().size()][numEnsemble];
        int i,e;
        for(i=0;i<network.getLinkList().getLink().size();i++){
            Link link = (Link) network.getLinkList().getLink().get(i);
            for(e=0;e<numEnsemble();e++)
                density[i][e] = link.getTotalDensityInVeh(e);
        }
        return density;
    }

    public InitialDensitySet current_densities_si(){
        Network network = (Network) scenario.getNetworks().get(0);
        JaxbObjectFactory factory = new JaxbObjectFactory();
        InitialDensitySet init_dens_set = (InitialDensitySet) factory.createInitialDensitySet();
        int numVehTypes = this.scenario.get.numVehicleTypes();
        for(edu.berkeley.path.beats.jaxb.Link jaxbL : network.getListOfLinks()){
            Link L = (Link) jaxbL;
            for(int v=0;v<numVehTypes;v++){
                Density den = factory.createDensity();
                den.setLinkId(jaxbL.getId());
                den.setVehicleTypeId(vehicleTypeIdForIndex(v));
                den.setContent(String.format("%f",L.getDensityInVeh(0,v)/L.getLengthInMeters()));
                init_dens_set.getDensity().add(den);
            }
        }
        return init_dens_set;
    }

    public double [][] totalInflow(long network_id){
        Network network = networkWithId(network_id);
        if(network==null)
            return null;

        int numEnsemble = this.scenario.get.numEnsemble();
        double [][] inflow = new double [network.getLinkList().getLink().size()][numEnsemble];
        int i,e;
        for(i=0;i<network.getLinkList().getLink().size();i++){
            Link link = (Link) network.getLinkList().getLink().get(i);
            for(e=0;e<numEnsemble;e++)
                inflow[i][e] = link.getTotalInflowInVeh(e);
        }
        return inflow;
    }

    public double [][] totalCumulativeInflow(long network_id) throws BeatsException {
        Network network = networkWithId(network_id);
        if(network==null)
            return null;

        int numEnsemble = this.scenario.get.numEnsemble();
        double [][] cumInflow = new double [network.getLinkList().getLink().size()][numEnsemble];
        int i,e;
        for(i=0;i<network.getLinkList().getLink().size();i++){
            Link link = (Link) network.getLinkList().getLink().get(i);
            for(e=0;e<numEnsemble;e++)
                cumInflow[i][e] = scenario.cumulatives.get(link).getCumulativeTotalInputFlowInVeh(e);
        }
        return cumInflow;
    }

    public double [][] totalMeanDensity(long network_id) throws BeatsException {
        Network network = networkWithId(network_id);
        if(network==null)
            return null;

        double [][] meanDensity = new double [network.getLinkList().getLink().size()][numEnsemble()];
        int i,e;
        for(i=0;i<network.getLinkList().getLink().size();i++) {
            Link link = (Link) network.getLinkList().getLink().get(i);
            for (e = 0; e < numEnsemble(); e++)
                meanDensity[i][e] = scenario.cumulatives.get(link).getMeanTotalDensityInVeh(e);
        }
        return meanDensity;
    }

    public Scenario.Cumulatives cumulatives() {
        return scenario.cumulatives;
    }

    /*  ROUTES ---------------------------------------------- */

    public Route routeWithId(long id) {
        if(scenario.getRouteSet()==null)
            return null;
        for(edu.berkeley.path.beats.jaxb.Route route : scenario.getRouteSet().getRoute()){
            if(route.getId()==id)
                return (Route)route;
        }
        return null;
    }

    /*  FDS ------------------------------------------------ */

    public FundamentalDiagramSet current_fds_si(double time_current){
        Network network = (Network) scenario.getNetworks().get(0);
        JaxbObjectFactory factory = new JaxbObjectFactory();
        FundamentalDiagramSet fd_set = factory.createFundamentalDiagramSet();
        for(edu.berkeley.path.beats.jaxb.Link jaxbL : network.getListOfLinks()){
            Link L = (Link) jaxbL;
            FundamentalDiagramProfile fdp = (FundamentalDiagramProfile) factory.createFundamentalDiagramProfile();
            fd_set.getFundamentalDiagramProfile().add(fdp);

            // set values
            fdp.setLinkId(L.getId());
            //fdp.setDt(-1d);
            FundamentalDiagram fd = new FundamentalDiagram(L);

            if(L.getFundamentalDiagramProfile()==null)
                fd.settoDefault();
            else
                fd.copyfrom(L.getFundamentalDiagramProfile().getFDforTime(time_current));
            fd.setOrder(0);
            fdp.getFundamentalDiagram().add(fd);
        }
        return fd_set;
    }

    public edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile FDprofileForLinkId(long link_id){
        if(scenario.getFundamentalDiagramSet()==null)
            return null;
        if(scenario.getFundamentalDiagramSet().getFundamentalDiagramProfile()==null)
            return null;
        for(edu.berkeley.path.beats.jaxb.FundamentalDiagramProfile fdp : scenario.getFundamentalDiagramSet().getFundamentalDiagramProfile())
            if(fdp.getLinkId()==link_id)
                return fdp;
        return null;
    }

    /* DEMANDS ---------------------------------------------- */

    public DemandProfile current_demand_for_link(long link_id){
        if(scenario.getDemandSet()==null)
            return null;
        return ((DemandSet)scenario.getDemandSet()).get_demand_profile_for_link_id(link_id);
    }

    /* SPLITS ---------------------------------------------- */

    public SplitRatioProfile splitprofile_for_node(long node_id){
        if(scenario.getSplitRatioSet()==null)
            return null;
        return ((SplitRatioSet) scenario.getSplitRatioSet()).get_split_profile_for_node_id(node_id);
    }

}
