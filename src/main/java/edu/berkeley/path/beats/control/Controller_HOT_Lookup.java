package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorVehType;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsFormatter;
import edu.berkeley.path.beats.simulator.utils.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by matt on 4/20/16.
 */
public class Controller_HOT_Lookup extends Controller {

	private final HashMap<Long, LinkData> linkData;

	public Controller_HOT_Lookup(Scenario scenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(scenario, c, Algorithm.HOT_Lookup);
		// the superclass constructor prepares the tables
		// column names for the tables: HOT lane flow, HOT lane speed, ML speed, price
		// properties for the tables: 	GP Link, HOT Link, Entering Links, FF Price Coefficient, FF Intercept, VehTypeIn, VehTypeOut,
		// 								Congested Price Coefficient, Congested Density Coefficient, Congested Intercept,
		//								Start time, Stop time
		linkData = new HashMap<Long, LinkData>();
	}

	@Override
	public void populate(Object jaxbO) {
		super.populate(jaxbO);

		// create the LinkData objects
		for (Table table : tables.values()) {
			Long hotlinkid = Long.valueOf(table.getParameters().get("HOT Link"));
			if (!linkData.containsKey(hotlinkid)) {
				Link hotlink = myScenario.get.linkWithId(hotlinkid);
				linkData.put(hotlinkid, new LinkData(hotlink, table, this));
			} else {
				linkData.get(hotlinkid).addTable(table);
			}

		}
	}

	@Override
	public boolean register() {
		for(LinkData ld : linkData.values())
			for( ActuatorVehType actuator : ld.myActuators)
				if(!actuator.register())
					return false;
		return true;
	}

	@Override
	protected void validate() {
		super.validate();

		if (myScenario.get.numEnsemble() > 1)
			BeatsErrorLog.addError("HOT Lookup Controller does not support ensembles size > 1 yet.");

		for (LinkData ld : linkData.values())
			ld.validate();
	}

	@Override
	protected void reset() {
		for (LinkData ld : linkData.values())
			ld.reset();
	}

	@Override
	protected void update() throws BeatsException {
		Clock clock = myScenario.get.clock();

		for (LinkData ld : linkData.values()) {
			ld.update(clock);
			ld.deploy(myScenario.get.currentTimeInSeconds());
		}

	}

	class LinkData {

		private final List<Link> myEnteringLinks;
		private final List<ActuatorVehType> myActuators;
		private final Link myHOTLink;
		private final Link myGPLink;

		private List<TableData> tableData;
		private TableData[] currentTableForVehtypes;

		private List<Table> allTables;
		private double[][] prices; // vehtype index x ensemble index
		private double[][] readyToPayPortion; // vehtype index x ensemble index

		public LinkData(Link link, Table T, Controller parent) {

			myHOTLink = link;
			Long GPLinkId = Long.valueOf(T.getParameters().get("GP Link"));
			myGPLink = myScenario.get.linkWithId(GPLinkId);

			myEnteringLinks = new ArrayList<Link>();
			Double[] enteringLinksArray = BeatsFormatter.readCSVstring_nonnegative(T.getParameters().get("Entering Links"), ",");
			for (Double linkid : enteringLinksArray) {
				myEnteringLinks.add(myScenario.get.linkWithId((long) linkid.doubleValue()));
			}

			allTables = new ArrayList<Table>();
			tableData = new ArrayList<TableData>();
			currentTableForVehtypes = new TableData[myScenario.get.numVehicleTypes()];

			prices = new double[myScenario.get.numVehicleTypes()][myScenario.get.numEnsemble()];
			readyToPayPortion = new double[myScenario.get.numVehicleTypes()][myScenario.get.numEnsemble()];

			addTable(T);

			// make actuators
			myActuators = new ArrayList<ActuatorVehType>();
			for (Link enteringLink : myEnteringLinks) {
				edu.berkeley.path.beats.jaxb.Actuator jaxbA = new edu.berkeley.path.beats.jaxb.Actuator();
				edu.berkeley.path.beats.jaxb.ScenarioElement se = new edu.berkeley.path.beats.jaxb.ScenarioElement();
				edu.berkeley.path.beats.jaxb.ActuatorType at = new edu.berkeley.path.beats.jaxb.ActuatorType();
				se.setId(enteringLink.getId());
				se.setType("link");
				at.setId(-1);
				at.setName("vehtype_changer");
				jaxbA.setId(-1);
				jaxbA.setScenarioElement(se);
				jaxbA.setActuatorType(at);
				ActuatorVehType actuator = new ActuatorVehType(myScenario, jaxbA, new BeatsActuatorImplementation(jaxbA, myScenario));
				actuator.populate(null, null);
				actuator.setMyController(parent);
				myActuators.add(actuator);
			}
		}

