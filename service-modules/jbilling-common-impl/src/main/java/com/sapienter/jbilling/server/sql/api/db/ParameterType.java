package com.sapienter.jbilling.server.sql.api.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public enum ParameterType {

    INTEGER {

	@Override
	public Serializable getValue(String value) {
	    return Integer.valueOf(value);
	}
	
    } ,DECIMAL {

	@Override
	public Serializable getValue(String value) {
	    return new BigDecimal(value);
	}
	
    }, DATE {

	@Override
	public Serializable getValue(String value) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		try {
			return dateFormat.parse(value);
		} catch(ParseException parseException) {
			throw new IllegalArgumentException("Invalid Date Entered  Expected Format (yyyy-MM-dd HH-mm-ss)", parseException.getCause());
		}
	    
	}
	
    },STRING {

	@Override
	public Serializable getValue(String value) {
	    return value;
	}
	
    }, LIST_OF_INTERGER {
	@Override
	public Serializable getValue(String value) {
		try {
		ArrayList<Integer> intValList = new ArrayList<Integer>();
		for(String intVal : value.split(",")) {
			intValList.add(Integer.valueOf(intVal));
		}
	    	return intValList;
		} catch(NumberFormatException ex) {
			throw new IllegalArgumentException("InValid Value passed. character or String not allowed.");
		}
	}
    };
    
    public abstract Serializable getValue(String value);
}
