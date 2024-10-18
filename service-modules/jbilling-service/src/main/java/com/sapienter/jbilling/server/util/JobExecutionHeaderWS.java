package com.sapienter.jbilling.server.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JobExecutionHeaderWS implements Serializable {

    private int id;
    private long jobExecutionId;
    private String jobType;
    private Date startDate;
    private Date endDate;
    private String status;
    private JobExecutionLineWS[] lines;
    private Integer entityId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JobExecutionLineWS[] getLines() {
        return lines;
    }

    public void setLines(JobExecutionLineWS[] lines) {
        this.lines = lines;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "JobExecutionHeaderWS{" +
                "id=" + id +
                ", jobExecutionId=" + jobExecutionId +
                ", jobType='" + jobType + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                ", lines=" + Arrays.toString(lines) +
                ", entityId=" + entityId +
                '}';
    }
}