		private void addTable(Table T) {
			if(!allTables.contains(T)) {
				allTables.add(T);

				double startTime = Double.valueOf(T.getParameters().get("Start Time"));
				double stopTime = Double.valueOf(T.getParameters().get("Stop Time"));
				int vehTypeIn = Integer.valueOf(T.getParameters().get("VehTypeIn"));
				int vehTypeOut = Integer.valueOf(T.getParameters().get("VehTypeOut"));
				double FF_intercept = Double.valueOf(T.getParameters().get("FF Intercept"));
				double FF_price_coeff = Double.valueOf(T.getParameters().get("FF Price Coefficient"));
				double Cong_price_coeff = Double.valueOf(T.getParameters().get("Congested Price Coefficient"));
				double Cong_density_coeff = Double.valueOf(T.getParameters().get("Congested Density Coefficient"));
				double Cong_intercept = Double.valueOf(T.getParameters().get("Congested Intercept"));

				TableData td = new TableData(T, startTime, stopTime, vehTypeIn, vehTypeOut, FF_intercept, FF_price_coeff,
						Cong_price_coeff, Cong_density_coeff, Cong_intercept);

				tableData.add(td);

				if (myScenario.get.clock() != null)
					if (isTableActivatable(td, myScenario.get.clock()))
						activateTable(td);
			}
		}

		private boolean isTableActivatable(TableData td, Clock clock) {
			return ( (currentTableForVehtypes[td.vehTypeIn] == null) // true if no table active for this vtype
					&& (td.startTime <= clock.getT()) && (td.stopTime > clock.getT()));
		}

		private void activateTable(TableData td) {
			currentTableForVehtypes[td.vehTypeIn] = td;
			td.isActive = true;
		}

		private void update(Clock clock) {
			updateTables(clock);
			updatePrices();
			updateReadyToPayPortion();
		}

		private void deploy(double current_time_in_seconds) {
			int ensemble_index = 0;
			double alreadyReadyToPay, notAlreadyReadyToPay, portionToSwitch;

			for (int v=0; v<currentTableForVehtypes.length; v++) {
				if (currentTableForVehtypes!= null) {
					alreadyReadyToPay = 0d;
					notAlreadyReadyToPay = 0d;
					for ( Link link : myHOTLink.getBegin_node().getInput_link()) {
						alreadyReadyToPay = alreadyReadyToPay +
								link.getDensityInVeh(ensemble_index, currentTableForVehtypes[v].vehTypeOut);
						notAlreadyReadyToPay = notAlreadyReadyToPay +
								link.getDensityInVeh(ensemble_index, currentTableForVehtypes[v].vehTypeIn);
					}
					portionToSwitch = (readyToPayPortion[v][0] * (alreadyReadyToPay + notAlreadyReadyToPay) - alreadyReadyToPay)
							/ notAlreadyReadyToPay;
					for (ActuatorVehType actuator : myActuators) {
						actuator.set_switch_ratio(myScenario.get.vehicleTypeIdForIndex(currentTableForVehtypes[v].vehTypeIn),
								myScenario.get.vehicleTypeIdForIndex(currentTableForVehtypes[v].vehTypeOut),
								portionToSwitch);
					}
				}
			}
			for (ActuatorVehType actuator : myActuators)
				actuator.deploy(current_time_in_seconds);
		}

