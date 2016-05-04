package edu.berkeley.path.beats.control;

import edu.berkeley.path.beats.actuator.ActuatorVehType;
import edu.berkeley.path.beats.simulator.*;
import edu.berkeley.path.beats.simulator.utils.BeatsException;
import edu.berkeley.path.beats.simulator.utils.BeatsMath;
import edu.berkeley.path.beats.simulator.utils.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by matt on 4/20/16.
 */
public class Controller_HOT_Lookup extends Controller {

	private HashMap<Long, LinkData> linkData;

	public Controller_HOT_Lookup(Scenario scenario, edu.berkeley.path.beats.jaxb.Controller c) {
		super(scenario, c, Algorithm.HOT_LOOKUP);
		// the superclass constructor prepares the tables
		// column names for the tables: HOT lane flow, HOT lane speed, ML speed, price
		// properties for the tables: 	GP Link, HOT Link, FF Price Coefficient, FF Intercept, VehTypeIn, VehTypeOut,
		// 								Congested Price Coefficient, Congested GP Density Coefficient, Congested Intercept,
		//								Start time, Stop time
	}

	@Override
	public void populate(Object jaxbO) {
		super.populate(jaxbO);

		// create the LinkData objects
		for (Table table : tables.values()) {
			Long linkid = Long.valueOf(table.getParameters().get("GP Link"));
			if (linkData.containsKey(linkid))
				linkData.get(linkid).addTable(table);
			else {
				Link link = myScenario.get.linkWithId(linkid);
				linkData.put(linkid, new LinkData(link, table, this));
			}
		}
	}

	@Override
	public boolean register() {
		for(LinkData ld : linkData.values())
			if(!ld.myActuator.register())
				return false;
		return true;
	}

	@Override
	protected void validate() {
		super.validate();

		for (LinkData ld : linkData.values())
			ld.validate();
	}

	@Override
	protected void reset() {

		for (LinkData ld : linkData.values())
			ld.reset();
	}

	protected void update(Clock clock) throws BeatsException {

		for (LinkData ld : linkData.values())
			ld.update(clock);

	}

	class LinkData {

		protected Link myGPLink;
		protected ActuatorVehType myActuator;
		protected Link myHOTLink;

		protected List<TableData> tableData;
		protected TableData[] currentTableForVehtypes;

		protected List<Table> allTables;
		private List<List<Double>> currentPrices; // vehtype x ensemble index

		public LinkData(Link link, Table T, Controller parent) {

			this.myGPLink = link;

			Long HOTLinkId = Long.valueOf(T.getParameters().get("HOT Link"));
			myHOTLink = myScenario.get.linkWithId(HOTLinkId);

			allTables = new ArrayList<Table>();
			tableData = new ArrayList<TableData>();
			currentTableForVehtypes = new TableData[myScenario.get.numVehicleTypes()];

			for(int e=0;e<myScenario.get.numEnsemble(); e++)
				currentPrices.add(new ArrayList<Double>());

			addTable(T);

			// make actuator
			edu.berkeley.path.beats.jaxb.Actuator jaxbA = new edu.berkeley.path.beats.jaxb.Actuator();
			edu.berkeley.path.beats.jaxb.ScenarioElement se = new edu.berkeley.path.beats.jaxb.ScenarioElement();
			edu.berkeley.path.beats.jaxb.ActuatorType at = new edu.berkeley.path.beats.jaxb.ActuatorType();
			se.setId(myGPLink.getId());
			se.setType("link");
			at.setId(-1);
			at.setName("vehtype_changer");
			jaxbA.setId(-1);
			jaxbA.setScenarioElement(se);
			jaxbA.setActuatorType(at);
			myActuator = new ActuatorVehType(myScenario,jaxbA,new BeatsActuatorImplementation(jaxbA,myScenario));
			myActuator.populate(null,null);
			myActuator.setMyController(parent);
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
				double Cong_GP_density_coeff = Double.valueOf(T.getParameters().get("Congested GP Density Coefficient"));
				double Cong_intercept = Double.valueOf(T.getParameters().get("Congested Intercept"));

				TableData td = new TableData(T, startTime, stopTime, vehTypeIn, vehTypeOut, FF_intercept, FF_price_coeff,
						Cong_price_coeff, Cong_GP_density_coeff, Cong_intercept);

				tableData.add(td);

				double t = myScenario.get.currentTimeInSeconds();
				if ((td.startTime >= t)	&& (td.stopTime <= t) ) {
					if (isTableActivatable(td, myScenario.get.clock()))
						activateTable(td);
				}
			}
		}

		private boolean isTableActivatable(TableData td, Clock clock) {
			return ( (currentTableForVehtypes[td.vehTypeIn] == null) // true if no table active for this vtype
					&& (td.startTime >= clock.getT()) && (td.stopTime < clock.getT()));
		}

		private void activateTable(TableData td) {
			currentTableForVehtypes[td.vehTypeIn] = td;
			td.isActive = true;
		}

		private void update(Clock clock) {
			updateTables(clock);
			updatePrices();
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
						td.isActive = false;
						currentTableForVehtypes[i] = null;
					}
				}
			}
		}

		private void scanAndActivateTables(Clock clock) {
			for(int i = 0; i<tableData.size(); i++) {
				TableData td = tableData.get(i);
				if(!td.isActive && (isTableActivatable(td, clock)))
					activateTable(td);
			}
		}

		private void updatePrices() {
			for ( int v=0;v<currentTableForVehtypes.length; v++ ) {
				if( currentTableForVehtypes[v] != null ) {
					for (int e = 0; e < myScenario.get.numEnsemble(); e++) {
						currentPrices.get(v).set(e, findCurrentPrice(currentTableForVehtypes[v], e));
					}
				}
			}
		}

		private double findCurrentPrice(TableData td, int ensembleIndex) {
			return td.getPriceFromClosestRow1Norm(myHOTLink.getTotalOutflowInVeh(ensembleIndex),
					myHOTLink.computeSpeedInMPS(ensembleIndex), myGPLink.computeSpeedInMPS(ensembleIndex));
		}

	}

	private class TableData {

		public final Table table;
		public final double startTime, stopTime;
		public final int vehTypeIn, vehTypeOut;
		public final double FF_intercept, FF_price_coeff, Cong_price_coeff, Cong_GP_density_coeff, Cong_intercept;
		protected boolean isActive = false;

		private List<TableRow> rows;

		TableData(Table t, double start, double stop, int vtin, int vtout, double FF_int, double FF_p_c,
				  double Cong_pr_coeff, double Cong_GP_d_c, double Cong_int) {
			table = t;
			startTime = start;
			stopTime = stop;
			vehTypeIn = vtin;
			vehTypeOut = vtout;
			FF_intercept = FF_int;
			FF_price_coeff = FF_p_c;
			Cong_price_coeff = Cong_pr_coeff;
			Cong_GP_density_coeff = Cong_GP_d_c;
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
				distances.set(i, rows.get(i).computeDistance1Norm(current_hot_flow, current_hot_speed, current_gp_speed));
			}
			int min_index = 0;
			for (i=1; i<distances.size(); i++) {
				if (distances.get(i) < distances.get(min_index))
					min_index = i;
			}
			return rows.get(min_index).price;
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

}
