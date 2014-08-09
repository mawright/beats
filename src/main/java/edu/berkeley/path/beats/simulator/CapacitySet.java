package edu.berkeley.path.beats.simulator;


import edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacityProfile;
import edu.berkeley.path.beats.jaxb.DownstreamBoundaryCapacitySet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by gomes on 7/15/14.
 */
public class CapacitySet extends DownstreamBoundaryCapacitySet {

    private Scenario myScenario;
    private Map<Long,CapacityProfile> link_id_to_capacityprofile;

    /////////////////////////////////////////////////////////////////////
    // populate / reset / validate / update
    /////////////////////////////////////////////////////////////////////

    public void populate(Scenario myScenario) {

        this.myScenario = myScenario;

        if(this.getDownstreamBoundaryCapacityProfile().isEmpty())
            return;

        // link to demand profile map
        link_id_to_capacityprofile = new HashMap<Long,CapacityProfile>();
        for(DownstreamBoundaryCapacityProfile cp : getDownstreamBoundaryCapacityProfile()){

            CapacityProfile scp = (CapacityProfile) cp;

            link_id_to_capacityprofile.put(new Long(cp.getLinkId()), scp);

            // populate demand profile
            scp.populate(myScenario);
        }
    }

    protected void reset() {
        Iterator it = link_id_to_capacityprofile.entrySet().iterator();
        while (it.hasNext())
            ((CapacityProfile) ((Map.Entry)it.next()).getValue()).reset();
    }

    protected void validate() {
        if(link_id_to_capacityprofile ==null)
            return;
        if(link_id_to_capacityprofile.isEmpty())
            return;
        Iterator it = link_id_to_capacityprofile.entrySet().iterator();
        while (it.hasNext())
            ((CapacityProfile) ((Map.Entry)it.next()).getValue()).validate();
    }

    protected void update() {
        Iterator it = link_id_to_capacityprofile.entrySet().iterator();
        while (it.hasNext())
            ((CapacityProfile) ((Map.Entry)it.next()).getValue()).update();
    }

    /////////////////////////////////////////////////////////////////////
    // protected interface
    /////////////////////////////////////////////////////////////////////

    public boolean has_profile_for_link(long link_id){
        return link_id_to_capacityprofile==null ? false : link_id_to_capacityprofile.containsKey(link_id);
    }

    protected void add_or_replace_profile(CapacityProfile cp){
        if(link_id_to_capacityprofile==null)
            link_id_to_capacityprofile = new HashMap<Long,CapacityProfile>();
        link_id_to_capacityprofile.put(cp.getLinkId(),cp);
    }

}
