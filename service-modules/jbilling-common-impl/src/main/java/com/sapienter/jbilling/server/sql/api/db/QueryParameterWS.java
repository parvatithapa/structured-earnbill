package com.sapienter.jbilling.server.sql.api.db;

import java.io.Serializable;

import com.sapienter.jbilling.server.sql.api.db.ParameterType;

@SuppressWarnings("serial")
public class QueryParameterWS implements Serializable {
    private String parameterName;
    private String parameterValue;
    private ParameterType parameterType;
    
    public QueryParameterWS() {
	
    }
    
    public QueryParameterWS(String parameterName, ParameterType parameterType) {
	this.parameterName = parameterName;
	this.parameterType = parameterType;
    }
    
    public QueryParameterWS(String parameterName, ParameterType parameterType, String parameterValue) {
	this.parameterName = parameterName;
	this.parameterType = parameterType;
	this.parameterValue = parameterValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    @Override
    public String toString() {
	return "PreEvaluatedSQLParameter [parameterName=" + parameterName
		+ ", parameterValue=" + parameterValue + ", parameterType="
		+ parameterType + "]";
    }
    
}
