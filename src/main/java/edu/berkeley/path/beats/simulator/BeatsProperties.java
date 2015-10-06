package edu.berkeley.path.beats.simulator;

import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.DebugFlags;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by gomes on 2/7/14.
 */
public class BeatsProperties extends Properties {

    public HashMap<String,Properties> aux_props;

    public String scenario_name;
    public String performance_config;
    public String output_prefix;
    public String output_format;
    public String uncertainty_model;
    public String split_ratio_model;
    public String node_flow_model;
    public String run_mode;
    public String split_logger_prefix;
    public Double sim_dt;
    public Double start_time;
    public Double duration;
    public Double output_dt;
    public Integer num_reps;
    public Integer ensemble_size;
    public Double split_logger_dt;
    public Boolean use_actm;

    public BeatsProperties(String prop_file_name) throws BeatsException {

        // load properties file
        try{
            load(new FileInputStream(prop_file_name));
        }
        catch (IOException e){
            System.err.print(e);
        }

        // read properties
        scenario_name = getProperty("SCENARIO","");
        sim_dt = getProperty("SIM_DT")==null ? Double.NaN : Double.parseDouble(getProperty("SIM_DT"));
        output_prefix = getProperty("OUTPUT_PREFIX","");
        output_format = getProperty("OUTPUT_FORMAT", "text");
        start_time = Double.parseDouble(getProperty("START_TIME", "0"));
        duration = Double.parseDouble(getProperty("DURATION","86400"));
        output_dt = Double.parseDouble(getProperty("OUTPUT_DT","300"));
        num_reps = Integer.parseInt(getProperty("NUM_REPS", "1"));
        uncertainty_model = getProperty("UNCERTAINTY_MODEL", "gaussian").trim();
        split_ratio_model = getProperty("NODE_SPLIT_RATIO_SOLVER","A").trim();
        node_flow_model = getProperty("NODE_FLOW_SOLVER","proportional").trim();
        run_mode = getProperty("RUN_MODE","normal").trim();
        ensemble_size = Integer.parseInt(getProperty("ENSEMBLE_SIZE", "1"));
        performance_config = getProperty("PERFORMANCE", "");
        split_logger_prefix = getProperty("SPLIT_LOGGER_PREFIX","");
        split_logger_dt = getProperty("SPLIT_LOGGER_DT")==null ? sim_dt : Double.parseDouble(getProperty("SPLIT_LOGGER_DT","0"));
        use_actm = getProperty("USE_ACTM")==null ? false : Boolean.parseBoolean(getProperty("USE_ACTM"));

        DebugFlags.time_print = getProperty("DEBUG.TIME")==null ? 0 : Integer.parseInt(getProperty("DEBUG.TIME", "0"));
        DebugFlags.signal_events = getProperty("DEBUG.SIGNAL_EVENTS") ==null ? false : Boolean.parseBoolean(getProperty("DEBUG.SIGNAL_EVENTS"));

        // validate
        if(scenario_name.isEmpty())
            throw new BeatsException("Scenario name not provided in properties file.");
        if(Double.isNaN(sim_dt))
            throw new BeatsException("Simulation dt not provided in properties file.");
        if(output_prefix.isEmpty())
            throw new BeatsException("Output prefix not provided in properties file.");

        aux_props = new HashMap<String,Properties>();
        for(Map.Entry<Object, Object> entry : entrySet()){
            String [] strlist = ((String) entry.getKey()).split("\\.");
            if(strlist.length<=1)
                continue;
            String group_name = strlist[0];
            if(!aux_props.containsKey(group_name))
                aux_props.put(group_name,new Properties());
            Properties prop = aux_props.get(group_name);
            prop.setProperty(strlist[1],(String) entry.getValue());
        }
    }


    @Override
    public synchronized String toString() {
        String str;
        str = "SCENARIO = " + scenario_name +"\n";
        str += "SIM_DT = " + sim_dt +"\n";
        str += "OUTPUT_PREFIX = " +output_prefix + "\n";
        str += "OUTPUT_FORMAT = " +output_format + "\n";
        str += "START_TIME = " + start_time+ "\n";
        str += "DURATION = " + duration+ "\n";
        str += "OUTPUT_DT = " + output_dt+ "\n";
        str += "NUM_REPS = " + num_reps+ "\n";
        str += "UNCERTAINTY_MODEL = " + uncertainty_model + "\n";
        str += "NODE_FLOW_SOLVER = " + node_flow_model + "\n";
        str += "NODE_SPLIT_RATIO_SOLVER = " + split_ratio_model + "\n";
        str += "RUN_MODE = " + run_mode + "\n";
        str += "ENSEMBLE_SIZE = " + ensemble_size + "\n";
        str += "PERFORMANCE = " + performance_config + "\n";
        str += "SPLIT_LOGGER_PREFIX = " + split_logger_prefix + "\n";
        str += "SPLIT_LOGGER_DT = " + split_logger_dt;
        str += "USE_ACTM = " + use_actm;
        return str;
    }

}