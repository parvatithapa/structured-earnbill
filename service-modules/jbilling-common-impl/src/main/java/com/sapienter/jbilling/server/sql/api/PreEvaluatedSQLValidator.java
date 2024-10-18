package com.sapienter.jbilling.server.sql.api;

import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.server.sql.api.db.ParameterType;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterDTO;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;

public class PreEvaluatedSQLValidator {

    private PreEvaluatedSQLValidator() {
	
    }
    
    public static List<String> validateParameters(QueryParameterWS[] parameters) {
    	List<String> errorMessages = new ArrayList<String>();
    	for(QueryParameterWS parameter: parameters) {
    		if(null==parameter.getParameterName() || parameter.getParameterName().isEmpty()) {
    			errorMessages.add("Parameter Name  is required.");
    		} else if(null==parameter.getParameterValue() || parameter.getParameterValue().isEmpty()) {
    			errorMessages.add("Parameter Value  is required.");
    		} else if(parameter.getParameterType()==null) {
    			errorMessages.add("Parameter Type  is required.");
    		}
    	}
    	return errorMessages;
    }
    
    public static String validateQuery(PreEvaluatedSQLDTO query) {
    	String dbQuery = query.getSqlQuery().trim().substring(0, 10).toLowerCase();
    	if(dbQuery.contains("update") || dbQuery.contains("delete") ||
    			dbQuery.contains("insert") || dbQuery.contains("alter") || dbQuery.contains("drop") ||
    			dbQuery.contains("create") || dbQuery.contains("truncate") || dbQuery.contains("rename") 
    			|| dbQuery.contains("merge")) {
    		return " DDL or DML Query is not allowed.";
    	}
    	return "";
    }
    
    public static List<String> validateParameters(PreEvaluatedSQLDTO query, QueryParameterWS[] parameters) {
    	List<String> errorMessages = new ArrayList<String>();
    	if(!query.isParameterRequired()) {
    		return errorMessages;
    	} else if(parameters==null) {
    		errorMessages.add("Parameters are required.");
    		return errorMessages;
    	}
    	errorMessages.addAll(validateParameters(parameters));
    	
    	if(query.getParameters().size()==parameters.length) {
    		for(QueryParameterWS parameter: parameters) {
    			PreEvaluatedSQLParameterDTO dbParameter = query.getParameterByName(parameter.getParameterName());
    			if(null!=dbParameter) {
    				ParameterType parameterType = parameter.getParameterType();
    				if(!parameterType.equals(dbParameter.getParameterType())) {
    					errorMessages.add("Incorrect Type for  "+parameter.getParameterName()+ " Expected Type: "+dbParameter.getParameterType() +" But Type Was: "+parameterType);
    				}
    			} else {
    				errorMessages.add("You Have Passed Null or Invalid Parameter "+parameter.getParameterName());
    			}
    		}
    	} else {
    		errorMessages.add("Expected Parameters Was: "+query.getParameters().size() +" But Was: "+parameters.length);
    	}
    	return errorMessages;
    }
}
