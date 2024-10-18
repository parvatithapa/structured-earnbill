package com.sapienter.jbilling.server.mediation.converter.customMediations.dt;

import java.io.Serializable;

import lombok.ToString;

@SuppressWarnings("serial")
@ToString
public class DtCacheClearMessage implements Serializable {

    public enum ActionType {
        INIT, RESET
    }

    private Long jobExecutionId;
    private String jobName;
    private Integer entity;
    private String processId;
    private ActionType actionType;

    public DtCacheClearMessage(Long jobExecutionId, String jobName, Integer entity, String processId, ActionType actionType) {
        this.jobExecutionId = jobExecutionId;
        this.jobName = jobName;
        this.entity = entity;
        this.processId = processId;
        this.actionType = actionType;
    }

    public String getJobName() {
        return jobName;
    }

    public Integer getEntity() {
        return entity;
    }

    public String getProcessId() {
        return processId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }
}
