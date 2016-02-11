package edu.berkeley.path.beats.util.polyline;

import edu.berkeley.path.beats.jaxb.Point;

import java.io.Serializable;

public abstract class EncoderBase implements EncoderIF,Serializable {

	private static final long serialVersionUID = 1037605340443555827L;

	/**
	 * Encodes a list of points
	 * @param pl the point list
	 */
	public void add(java.util.List<Point> pl) {
		for (Point point : pl)
			add(point);
	}

}
