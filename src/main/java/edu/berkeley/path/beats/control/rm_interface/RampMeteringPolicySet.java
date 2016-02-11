package edu.berkeley.path.beats.control.rm_interface;

import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Scenario;
//import edu.berkeley.path.lprm.rm.RampMeteringSolution;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class RampMeteringPolicySet  implements Serializable {
    private static final long serialVersionUID = -6445139806056220313L;
    public List<RampMeteringPolicyProfile> profiles;

    public RampMeteringPolicySet() {
        profiles = new LinkedList<RampMeteringPolicyProfile>();
    }

//    public RampMeteringPolicySet(Scenario myScenario,RampMeteringSolution sol){
//        profiles = new LinkedList<RampMeteringPolicyProfile>();
//        HashMap<Long,Double[]> prof_map = null; //sol.get_   get_metering_profiles_in_vps();
//        for(Map.Entry<Long,Double[]> entry : prof_map.entrySet()){
//            Long or_id = entry.getKey();
//            Double [] policy = entry.getValue();
//            Link or = myScenario.get.linkWithId(or_id);
//            profiles.add(new RampMeteringPolicyProfile(or,policy));
//        }
//    }

    @Override
    public String toString() {
        String str = "";
        for (RampMeteringPolicyProfile profile : profiles)
            str += "\t" + profile +"\n";
        return str;
    }
}
