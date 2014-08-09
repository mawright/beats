/**
 * Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 **/

package edu.berkeley.path.beats;

import edu.berkeley.path.beats.simulator.*;

/**
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public final class Runner {

    public static void main(String[] args) {

        try {
			if (0 == args.length)
                throw new InvalidUsageException();

			String cmd = args[0];
			String[] arguments = new String[args.length - 1];
			System.arraycopy(args, 1, arguments, 0, args.length - 1);

            // simulate
			if (cmd.equals("-s")){
				Runner.run_simulation(arguments);
			}

            // version
            else if (cmd.equals("-v")){
                System.out.println(get_version());
            }

            // check scenario
            else if (cmd.equals("-c")){
                Runner.validate(args);
            }

            // properties
            else if (cmd.equals("-p")){
                System.out.println(get_help_simulate());
            }

            // help
            else if (cmd.equals("-h")){
                System.err.print(get_usage());
            }   else
                throw new InvalidUsageException(cmd);

		} catch (InvalidUsageException exc) {
            System.err.print(get_usage());
		} catch (Exception exc) {
			exc.printStackTrace();
		}

        finally {
			if (BeatsErrorLog.hasmessage()) {
				BeatsErrorLog.print();
				BeatsErrorLog.clearErrorMessage();
			}
        }
    }

    private static void run_simulation(String[] args){

        long time = System.currentTimeMillis();

        BeatsProperties props = null;

        // read properties file
        try {
            props = new BeatsProperties(args[0]);
        } catch (BeatsException e){
            System.err.println(e);
            System.exit(1);
        }

        try {

            // load configuration file
            Scenario scenario = ObjectFactory.createAndLoadScenario(props.scenario_name);

            if (scenario==null)
                throw new BeatsException("Scenario did not load");

            // initialize
            scenario.initialize( props.sim_dt ,
                    props.start_time ,
                    props.start_time + props.duration ,
                    props.output_dt ,
                    props.output_format,
                    props.output_prefix,
                    props.num_reps,
                    props.ensemble_size ,
                    props.uncertainty_model ,
                    props.node_flow_model ,
                    props.split_ratio_model ,
                    props.performance_config ,
                    props.run_mode,
                    props.split_logger_prefix,
                    props.split_logger_dt,
                    props.aux_props );

            // run the scenario
            scenario.run();

        }
        catch (BeatsException exc) {
            exc.printStackTrace();
        }

        finally{
            System.out.println("done in " + (System.currentTimeMillis()-time));
        }

    }

    private static void validate(String[] args){

    }

    private static Version get_version(){
        return Version.get();
    }

    public static String get_usage(){
        String str =
                "Usage: [-h|-p|-v] [-s file|-c file]\n" +
                "\t-h\tDisplay usage message.\n" +
                "\t-p\tDisplay description of the properties file.\n" +
                "\t-v\tDisplay version information.\n" +
                "\t-s file\tRun simulation with properties file.\n" +
                "\t-c file\tCheck (validate) a scenario file.\n";
        return str;
    }

    public static String get_help_simulate(){
        String str =
                "Usage: -s properties_file\n" +
                "The properties file contains the following:\n" +
                        "\tSCENARIO : Name of the scenario configuration file. (required)\n" +
                        "\tSIM_DT : Simulation time step in seconds. (required) \n" +
                        "\tOUTPUT_PREFIX : Prefix for the output file. (required)\n" +
                        "\tOUTPUT_FORMAT : Format of the output files <text,xml>. (default=text) \n" +
                        "\tSTART_TIME : Simulation start time in seconds after midnight. (default=0) \n" +
                        "\tDURATION : Duration of the simulation in seconds. (default=86400)\n" +
                        "\tOUTPUT_DT : Output sampling time in seconds. (default=300) \n" +
                        "\tNUM_REPS : Number of repetitions. (default=1)\n" +
                        "\tUNCERTAINTY_MODEL : Uncertainty model <gaussian,uniform>. (default=gaussian)\n" +
                        "\tNODE_FLOW_SOLVER : Node model <proportional,symmetric>. (default=proportional)\n" +
                        "\tNODE_SPLIT_RATIO_SOLVER : Algorithm for unknown splits <A,B,C,HAMBURGER>. (default=A) \n" +
                        "\tRUN_MODE : run mode <normal,fw_fr_split_output>. (default=normal)\n" +
                        "\tPERFORMANCE : Configuration file for performance output.\n";
        return str;
    }

//	public static void run_db(String [] args) throws BeatsException, edu.berkeley.path.beats.RunnerCOP.InvalidUsageException {
//		logger.info("Parsing arguments");
//		long scenario_id;
//		RunnerArguments runargs = new RunnerArguments(RunnerArguments.defaults());
//		if (0 == args.length || 5 < args.length) {
//			final String eol = System.getProperty("line.separator");
//			throw new edu.berkeley.path.beats.RunnerCOP.InvalidUsageException(
//					"Usage: simulate|s scenario_id [parameters]" + eol +
//					"Parameters:" + eol +
//					"\tstart time, sec" + eol +
//					"\tduration, sec" + eol +
//					"\toutput sampling time, sec" + eol +
//					"\tnumber of simulations");
//		} else {
//			scenario_id = Long.parseLong(args[0]);
//			runargs.parseArgs(args, 1);
//		}
//
//		edu.berkeley.path.beats.db.Service.init();
//
//		logger.info("Loading scenario");
//		Scenario scenario = ScenarioLoader.load(scenario_id);
//
//		if (args.length < 4) {
//			logger.info("Loading default simulation settings");
//			try {
//				DefSimSettings db_defss = DefSimSettingsPeer.retrieveByPK(Long.valueOf(scenario_id));
//				RunnerArguments defss = new RunnerArguments(runargs.getParent());
//				defss.setStartTime(db_defss.getSimStartTime());
//				defss.setDuration(db_defss.getSimDuration());
//				defss.setOutputDt(db_defss.getOutputDt());
//				runargs.setParent(defss);
//			} catch (NoRowsException exc) {
//				logger.warn("Found no default simulation settings for scenario " + scenario_id, exc);
//			} catch (TooManyRowsException exc) {
//				logger.error("Too many default simulation settings for scenario " + scenario_id, exc);
//			} catch (TorqueException exc) {
//				throw new BeatsException(exc);
//			}
//		}
//
//		logger.info("Simulation parameters: " + runargs);
//
//		logger.info("Simulation");
//		scenario.run(runargs.getDt(),
//				runargs.getStartTime(),
//				runargs.getEndTime(),
//				runargs.getOutputDt(),
//				"db",
//				runargs.getOutputfileprefix(),
//				runargs.getNumReps());
//
//		edu.berkeley.path.beats.db.Service.shutdown();
//		logger.info("Done");
//	}

    public static class InvalidUsageException extends Exception {
        public InvalidUsageException() {
            super();
        }
        public InvalidUsageException(String message) {
            super(message);
        }
    }

}
