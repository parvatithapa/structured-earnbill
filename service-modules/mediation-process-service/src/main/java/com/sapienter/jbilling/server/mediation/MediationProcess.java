package com.sapienter.jbilling.server.mediation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by andres on 19/10/15.
 */
@ApiModel(value = "MediationProcess", description = "Status of a specific mediation process run.")
public class MediationProcess implements Serializable, Exportable {

    private UUID id;
    private Integer entityId;
    private Boolean global = false;
    private Date startDate;
    private Date endDate;
    private Integer recordsProcessed = 0;
    private Integer doneAndBillable = 0;
    private Integer errors = 0;
    private Integer duplicates = 0;
    private Integer configurationId;
    private Integer doneAndNotBillable = 0;
    private Integer orderAffectedCount= 0;
    private Integer aggregated = 0;
    private Integer[] orderIds = new Integer[0];
    private String fileName = "";

    public MediationProcess() {
    }

    public MediationProcess(UUID id, Integer entityId, Integer configurationId, Boolean global, Date startDate,
							Date endDate, Integer recordsProcessed, Integer doneAndBillable, Integer errors,
							Integer duplicates, Integer doneAndNotBillable, Integer orderAffectedCount, Integer aggregated,
							String fileName) {
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
        this.fileName = fileName;
    }

    @ApiModelProperty(value = "Unique identifier")
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Current company ID")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "Is this a global mediation process")
    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    @ApiModelProperty(value = "Start date/time of the job")
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @ApiModelProperty(value = "End date/time of the job")
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @ApiModelProperty(value = "Nr of records processed")
    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    @ApiModelProperty(value = "Nr of records which are billable")
    public Integer getDoneAndBillable() {
        return doneAndBillable;
    }

    public void setDoneAndBillable(Integer doneAndBillable) {
        this.doneAndBillable = doneAndBillable;
    }

    @ApiModelProperty(value = "Nr of records with errors")
    public Integer getErrors() {
        return errors;
    }

    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    @ApiModelProperty(value = "Nr of duplicate records")
    public Integer getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(Integer duplicates) {
        this.duplicates = duplicates;
    }

    public void setConfigurationId(Integer configurationId) {
        this.configurationId = configurationId;
    }

    @ApiModelProperty(value = "Mediation Configuration ID")
    public Integer getConfigurationId() {
        return configurationId;
    }

    @ApiModelProperty(value = "Nr of records which are not billable")
    public Integer getDoneAndNotBillable() {
        return doneAndNotBillable;
    }

    public void setDoneAndNotBillable(Integer doneAndNotBillable) {
        this.doneAndNotBillable = doneAndNotBillable;
    }

    @ApiModelProperty(value = "Nr of orders affected")
    public Integer getOrderAffectedCount() {
		return orderAffectedCount;
	}

	public void setOrderAffectedCount(Integer orderAffectedCount) {
		this.orderAffectedCount = orderAffectedCount;
	}

    @ApiModelProperty(value = "List of order ids affected")
    public Integer[] getOrderIds() {
        return orderIds;
    }

	public void setOrderIds(Integer[] orderIds) {
		this.orderIds = orderIds;
	}

    public Integer getAggregated() {
        return aggregated;
    }

    public void setAggregated(Integer aggregated) {
        this.aggregated = aggregated;
    }

    @ApiModelProperty(value = "File name of the CDR file processed")
	public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    @JsonIgnore
    public String[] getFieldNames() {
        return new String[]{
            "id",
            "entityId",
            "configurationId",
            "global",
            "startDate",
            "endDate",
            "recordsProcessed",
            "doneAndBillable",
            "errors",
            "duplicates",
            "doneAndNotBillable",
            "orderAffectedCount",
            "aggregated",
            "fileName"
        };
    }

    @Override
    @JsonIgnore
    public Object[][] getFieldValues() {
        return new Object[][]{
            {
                id,
                entityId,
                configurationId,
                global,
                startDate,
                endDate,
                recordsProcessed,
                doneAndBillable,
                errors,
                duplicates,
                doneAndNotBillable,
                orderAffectedCount,
                aggregated,
                fileName
            }
        };
    }

    @Override
    public String toString() {
        return "MediationProcess [id=" + id + ", entityId=" + entityId
                + ", startDate=" + startDate + ", endDate=" + endDate
                + ", recordsProcessed=" + recordsProcessed
                + ", doneAndBillable=" + doneAndBillable + ", errors=" + errors
                + ", duplicates=" + duplicates + ", configurationId="
                + configurationId + ", doneAndNotBillable="
                + doneAndNotBillable + ", orderAffectedCount="
                + orderAffectedCount
                +", fileName=" + fileName + "]";
    }

}
