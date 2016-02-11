package edu.berkeley.path.beats.control.rr_interface;


import java.io.Serializable;
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
public class ReroutePolicyProfile  implements Serializable {
    private static final long serialVersionUID = -2608105767354228116L;
    public Node actuatorNode;
    public long in_link_id;
    public long out_link_id;
    public long vehicle_type_id;

    public List<Double> reroutePolicy;

    public ReroutePolicyProfile() {
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