package edu.berkeley.path.beats;

import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;

/**
 * Created by gomes on 9/8/14.
 */
public class Jaxb implements Serializable {
    private static final long serialVersionUID = 9081503712558255882L;

    /* READ */

    public static Scenario create_scenario_from_xml(String configfilename,Object object_factory) throws BeatsException {
        BeatsErrorLog.clearErrorMessage();
        Unmarshaller u = create_unmarshaller(object_factory);
        Scenario S = (Scenario) unmarshall(u,configfilename);
        S.get = new ScenarioGetApi(S);
        S.set = new ScenarioSetApi(S);
        S.set.configfilename(configfilename);
        return S;
    }

    public static Scenario create_scenario_from_xml(String configfilename) throws BeatsException{
        return create_scenario_from_xml(configfilename,new JaxbObjectFactory());
    }

    public static PerformanceCalculator create_performance_calculator(String configfilename) throws BeatsException {
        BeatsErrorLog.clearErrorMessage();
        Unmarshaller u = create_unmarshaller(new JaxbObjectFactory());
        return (PerformanceCalculator) unmarshall(u,configfilename);
    }

    public static DemandSet create_demand_set_from_xml(String filename) throws BeatsException {
        Unmarshaller u = create_unmarshaller(new JaxbObjectFactory());
        return (DemandSet) unmarshall(u,filename);
    }

    /* WRITE */

    public static void write_scenario_to_xml(edu.berkeley.path.beats.jaxb.Scenario S,String filename) throws BeatsException{
        try {
            Jaxb.getMarshaller().marshal(S, new File(filename));
        } catch (JAXBException e) {
            throw new BeatsException(e);
        }
    }

    /* PRIVATE */


    private static Unmarshaller create_unmarshaller(Object object_factory) throws BeatsException{
        Unmarshaller u = null;
        try {
            //Reset the classloader for main thread; need this if I want to run properly
            //with JAXB within MATLAB. (luis)
            Thread.currentThread().setContextClassLoader(ObjectFactory.class.getClassLoader());
            JAXBContext context = JAXBContext.newInstance("edu.berkeley.path.beats.jaxb");
            u = context.createUnmarshaller();
            setObjectFactory(u,object_factory);
        } catch( JAXBException je ) {
            throw new BeatsException("Failed to create context for JAXB unmarshaller", je);
        }
//        try{
//            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//            ClassLoader classLoader = ObjectFactory.class.getClassLoader();
//            Schema schema = factory.newSchema(classLoader.getResource("beats_output.xsd"));
//            u.setSchema(schema);
//        } catch(SAXException e){
//            throw new BeatsException("Schema not found", e);
//        }
        return u;
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

    private static Object unmarshall(Unmarshaller u,String configfilename) throws BeatsException {
        Object S = null;
        try {
            if(!configfilename.endsWith(".xml"))
                configfilename += ".xml";
            S = u.unmarshal( new FileInputStream(configfilename) );
        } catch( JAXBException je ) {
            throw new BeatsException("JAXB threw an exception when loading the configuration file", je);
        } catch (FileNotFoundException e) {
            throw new BeatsException("Configuration file not found. " + configfilename, e);
        }
        if(S==null){
            throw new BeatsException("Unknown load error");
        }
        return S;
    }

    private static Marshaller getMarshaller() throws JAXBException, BeatsException {
        Marshaller marshaller = null;
        try{
            Thread.currentThread().setContextClassLoader(ObjectFactory.class.getClassLoader());
            JAXBContext context = JAXBContext.newInstance("edu.berkeley.path.beats.jaxb");
            marshaller = context.createMarshaller();
//            marshaller.setSchema(SchemaUtil.getSchema());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (JAXBException e ) {
            throw new BeatsException(e);
        }
        return marshaller;
    }

}
