package edu.berkeley.path.beats.util.scenario;

import edu.berkeley.path.beats.jaxb.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;

class DBScenarioSaver implements ScenarioSaverIF,Serializable {
	private static final long serialVersionUID = 3085003338077425521L;

//	Long ID = null;
//
//	@Override
//	public void save(Scenario scenario) throws BeatsException {
//		ID = edu.berkeley.path.beats.db.ScenarioImporter.doImport(scenario);
//	}
//
//	public Long getID() {
//		return ID;
//	}

	
	
	
	
	// TEMP ============================================================ 
	@Override
	public void save(Scenario scenario) throws BeatsException {
	}
	public Long getID() {
		return null;
	}
	// TEMP ============================================================ 

}
