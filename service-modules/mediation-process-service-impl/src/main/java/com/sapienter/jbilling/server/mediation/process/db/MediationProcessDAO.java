package com.sapienter.jbilling.server.mediation.process.db;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by andres on 19/10/15.
 */
@Entity
@Table(name="jbilling_mediation_process")
public class MediationProcessDAO implements Serializable {

    @Id
    @Column(name="id")
    private UUID id;
    @Column(name="entity_id")
    private Integer entityId;
    @Column(name="configuration_id")
    private Integer configurationId;
    @Column(name="global")
    private Boolean global;
    @Column(name="star_date")
    private Date startDate;
    @Column(name="end_date")
    private Date endDate;
    @Column(name="records_processed")
    private Integer recordsProcessed;
    @Column(name="done_and_billable")
    private Integer doneAndBillable;
    @Column(name="errors")
    private Integer errors;
    @Column(name="duplicates")
    private Integer duplicates;
    @Column(name="done_and_not_billable")
    private Integer doneAndNotBillable;
    @Column(name="order_affected_count")
    private Integer orderAffectedCount;
    @Column(name="aggregated")
    private Integer aggregated;

    public MediationProcessDAO() {
    }

    public MediationProcessDAO(UUID id, Integer entityId, Integer configurationId, Boolean global, Date startDate,
                               Date endDate, Integer recordsProcessed, Integer doneAndBillable, Integer errors,
                               Integer duplicates, Integer doneAndNotBillable, Integer orderAffectedCount,
                               Integer aggregated) {
        this.id = id;
        this.entityId = entityId;
        this.configurationId = configurationId;
        this.global = global;
        this.startDate = startDate;
        this.endDate = endDate;
        this.recordsProcessed = recordsProcessed;
        this.doneAndBillable = doneAndBillable;
        this.errors = errors;
        this.duplicates = duplicates;
        this.doneAndNotBillable = doneAndNotBillable;
        this.orderAffectedCount = orderAffectedCount;
        this.aggregated = aggregated;
    }

    @Override
    public String toString() {
        return "com.sapienter.jbilling.server.mediation.MediationProcess{" +
                "id='" + id + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate=" + endDate +
                ", recordsProcessed=" + recordsProcessed +
                ", doneAndBillable=" + doneAndBillable +
                ", errors='" + errors + '\'' +
                ", duplicates=" + duplicates +
                ", doneAndNotBillable=" + doneAndNotBillable +
                ", aggregated=" + aggregated +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
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

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getDoneAndBillable() {
        return doneAndBillable;
    }

    public void setDoneAndBillable(Integer doneAndBillable) {
        this.doneAndBillable = doneAndBillable;
    }

    public Integer getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(Integer duplicates) {
        this.duplicates = duplicates;
    }

    public Integer getErrors() {
        return errors;
    }

    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    public Integer getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(Integer configurationId) {
        this.configurationId = configurationId;
    }

    public Integer getDoneAndNotBillable() {
        return doneAndNotBillable;
    }

    public void setDoneAndNotBillable(Integer doneAndNotBillable) {
        this.doneAndNotBillable = doneAndNotBillable;
    }

	public Integer getOrderAffectedCount() {
		return orderAffectedCount;
	}

	public void setOrderAffectedCount(Integer orderAffectedCount) {
		this.orderAffectedCount = orderAffectedCount;
	}

    public Integer getAggregated() {
        return aggregated;
    }

    public void setAggregated(Integer aggregated) {
        this.aggregated = aggregated;
    }
}