		private void updateTables(Clock clock) {
			deactivateExpiredTables(clock);
			scanAndActivateTables(clock);
		}

		private void deactivateExpiredTables(Clock clock) {
			for (int i=0; i<currentTableForVehtypes.length; i++) {
				if (currentTableForVehtypes[i] != null) {
					TableData td = currentTableForVehtypes[i];
					if( clock.getT() > td.stopTime) {
						deactivateTable(i);
					}
				}
			}
		}

		private void deactivateTable(int vehtype_index) {
			if (currentTableForVehtypes[vehtype_index] == null)
				return;

			for (ActuatorVehType actuator : myActuators)
				actuator.set_switch_ratio( myScenario.get.vehicleTypeIdForIndex(currentTableForVehtypes[vehtype_index].vehTypeIn),
						myScenario.get.vehicleTypeIdForIndex(currentTableForVehtypes[vehtype_index].vehTypeOut),
						0);

			currentTableForVehtypes[vehtype_index].isActive = false;
			currentTableForVehtypes[vehtype_index] = null;
		}

		private void scanAndActivateTables(Clock clock) {
			for(TableData td : tableData) {
				if(!td.isActive && (isTableActivatable(td, clock)))
					activateTable(td);
			}
		}

		private void updatePrices() {
			for ( int v=0;v<currentTableForVehtypes.length; v++ ) {
				if( currentTableForVehtypes[v] != null ) {
					for (int e = 0; e < myScenario.get.numEnsemble(); e++) {
						prices[v][e] = findCurrentPrice(currentTableForVehtypes[v], e);
					}
				}
			}
		}

		private double findCurrentPrice(TableData td, int ensembleIndex) {
			return td.getPriceFromClosestRow1Norm(myHOTLink.getTotalOutflowInVeh(ensembleIndex),
					myHOTLink.computeSpeedInMPS(ensembleIndex), myGPLink.computeSpeedInMPS(ensembleIndex));
		}

		private void updateReadyToPayPortion() {
			for ( int v=0; v<currentTableForVehtypes.length; v++) {
				if ( currentTableForVehtypes[v] != null) {
					for (int e = 0; e < myScenario.get.numEnsemble(); e++)
						readyToPayPortion[v][e] = computeReadyToPayPortionTwoPhase(currentTableForVehtypes[v],e);
				}
			}
		}

		private double computeReadyToPayPortionTwoPhase(TableData td, int ensembleIndex) {
			double value;
			if (myGPLink.getTotalDensityInVeh(ensembleIndex) < myGPLink.getDensityCriticalInVeh(ensembleIndex))
				value = td.FF_intercept + td.FF_price_coeff * prices[td.vehTypeIn][ensembleIndex]; // freeflow
			else
				value =  td.Cong_intercept + td.Cong_price_coeff * prices[td.vehTypeIn][ensembleIndex] // congestion
						+ td.Cong_density_coeff
						* ( myGPLink.getTotalDensityInVeh(ensembleIndex) - myHOTLink.getTotalDensityInVeh(ensembleIndex));
			return 1d / (1d + Math.exp(-value)); // logistic regression
		}

		private void validate() {
			if (myHOTLink == null)
				BeatsErrorLog.addError("HOT Controller has invalid HOT link id: " + allTables.get(0).getParameters().get("HOT Link"));

			if (myGPLink == null)
				BeatsErrorLog.addError("HOT Controller has invalid GP link id: " + allTables.get(0).getParameters().get("GP Link"));

			if (myEnteringLinks.contains(null))
				BeatsErrorLog.addError("HOT Controller for HOT link id " + myHOTLink.getId() + " has one or more invalid entering link IDs");

			if (allTables.isEmpty())
				BeatsErrorLog.addError("HOT Controller for link id=" + myHOTLink.getId() +" has no price tables");

			for (TableData td : tableData)
				td.validate(this);

			for (ActuatorVehType actuator : myActuators)
				actuator.validate();
		}

