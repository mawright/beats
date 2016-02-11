package edu.berkeley.path.beats.util.scenario;

import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;

class DBScenarioLoader implements ScenarioLoaderIF,Serializable {
	private static final long serialVersionUID = 5996726559475161602L;

//	private Long ID;
//
//	public DBScenarioLoader(Long ID) {
//		this.ID = ID;
//	}
//
//	@Override
//	public edu.berkeley.path.beats.jaxb.Scenario loadRaw() throws BeatsException {
//		return edu.berkeley.path.beats.db.ScenarioExporter.doExport(ID);
//	}
//
//	@Override
//	public edu.berkeley.path.beats.simulator.Scenario load() throws BeatsException {
//		return ObjectFactory.process((edu.berkeley.path.beats.simulator.Scenario) loadRaw());
//	}
	
	// TEMP ============================================================ 
	@Override
	public edu.berkeley.path.beats.jaxb.Scenario loadRaw() throws BeatsException {
		return null;
	}
	@Override
	public edu.berkeley.path.beats.simulator.Scenario load() throws BeatsException {
		return null;
	}
	// TEMP ============================================================ 

}
