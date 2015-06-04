package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.simulator.Controller;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.utils.Table;

/**
 * Created by gomes on 6/4/2015.
 */
public class Controller_Commodity_Swapper extends Controller {

    /////////////////////////////////////////////////////////////////////
    // Construction
    /////////////////////////////////////////////////////////////////////

    public Controller_Commodity_Swapper(Scenario myScenario, edu.berkeley.path.beats.jaxb.Controller c) {
        super(myScenario,c,Algorithm.Commodity_Swapper);
    }


    /////////////////////////////////////////////////////////////////////
    // populate / validate / reset  / update
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void populate(Object jaxbO) {

        super.populate(jaxbO);



        Table splitstable = getTables().get("splits");
        for(Table.Row row : splitstable.getRows()){
            Integer comm_in = Integer.parseInt(row.get_value_for_column_name("comm_in"));
            Integer comm_out = Integer.parseInt(row.get_value_for_column_name("comm_out"));
            Double dt = Double.parseDouble(row.get_value_for_column_name("comm_out"));


            ADVGBHQEG
        }




        System.out.println(jaxbO);
    }

    @Override
    protected void validate() {
        super.validate();
    }

    @Override
    protected void reset()  {
        super.reset();
    }

}