		private void reset() {
			for (int v=0; v<currentTableForVehtypes.length; v++)
				deactivateTable(v);

			prices = new double[myScenario.get.numVehicleTypes()][myScenario.get.numEnsemble()];
			readyToPayPortion = new double[myScenario.get.numVehicleTypes()][myScenario.get.numEnsemble()];

			update(myScenario.get.clock()); // clock has already been reset
			try {
				for (ActuatorVehType actuator : myActuators)
					actuator.reset();
			} catch (BeatsException ex) {
				ex.printStackTrace();
			}
		}
	}

	private class TableData {

		public final Table table;
		public final double startTime, stopTime;
		public final int vehTypeIn, vehTypeOut;
		public final double FF_intercept, FF_price_coeff, Cong_price_coeff, Cong_density_coeff, Cong_intercept;
		protected boolean isActive = false;

		private final List<TableRow> rows;

		TableData(Table t, double start, double stop, int vtin, int vtout, double FF_int, double FF_p_c,
				  double Cong_pr_coeff, double Cong_d_c, double Cong_int) {
			table = t;
			startTime = start;
			stopTime = stop;
			vehTypeIn = vtin;
			vehTypeOut = vtout;
			FF_intercept = FF_int;
			FF_price_coeff = FF_p_c;
			Cong_price_coeff = Cong_pr_coeff;
			Cong_density_coeff = Cong_d_c;
			Cong_intercept = Cong_int;

			ArrayList<Table.Row> rawRows = table.getRows();

			rows = new ArrayList<TableRow>(rawRows.size());

			for (Table.Row rawRow : rawRows) {
				double hot_flow = Double.valueOf(rawRow.get_value_for_column_name("HOT Lane Flow"));
				double hot_speed = Double.valueOf(rawRow.get_value_for_column_name("HOT Lane Speed"));
				double ml_speed = Double.valueOf(rawRow.get_value_for_column_name("GP Lane Speed"));
				double price = Double.valueOf(rawRow.get_value_for_column_name("Price"));
				rows.add(new TableRow(hot_flow, hot_speed, ml_speed, price));
			}
		}

		private double getPriceFromClosestRow1Norm(double current_hot_flow, double current_hot_speed,
												   double current_gp_speed) {

			// quick check - if only one row, just give that price
			if (rows.size()==1)
				return rows.get(0).price;

			int i;
			List<Double> distances = new ArrayList<Double>(rows.size());
			for (i=0; i<rows.size(); i++) {
				distances.add(i, rows.get(i).computeDistance1Norm(current_hot_flow, current_hot_speed, current_gp_speed));
			}
			int min_index = 0;
			for (i=1; i<distances.size(); i++) {
				if (distances.get(i) < distances.get(min_index))
					min_index = i;
			}
			return rows.get(min_index).price;
		}

		private void validate(LinkData myParent) {
			if (rows.size()==0)
				BeatsErrorLog.addError("A table for link id=" + myParent.myHOTLink.getId() +" has no rows.");
		}
	}

	private class TableRow {
		public final double hot_flow, hot_speed, ml_speed, price;

		TableRow(double hot_flow, double hot_speed, double ml_speed, double price) {
			this.hot_flow = hot_flow;
			this.hot_speed = hot_speed;
			this.ml_speed = ml_speed;
			this.price = price;
		}

		public double computeDistance1Norm(double compare_hot_flow, double compare_hot_speed, double compare_ml_speed) {
			return Math.abs(compare_hot_flow - hot_flow) + Math.abs(compare_hot_speed - hot_speed) +
					Math.abs(compare_ml_speed - ml_speed);
		}
	}

	public double getPriceAtLinkForVehtype(long linkid, long vehType) {
		if (!linkData.containsKey(linkid))
			return 0d;

		LinkData ld = linkData.get(linkid);
		return ld.prices[ myScenario.get.vehicleTypeIndexForId(vehType)][0];
	}

	public HashMap<Long, LinkData> getLinkData() {return linkData;}

}
