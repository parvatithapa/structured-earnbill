package com.sapienter.jbilling.client.mcf;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class MCFResponse {
    @JsonProperty("TASK")
    private String task;
    @JsonProperty("BUSINESS_UNIT")
    private String businessUnit;
    @JsonProperty("TRANSACTION")
    private String transaction;
    @JsonProperty("ERRORS")
    private String errors;
    @JsonProperty("RESULTS")
    private Map<String, String> results;

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
