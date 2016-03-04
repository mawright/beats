package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.control.rm_interface.RampMeteringControlSet;
import edu.berkeley.path.beats.control.rm_interface.RampMeteringPolicyMaker;
import edu.berkeley.path.beats.control.rm_interface.RampMeteringPolicyProfile;
import edu.berkeley.path.beats.control.rm_interface.RampMeteringPolicySet;
import edu.berkeley.path.beats.jaxb.FundamentalDiagramSet;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsFormatter;

import java.io.Serializable;
import java.util.Properties;

public class PolicyMaker_Tester implements RampMeteringPolicyMaker,Serializable {

    private static final long serialVersionUID = 9075446564522383183L;

    @Override
    public RampMeteringPolicySet givePolicy(Network net, FundamentalDiagramSet fd, DemandSet demand, SplitRatioSet splitRatios, InitialDensitySet ics, RampMeteringControlSet control, Double dt, Properties props) {

        RampMeteringPolicySet policy = new RampMeteringPolicySet();

        Double [] sample_data = BeatsFormatter.readCSVstring_nonnegative(demand.getDemandProfile().get(0).getDemand().get(0).getContent(), ",");
        double num_data = sample_data.length;

        for(edu.berkeley.path.beats.jaxb.Link jaxbL : net.getListOfLinks()){
            Link L = (Link) jaxbL;
            if(L.isSource()){
                RampMeteringPolicyProfile profile = new RampMeteringPolicyProfile();
                profile.sensorLink = L;
                for(int i=0;i<num_data;i++)
                    profile.rampMeteringPolicy.add(900d/3600d);
                policy.profiles.add(profile);
            }
        }
        return policy;
    }
}
