package edu.berkeley.path.beats.simulator.linkBehavior;

import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;
import edu.berkeley.path.beats.simulator.TypeUncertainty;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;

import java.io.Serializable;

/**
 * Created by gomes on 11/3/2014.
 */
public class LinkBehaviorACTM extends LinkBehaviorCTM implements Serializable {

    private static final long serialVersionUID = -2893966833535794822L;

    private Node my_end_node;
    private int my_link_ind;
    private int fr_link_ind;

    public LinkBehaviorACTM(Link link) {
        super(link);
        my_end_node = link.getEnd_node();
        my_link_ind = my_end_node.getInputLinkIndex(link.getId());
        fr_link_ind = -1;
        for(int i=0;i<my_end_node.getOutput_link().length;i++)
            if(my_end_node.output_link[i].isOfframp())
                fr_link_ind = i;

    }

    @Override
    public void update_outflow_demand(double external_max_speed, double external_max_flow) {


        int numVehicleTypes = myScenario.get.numVehicleTypes();

        double totaldensity;
        double totaloutflow;
        FundamentalDiagram FD;
        double betabar  = fr_link_ind>=0? 1 - my_end_node.getSplitRatio(my_link_ind,fr_link_ind,0) : 1d;

        for(int e=0;e<myScenario.get.numEnsemble();e++){

            FD = myLink.currentFD(e);

            totaldensity = myLink.getTotalDensityInVeh(e);

            // case empty link
            if( BeatsMath.lessorequalthan(totaldensity, 0d) ){
                flow_demand[e] =  BeatsMath.zeros(numVehicleTypes);
                continue;
            }

            // compute total flow leaving the link in the absence of flow control
            double ff_speed = Math.min(FD.getVfNormalized(),external_max_speed);

            totaloutflow = Math.min( ff_speed*totaldensity , FD._getCapacityInVeh() / betabar );

            // capacity profile
            if(myLink.myCapacityProfile!=null)
                totaloutflow = Math.min( totaloutflow , myLink.myCapacityProfile.getCurrentValue() );

            // flow controller
            totaloutflow = Math.min( totaloutflow , external_max_flow );

            // flow uncertainty model (unless controller wants zero flow)
            if(myScenario.get.has_flow_unceratinty() && BeatsMath.greaterthan(external_max_flow,0d) ){

                double delta_flow=0.0;
                double std_dev_flow = myScenario.get.std_dev_flow();

                switch(myScenario.get.uncertaintyModel()){
                    case uniform:
                        delta_flow = BeatsMath.sampleZeroMeanUniform(std_dev_flow);
                        break;

                    case gaussian:
                        delta_flow = BeatsMath.sampleZeroMeanGaussian(std_dev_flow);
                        break;
                }

                totaloutflow = Math.max( 0d , totaloutflow + delta_flow );
                totaloutflow = Math.min( totaloutflow , totaldensity );
            }

            // split among types
            if(myScenario.get.numVehicleTypes()==1)
                flow_demand[e][0] = totaloutflow;
            else{
                double alpha = totaloutflow/totaldensity;
                for(int j=0;j<myScenario.get.numVehicleTypes();j++)
                    flow_demand[e][j] = get_density_in_veh(e, j)*alpha;
            }

        }

        return;

    }

}
