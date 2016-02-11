package edu.berkeley.path.beats.control.rr_interface;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReroutePolicySet  implements Serializable {
    private static final long serialVersionUID = -5159098564508658004L;
    public List<ReroutePolicyProfile> profiles;

    public ReroutePolicySet() {
        profiles = new LinkedList<ReroutePolicyProfile>();
    }

    public void print() {
        for (ReroutePolicyProfile profile : profiles) {
            profile.print();
        }
    }
}
