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

/****************************************************************************/
/************        Author: Alexey Goder alexey@goder.com  *****************/
/************                    Dec 10, 2012               *****************/
/****************************************************************************/

package edu.berkeley.path.beats.processor;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.map.ColumnMap;
import com.workingdogs.village.DataSetException;
import com.workingdogs.village.Record;

import edu.berkeley.path.beats.om.LinkDataDetailedPeer;

public class LinkDataDetailed extends edu.berkeley.path.beats.om.LinkDataDetailed{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7977280743015537538L;
	
	/**
	 * populates aggregated object, sets calculated data and insert
	 * @param table
	 * @param aggregation ID
	 * @param time
	 * @param originalData
	 * @param aggregatedData
	 * @return number of rows processed
	 */
    
    public  int saveAggregated(String table, long aggregationId, Long time, List originalData, List aggregatedData )  {
			
		Timestamp ts = new Timestamp(time);
		
		ArrayList<String> colList = getColumnsForAggreagtion(); 

		try {
			
			// Use the originalData record to populate non-aggregated values of the this row.
			LinkDataDetailedPeer.populateObject((Record)originalData.get(0), 1, this);
			
			
				
			//Populate aggregated data
				
			for (int i=0; i < colList.size(); i++ ) {
				
				setByPeerName(table+"."+colList.get(i), (BigDecimal)(((Record)aggregatedData.get(0)).getValue(i+1).asBigDecimal()));
				
			}		
			
			setTs(ts);
			setAggTypeId(aggregationId);
			
			setNew(true);
			save();
			
			
		} catch (Exception e) {

			e.printStackTrace();
			return 0;
		}

		return 1;
		
	}
    
