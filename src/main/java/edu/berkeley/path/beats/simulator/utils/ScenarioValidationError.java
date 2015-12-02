package edu.berkeley.path.beats.simulator.utils;

import edu.berkeley.path.beats.simulator.utils.BeatsException;

import java.io.Serializable;

@SuppressWarnings("serial")
final public class ScenarioValidationError extends BeatsException {
	public ScenarioValidationError() {
		super("Scenario validation failed. See error log for details");
	}
}
