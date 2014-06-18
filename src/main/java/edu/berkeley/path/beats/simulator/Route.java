package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.jaxb.RouteLink;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/24/14.
 */
public class Route extends edu.berkeley.path.beats.jaxb.Route {

    protected List<Link> ordered_links;

    /////////////////////////////////////////////////////////////////////
    // populate / validate
    /////////////////////////////////////////////////////////////////////

    protected void populate(Scenario scenario) {
        // store links in order
        ordered_links = new ArrayList<Link>();
        for(int i=0;i<getRouteLink().size();i++){
            RouteLink rlink = null;
            for(RouteLink r : getRouteLink())
                if(r.getLinkOrder()==i){
                    rlink = r;
                    break;
                }
            ordered_links.add(rlink==null?null:scenario.getLinkWithId(rlink.getLinkId()));
        }
    }

    protected void validate() {

        // check link ids
        for(Link link : ordered_links)
            if(link==null)
                BeatsErrorLog.addError("Incorrect link ID or missing order index in route " + getId());

        // check order
        for(int i=0;i<ordered_links.size()-1;i++){
            Link link = ordered_links.get(i);
            Link next_link = ordered_links.get(i+1);
            if(link==null || next_link==null)
                break;
            boolean found_it=false;
            for(Link L : link.getEnd_node().getOutput_link()){
                if(L.equals(next_link)){
                    found_it=true;
                    break;
                }
            }
            if(!found_it)
                BeatsErrorLog.addError("Ordering problem in route " + getId() + ". Link " + next_link.getId() + " does not follow link " + link.getId());
        }
    }


    /////////////////////////////////////////////////////////////////////
    // public
    /////////////////////////////////////////////////////////////////////

    public Link get_link_number(int i){
        if(ordered_links==null)
            return null;
        if(i>=0 & i<ordered_links.size())
            return ordered_links.get(i);
        else
            return null;
    }



}
