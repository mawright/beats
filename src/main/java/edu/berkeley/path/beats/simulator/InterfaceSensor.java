/**
 * Copyright (c) 2012, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 **/

package edu.berkeley.path.beats.simulator;

import java.io.Serializable;

/** Sensor interface.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public interface InterfaceSensor {

	/** Measured density per vehicle type in veh/meter.
	 * 
	 * <p> The output array contains measured densities.
	 * The array is organized by vehicle type in the order in which they appear in the 
	 * <code>settings</code> block of the configuration file (see {@link Scenario#getVehicleTypeNames}).
	 * 
	 * @return Array of densities.
	 */
	public Double[] getDensityInVPM(int ensemble);
	
	/** Measured total density in veh/meter.
	 * 
	 * <p> Returns the total density measured by the sensor.
	 * Must equal the sum of values in {@link InterfaceSensor#getDensityInVPM}.
	 * 
	 * @return A Double with the total measured density in veh/meter.
	 */
	public Double getTotalDensityInVPM(int ensemble);

	/** Measured total density in veh/link. 
	 * 
	 * <p> Returns the total density measured by the sensor averaged over the links it's in.
	 * 
	 * @return A Double with the total measured density in veh/link.
	 */
	public Double getTotalDensityInVeh(int ensemble);
	
	/** Measured total occupancy in a number between 0 and 100. 
	 * 
	 * <p> Returns the occupancy  measured by the sensor.	 * 
	 * 
	 * @return A Double with the total occupancy, with values between 0 and 100.
	 */
	public Double getOccupancy(int ensemble);
	
	/** Measured flow per vehicle type in veh/sec.
	 * 
	 * <p> The output array contains measured flows.
	 * The array is organized by vehicle type in the order in which they appear in the 
	 * <code>settings</code> block of the configuration file (see {@link Scenario#getVehicleTypeNames}).
	 * 
	 * @return Array of flows.
	 */	
	public Double[] getFlowInVPS(int ensemble);
	
	/** Measured total flow in veh/sec.
	 * 
	 * <p> Returns the total flow measured by the sensor.
	 * Must equal the sum of values in {@link InterfaceSensor#getFlowInVPS}.
	 * 
	 * @return A Double with the total measured flow in veh/sec.
	 */
	public Double getTotalFlowInVPS(int ensemble);
	
	/** Measured speed in meters/sec.
	 * 
	 * <p> Returns the speed measured by the sensor.
	 * 
	 * @return A Double with the measured speed in meters/sec.
	 */
	public Double getSpeedInMPS(int ensemble);
	
}
