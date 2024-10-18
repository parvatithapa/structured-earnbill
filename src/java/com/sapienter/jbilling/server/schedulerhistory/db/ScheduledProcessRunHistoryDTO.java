package com.sapienter.jbilling.server.schedulerhistory.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * @author jbilling-pranay
 *
 */
@Entity
@TableGenerator(
        name            = "schedular_request_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "scheduler_history")
@Table(name = "scheduled_process_run_history")
public class ScheduledProcessRunHistoryDTO {

    public enum SchedulerStatus {
        STARTED, FINISHED, ERROR;
    }

    private int id;
    private Integer entityId;
    private Date startDatetime;
    private Date endDatetime;
    private String name;
    private String errorMessage;
    private SchedulerStatus status;

    public ScheduledProcessRunHistoryDTO() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "schedular_request_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name", length = 200, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "entity_id")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Column(name = "start_date")
    public Date getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Date startDatetime) {
        this.startDatetime = startDatetime;
    }

    @Column(name = "end_date")
    public Date getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Date endDatetime) {
        this.endDatetime = endDatetime;
    }

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    public SchedulerStatus getStatus() {
        return status;
    }

    public void setStatus(SchedulerStatus status) {
        this.status = status;
    }

    @Column(name = "error_message", length = 500)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ScheduledProcessRunHistoryDTO [entityId=" + entityId + ", name=" + name
                + ", startDatetime=" + startDatetime + ", endDatetime="
                + endDatetime + ", status=" + status + ", errorMessage=" + errorMessage + "]";
    }
}
