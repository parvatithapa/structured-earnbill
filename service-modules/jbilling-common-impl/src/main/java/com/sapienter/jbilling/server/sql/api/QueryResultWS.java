package com.sapienter.jbilling.server.sql.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class QueryResultWS implements Serializable {

    private Object[] [] result;
    private String[] columnNames;
    private Integer rowCount;
    private Long executionTime;
    
    public QueryResultWS() {
	
    }
 
    public QueryResultWS(Object[][] result, String[] columnNames, Integer rowCount) {
    	this.result = result;
    	this.columnNames = columnNames;
    	this.rowCount = rowCount;
    }
    
    public Object[][] getResult() {
        return result;
    }
    
    public void setResult(Object[][] result) {
        this.result = result;
    }
    
    public String[] getColumnNames() {
        return columnNames;
    }
    
    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }
    
    public Integer getRowCount() {
        return rowCount;
    }
    
    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }
    
    public Long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(Long executionTime) {
		this.executionTime = executionTime;
	}

	public List<String> getColumnValuesByColumnName(String columnName) { 
    	try {
    		List<String> columnValues = new ArrayList<String>();
    		int indexOfColumn  = Arrays.asList(getColumnNames()).indexOf(columnName);
    		for(Object[] objects: getResult()) {
    			columnValues.add(objects[indexOfColumn].toString());
    		}
    		return columnValues;
    	} catch(ArrayIndexOutOfBoundsException ex) {
    		throw new IllegalArgumentException("Entered Column "+ columnName +" is not present.");
    	}
    }

	@Override
	public String toString() {
		return "PreEvaluatedSQLResultWS [columnNames="
				+ Arrays.toString(columnNames) + ", rowCount=" + rowCount
				+ ", executionTime=" + executionTime + "]";
	}
}
