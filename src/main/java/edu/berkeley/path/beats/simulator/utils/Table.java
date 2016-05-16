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

package edu.berkeley.path.beats.simulator.utils;
import java.io.Serializable;
import java.util.ArrayList;

import edu.berkeley.path.beats.jaxb.ColumnName;
import edu.berkeley.path.beats.simulator.Parameters;
import edu.berkeley.path.beats.simulator.utils.BeatsErrorLog;

/** 
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
final public class Table implements Serializable {

	private static final long serialVersionUID = 3891566133140383721L;
	protected int key_index;                // single primary key only
	protected ArrayList<Long> column_ids;
	protected ArrayList<String> column_names;
	protected ArrayList<Row> rows;
	protected Parameters parameters;

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////

	public Table(ArrayList<String> colnames) {

		// check column names has single key
//		boolean found_key = false;
//		for(int i=0;i<T.getColumnNames().getColumnName().size();i++){
//			if(T.getColumnNames().getColumnName().get(i).isKey()){
//				if(found_key)
//					BeatsErrorLog.addError("No more than one key allowed in a table.");
//				else{
//					found_key = true;
//					key_index = i;
//				}
//			}
//		}

		// populate column_names, column_ids, column_is_key
		column_names = new ArrayList<String>();
		column_ids = new ArrayList<Long>();
		ArrayList<Boolean> column_is_key = new ArrayList<Boolean>();
		for(int i=0;i<colnames.size();i++){
			column_ids.add((long)i);
			column_is_key.add(false);
			column_names.add(colnames.get(i));
		}

	}

	public Table(edu.berkeley.path.beats.jaxb.Table T) {
		
		// check column names has single key
		boolean found_key = false;
        for(int i=0;i<T.getColumnNames().getColumnName().size();i++){
            if(T.getColumnNames().getColumnName().get(i).isKey()){
                if(found_key)
                    BeatsErrorLog.addError("No more than one key allowed in a table.");
                else{
                    found_key = true;
                    key_index = i;
                }
            }
        }
		
		// populate column_names, column_ids, column_is_key
		column_names = new ArrayList<String>();
		column_ids = new ArrayList<Long>();
		ArrayList<Boolean> column_is_key = new ArrayList<Boolean>();
		for(ColumnName cn : T.getColumnNames().getColumnName()){
			column_ids.add(cn.getId());
			column_is_key.add(cn.isKey());
			column_names.add(cn.getName());
		}

		// populate key_row
		rows = new ArrayList<Row>();
		for(edu.berkeley.path.beats.jaxb.Row row : T.getRow())
			rows.add(new Row(row));

		this.parameters = (Parameters) T.getParameters();
	}
	
	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

	public void addRow(ArrayList<String> colnames,ArrayList<String> rowvalues){
		if(colnames.size()!=rowvalues.size())
			return;
		Row row = new Row(column_names.size());
		for(int i=0;i<colnames.size();i++){
			int index = column_names.indexOf(colnames.get(i));
			if(index<0)
				continue;
			row.set_value(index,rowvalues.get(i));
		}
	}

	/** Returns the rows in the table*/
	public ArrayList<Row> getRows(){
		return rows;
	}

	public Row getRowWithName(String name){
		for( Row row : rows) {
			if (row.get_name().equalsIgnoreCase(name))
				return row;
		}
		return null;
	}
	
	/** Returns the number of columns in the table*/ 
	public int getNoColumns(){		
		return 	column_names.size();
	}

	/** Returns the name of columns in the table*/
	public ArrayList<String>  getColumnNames(){
		return column_names;
	}
	
	/** Returns the column number corresponding to the given column_name*/ 
	public int getColumnNo(String cname){		
		return 	column_names.indexOf(cname);
	}

	public Parameters getParameters() {
		return parameters;
	}
	
//	public String getColumnNameForId(Long ID){
//		int index = column_ids.indexOf(ID);
//		if(index<0)
//			return null;
//		return column_names.get(index);
//	}
//
//    public Row get_row_with_key(String key){
//        for(Row row : rows)
//            if(row.column_value[key_index].compareTo(key)==0)
//                return row;
//        return null;
//    }
//
//    public ArrayList<String> get_keys(){
//        ArrayList<String> keys = new ArrayList<String>();
//        for(Row row : rows)
//            keys.add(row.column_value[key_index]);
//        return keys;
//    }

	public class Row implements Serializable {
		private static final long serialVersionUID = 5243339373051310319L;
		public String name = null;
		public String [] column_value;
		public Row(int numcol, String RowName) {
			name = RowName;
			column_value = new String[numcol];
		}
		public Row(int numcol){
			column_value = new String[numcol];
		}
		public Row(edu.berkeley.path.beats.jaxb.Row jrow){
            column_value = new String[column_names.size()];
            for(edu.berkeley.path.beats.jaxb.Column col : jrow.getColumn()){
                int index = column_ids.indexOf(col.getId());
                if(index<0)
                    continue;
                column_value[index] = col.getContent().trim();
            }
			name = jrow.getName();
        }
        public String get_value_for_column_name(String colname){
            int ind = column_names.indexOf(colname);
            return ind<0 ? null : column_value[ind];
        }
		public String get_name() {return name;}
		public void set_value(int index,String val){
			column_value[index] = val;
		}
	}
	
}


