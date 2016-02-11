package edu.berkeley.path.beats.util.scenario;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import edu.berkeley.path.beats.util.SchemaUtil;
import org.apache.log4j.Logger;

import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;

abstract class ScenarioSaverBase implements ScenarioSaverIF,Serializable {

	private static final long serialVersionUID = 8801769867183185911L;

	protected Marshaller getMarshaller() throws JAXBException, BeatsException {
		Marshaller marshaller = ScenarioLoaderBase.getJAXBContext().createMarshaller();
		marshaller.setSchema(SchemaUtil.getSchema());
		return marshaller;
	}

	protected static Logger logger = Logger.getLogger(ScenarioSaverBase.class);

	/**
	 * Sets the scenario schema version if it has not been set yet
	 * @param scenario
	 * @throws BeatsException
	 */
	protected static void ensureSchemaVersion(edu.berkeley.path.beats.jaxb.Scenario scenario) throws BeatsException {
		if (null == scenario.getSchemaVersion()) {
			String schemaVersion = SchemaUtil.getSchemaVersion();
			logger.debug("Schema version was not set. Assuming current version: " + schemaVersion);
			scenario.setSchemaVersion(schemaVersion);
		}
	}

}
