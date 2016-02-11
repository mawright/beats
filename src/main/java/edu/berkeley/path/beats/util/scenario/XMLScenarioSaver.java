package edu.berkeley.path.beats.util.scenario;

import java.io.File;
import java.io.Serializable;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import edu.berkeley.path.beats.jaxb.Scenario;
import edu.berkeley.path.beats.simulator.utils.BeatsException;

class XMLScenarioSaver extends ScenarioSaverBase implements ScenarioSaverIF,Serializable {

	private static final long serialVersionUID = -5962493670375208361L;
	private String filename;

	public XMLScenarioSaver(String filename) {
		this.filename = filename;
	}

	@Override
	protected Marshaller getMarshaller() throws JAXBException, BeatsException {
		Marshaller marshaller = super.getMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		return marshaller;
	}

	@Override
	public void save(Scenario scenario) throws BeatsException {
		ensureSchemaVersion(scenario);
		try {
			getMarshaller().marshal(scenario, new File(filename));
		} catch (JAXBException exc) {
			throw new BeatsException(exc);
		}
	}

}
