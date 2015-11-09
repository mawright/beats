package edu.berkeley.path.beats.simulator.utils;

import edu.berkeley.path.beats.simulator.FundamentalDiagram;
import edu.berkeley.path.beats.simulator.Link;

import java.io.Serializable;
import java.util.List;

/**
 * Created by gomes on 6/4/2015.
 */
public class BeatsTimeProfileFD extends BeatsTimeProfile<FundamentalDiagram> implements Serializable {
    private static final long serialVersionUID = -6924411263724890747L;

    public BeatsTimeProfileFD(Link myLink,List<edu.berkeley.path.beats.jaxb.FundamentalDiagram> fds, Double dt, Double startTime, double simdtinseconds) {
        super(dt, startTime, simdtinseconds);
        for (edu.berkeley.path.beats.jaxb.FundamentalDiagram fd : fds) {
            FundamentalDiagram _fd = new FundamentalDiagram(myLink, fd);    // create empty fd
            // _fd.settoDefault();					// set to default
            // _fd.copyfrom(fd);					// copy and normalize
            data.add(_fd);
        }
    }

//        @Override
//        public void reset(){
//            super.reset();
//            current_sample = reset_fd;
//        }

    @Override
    public void validate(){
        super.validate();
        for (FundamentalDiagram fd : data)
            fd.validate();
    }

    public void set_lanes(double newlanes) {
        for (FundamentalDiagram fd : data)
            fd.setLanes(newlanes);
    }
}