    /**
     * returns list of primary keys with values except time stamp and aggregation
     * @return string
     * @throws TorqueException
     * @throws DataSetException 
     */
    public String getListOfKeys(Record rec) throws TorqueException, DataSetException {
    	
    	String str = new String("");
    	int n=1;
    	
    	ColumnMap[] columns = getTableMap().getColumns();
    	
    	for (int i=0; i< columns.length; i++) {

    		if ( columns[i].isPrimaryKey() ) {
    			
    			if ( columns[i].getColumnName().equals("ts") || columns[i].getColumnName().equals("agg_type_id") ) {
    				// do not include time stamp or aggregation
    			}
    			else  {
    				    			
    				// include key name and value
    				
    				str  += " AND " + columns[i].getColumnName() + "=" + rec.getValue(n++).asString() ;	
    			}

    		}
    	}
    		
    	return str;
    }
   
    
    public static void removeNulls(edu.berkeley.path.beats.om.LinkDataDetailed obj) {
    	
    	
    	BigDecimal zero = new BigDecimal(0);
    	ColumnMap[] columns;
    	

    	try {
    		
    		columns = obj.getTableMap().getColumns();
    		
			for (int i=0; i< obj.getTableMap().getColumns().length; i++) {

	    		if ( columns[i].isPrimaryKey() ||  columns[i].isForeignKey() ) {
	    			
	    		} else {
					if ( obj.getByPosition(i) == null ) {
						
						try {
							obj.setByPosition(i, zero);
						} catch (TorqueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
	    		}
			}
			
		} catch (TorqueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    		   	
	
    }
    
    
    /**
     * Creates a list of columns that should be aggregated
     * @return List of strings
     * @throws TorqueException
     * @throws DataSetException
     */
   public ArrayList<String> getColumnsForAggreagtion()  {
    	
	   ArrayList<String> colList = new ArrayList<String>();    	
    	
    	ColumnMap[] columns;
		try {
			columns = getTableMap().getColumns();
		
    	
    	for (int i=0; i< columns.length; i++) {

    		if ( columns[i].isPrimaryKey() ) {
    			
    		} else {
    			
    			if ( columns[i].getTorqueType().equals("DECIMAL") ) {
    				
    				colList.add(columns[i].getColumnName());
    				
    			}
    		}
    	}
    		
    	
		} catch (TorqueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	return colList;
		
    }
   
 
    
    /**
     * returns list of primary keys with values except time stamp and aggregation
     * @return string
     * @throws TorqueException
     * @throws DataSetException 
     */
    public String getLinkIdAndNetwokId(Record rec) throws TorqueException, DataSetException {
    	
    	String str = new String("");   	
    	
   	
    	
    	int n=1;
    	
    	ColumnMap[] columns = getTableMap().getColumns();
    	
    	for (int i=0; i< columns.length; i++) {

    		if ( columns[i].isPrimaryKey() ) {
    			
    			if ( columns[i].getColumnName().equals("ts") || columns[i].getColumnName().equals("agg_type_id") ) {
    				// do not include time stamp
    			}
    			else  {
    				
    			
    				// include key name and value
    				if (columns[i].getColumnName().equals("link_id")) {
    					str  += " AND id=" + rec.getValue(n).asString() ;	
    				} else 
    				if (columns[i].getColumnName().equals("network_id")) {
    					str  += " AND network_id=" + rec.getValue(n).asString() ;	
    				}
    					
    				n++;	
    					
    			}

    		}
    	}
  
    	
    	return str;
    }
    	
    	
    	   /**
         * returns list of primary keys except time stamp and aggregation
         * @return string
         * @throws TorqueException
         */
        public String getListOfKeys() throws TorqueException {
        	
        	String str = new String("");
        	
        	ColumnMap[] columns = getTableMap().getColumns();
        	
        	for (int i=0; i< columns.length; i++) {

        		if ( columns[i].isPrimaryKey() ) {
        			
        			if ( columns[i].getColumnName().equals("ts") || columns[i].getColumnName().equals("agg_type_id") ) {
        				// do not include time stamp or aggregation
        			}
        			else  {
        				// include key name
        				if (str.length() > 1 ) str += ", ";
        				
        				str  += columns[i].getColumnName();	
        			}

        		}
        		
        	}
        	  	
    	  	
    	
    	return str;
    }
        
        /**
         * Form list of keys except exclusion, aggregation and timetamp
         * @param exclusion
         * @return
         * @throws TorqueException
         */
                public String getListOfKeys(String exclusion) throws TorqueException {
                	
                	String str = new String("");
                	
                	ColumnMap[] columns = getTableMap().getColumns();
                	
                	for (int i=0; i< columns.length; i++) {

                		if ( columns[i].isPrimaryKey() ) {
                			
                			if ( columns[i].getColumnName().equals("ts") 
                					|| columns[i].getColumnName().equals("agg_type_id") 
                					|| columns[i].getColumnName().equals(exclusion) 
                					) {
                				// do not include time stamp or aggregation
                			}
                			else  {
                				// include key name
                				if (str.length() > 1 ) str += ", ";
                				
                				str  += columns[i].getColumnName();	
                			}

                		}
                		
                	}
                	  	   	
            	return str;
            }
         

	/**
	 * returns column number for given name
	 * @param name
	 * @return
	 */
     public int getColumnNumber(String name) {    	
     	
     	ColumnMap[] columns;
		try {			
				columns = getTableMap().getColumns();
			    	
		     	for (int i=0; i< columns.length; i++) {

		 			
		 			if ( columns[i].getColumnName().equals(name)  ) return i+1;	   		
	     	}
	     	
			} catch (TorqueException e) {
				
				return 0;
			}
     	    	 	
		return 0;
     }
     /**
      * returns list of primary keys with values except exclusion, time stamp and aggregation
      * @return string
      * @throws TorqueException
      * @throws DataSetException 
      */
     public String getListOfKeys(Record rec, String exclusion) throws TorqueException, DataSetException {
     	
     	String str = new String("");
     	int n=1;
     	
     	ColumnMap[] columns = getTableMap().getColumns();
     	
     	for (int i=0; i< columns.length; i++) {

     		if ( columns[i].isPrimaryKey() ) {
     			
     			if ( columns[i].getColumnName().equals("ts") 
     					|| columns[i].getColumnName().equals("agg_type_id") 
     					|| columns[i].getColumnName().equals(exclusion)) {
     				// do not include time stamp or aggregation or the specified exclusion key
     			}
     			else  {
     				    			
     				// include key name and value
     				
     				str  += " AND " + columns[i].getColumnName() + "=" + rec.getValue(n++).asString() ;	
     			}

     		}
     	}
     		
     	return str;
     }       
}