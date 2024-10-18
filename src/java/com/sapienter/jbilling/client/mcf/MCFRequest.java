package com.sapienter.jbilling.client.mcf;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class MCFRequest {
    @JsonProperty("TASK")
    private String task;
    @JsonProperty("BUSINESS_UNIT")
    private String businessUnit;
    @JsonProperty("TRANSACTION")
    private String transaction;
    @JsonProperty("JOBS")
    private Map<String, String> jobs;

    public void setTask(String task) {
        this.task = task;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public void setJobs(Map<String, String> jobs) {
        this.jobs = jobs;
    }
}
