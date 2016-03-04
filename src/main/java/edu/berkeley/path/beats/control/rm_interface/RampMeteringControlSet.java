package edu.berkeley.path.beats.control.rm_interface;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jdr
 * Date: 10/25/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class RampMeteringControlSet implements Serializable {
    private static final long serialVersionUID = -1380350383831144760L;
    public List<RampMeteringControl> control;

    public RampMeteringControlSet() {
        control = new LinkedList<RampMeteringControl>();
    }
}
