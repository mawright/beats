package edu.berkeley.path.beats.util.scenario;

import org.codehaus.jettison.mapped.MappedNamespaceConvention;

import java.io.Serializable;

class JSONSettings implements Serializable{

	private static final long serialVersionUID = 1298309645460848951L;

	public static MappedNamespaceConvention getConvention() {
		org.codehaus.jettison.mapped.Configuration config = new org.codehaus.jettison.mapped.Configuration();
		return new MappedNamespaceConvention(config);
	}

}