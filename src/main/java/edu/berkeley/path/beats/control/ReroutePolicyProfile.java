package edu.berkeley.path.beats.control;


import java.util.LinkedList;
import java.util.List;

import edu.berkeley.path.beats.jaxb.Node;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReroutePolicyProfile {
    public Node actuatorNode;
    public long route_id; // specifies the route for a given OD
//    public long compliant_vehicle_type_id;

    public List<Double> reroutePolicy;

    public ReroutePolicyProfile() {
        reroutePolicy = new LinkedList<Double>();
    }

    public ReroutePolicyProfile(long rid, long vid) {
    	route_id = rid;
//        compliant_vehicle_type_id = vid;
        reroutePolicy = new LinkedList<Double>();
    }

    public void print() {
        System.out.println(actuatorNode.getNodeName());
        for (Double d : reroutePolicy) {
            System.out.print(d.toString() + ",");
        }
        System.out.println();
    }
}