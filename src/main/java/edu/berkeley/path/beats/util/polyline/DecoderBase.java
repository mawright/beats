package edu.berkeley.path.beats.util.polyline;

import edu.berkeley.path.beats.jaxb.ObjectFactory;

import java.io.Serializable;

public abstract class DecoderBase implements DecoderIF,Serializable {

	private static final long serialVersionUID = -8038649504444460175L;
	protected ObjectFactory factory = null;

	@Override
	public void setObjectFactory(ObjectFactory factory) {
		this.factory = factory;
	}

}
