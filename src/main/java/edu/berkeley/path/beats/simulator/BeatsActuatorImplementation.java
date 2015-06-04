package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.actuator.ActuatorSignal;
//import edu.berkeley.path.beats.actuator.ActuatorSignalStageSplits;
import edu.berkeley.path.beats.actuator.NEMA;
//import edu.berkeley.path.beats.actuator.StageSplit;
import edu.berkeley.path.beats.jaxb.LinkReference;
import edu.berkeley.path.beats.jaxb.Phase;
import edu.berkeley.path.beats.jaxb.Splitratio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeatsActuatorImplementation extends ActuatorImplementation {

	public BeatsActuatorImplementation(edu.berkeley.path.beats.jaxb.Actuator parent,Object context){

        Scenario scenario = (Scenario) context;
        edu.berkeley.path.beats.jaxb.ScenarioElement se = parent.getScenarioElement();

        switch(Actuator.Type.valueOf(parent.getActuatorType().getName())){
            case ramp_meter:
            case commodity_changer:
            case vsl:
                target = scenario.get.linkWithId(se.getId());
                break;
            case cms:
                target = scenario.get.nodeWithId(se.getId());
                break;
            case signal:
                break;
        }
	}

    public BeatsActuatorImplementation(edu.berkeley.path.beats.jaxb.Signal signal,Object context){
        Scenario scenario = (Scenario) context;
        HashMap<NEMA.ID,List<Link>> targ = new HashMap<NEMA.ID,List<Link>>();
        for(Phase phase : signal.getPhase()){
            List linklist = new ArrayList<Link>();
            if(phase.getLinkReferences()!=null && phase.getLinkReferences().getLinkReference()!=null)
                for(LinkReference linkref : phase.getLinkReferences().getLinkReference())
                    linklist.add(scenario.get.linkWithId(linkref.getId()));
            targ.put( NEMA.int_to_nema(phase.getNema().intValue()) , linklist );
        }
        target = targ;
    }
	
	@Override
	public void deploy_metering_rate_in_veh(Double metering_rate_in_veh) {
        ((Link)target).set_external_max_flow_in_veh( metering_rate_in_veh==null ? Double.POSITIVE_INFINITY : metering_rate_in_veh );
	}

//	@Override
//	public void deploy_stage_splits(StageSplit[] stage_splits) {
//
//        // normalize splits
//        double sum_splits = 0d;
//        for(StageSplit stage_split : stage_splits)
//            sum_splits += stage_split.split;
//        if(BeatsMath.greaterthan(sum_splits,0d))
//            for(StageSplit stage_split : stage_splits)
//                stage_split.split /= sum_splits;
//        else{
//            for(StageSplit stage_split : stage_splits)
//                stage_split.split = 0d;
//            stage_splits[0].split = 1d;
//        }
//
//        // map splits onto links
//        ActuatorSignalStageSplits myAct =(ActuatorSignalStageSplits) myActuator;
//        double [] link_splits = BeatsMath.zeros(myAct.inlinks.size());
//        for(StageSplit stage_split:stage_splits){
//            if(stage_split.stage.phaseA!=null)
//                for(Link link : stage_split.stage.phaseA.getTargetlinks())
//                    link_splits[myAct.inlinks.indexOf(link)] += stage_split.split;
//            if(stage_split.stage.phaseB!=null)
//                for(Link link : stage_split.stage.phaseB.getTargetlinks())
//                    link_splits[myAct.inlinks.indexOf(link)] += stage_split.split;
//        }
//
//        // send capacities to links
//        for(int i=0;i<myAct.inlinks.size();i++){
//            Link link = myAct.inlinks.get(i);
//            link.set_external_max_flow_in_vph(link.getCapacityInVPS(0)*3600d*link_splits[i]);
//        }
//
//	}

	@Override
	public void deploy_cms_split(List<Splitratio> splits) {
		if(target==null)
            return;
        if(splits==null)
            ((Node)target).deactivate_split_control();
        else
            ((Node)target).set_controller_split(splits);
	}

	@Override
	public void deploy_vsl_speed() {
		// TODO Auto-generated method stub
	}

    @Override
    public void deploy_bulb_color(NEMA.ID nema,ActuatorSignal.BulbColor color){

        List<Link> links = ((HashMap<NEMA.ID,List<Link>>) target).get(nema);
        if(links==null || links.isEmpty())
            return;

        double maxflow = Double.POSITIVE_INFINITY;
        switch(color){
            case GREEN:
                maxflow = Double.POSITIVE_INFINITY;
                break;
            case YELLOW:
            case RED:
            case DARK:
                maxflow = 0d;
                break;
        }

        for(Link link : links)
            link.set_external_max_flow_in_veh(maxflow);
    }

}
