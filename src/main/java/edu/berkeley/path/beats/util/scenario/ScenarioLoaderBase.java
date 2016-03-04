package edu.berkeley.path.beats.util.scenario;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.util.SchemaUtil;

import java.io.Serializable;

abstract class ScenarioLoaderBase implements ScenarioLoaderIF,Serializable {

	private static final long serialVersionUID = 8970108864473060795L;

	@Override
	public edu.berkeley.path.beats.simulator.Scenario load() throws BeatsException {
//		edu.berkeley.path.beats.simulator.Scenario scenario = (edu.berkeley.path.beats.simulator.Scenario) loadRaw();
//		edu.berkeley.path.beats.util.SchemaUtil.checkSchemaVersion(scenario);
//		return edu.berkeley.path.beats.simulator.ObjectFactory.populate_validate(scenario);
		return null;
	}

	static JAXBContext getJAXBContext() throws JAXBException {
		return JAXBContext.newInstance(edu.berkeley.path.beats.jaxb.ObjectFactory.class);
	}

	protected static Object getJAXBObjectFactory() {
		return new edu.berkeley.path.beats.simulator.JaxbObjectFactory();
	}

	protected static Unmarshaller getUnmarshaller() throws JAXBException, BeatsException {
		Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
		unmarshaller.setSchema(SchemaUtil.getSchema());
		setObjectFactory(unmarshaller, getJAXBObjectFactory());
		return unmarshaller;
	}

    private static void setObjectFactory(Unmarshaller unmrsh, Object factory) throws BeatsException {
        final String classname = unmrsh.getClass().getName();
        String propnam = classname.startsWith("com.sun.xml.internal") ?//
                "com.sun.xml.internal.bind.ObjectFactory" ://
                "com.sun.xml.bind.ObjectFactory";
        try {
            unmrsh.setProperty(propnam, factory);
        } catch (PropertyException e) {
            throw new BeatsException(e);
        }
    }

}
