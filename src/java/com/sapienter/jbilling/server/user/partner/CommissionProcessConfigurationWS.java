/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.user.partner;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;

@ApiModel(value = "CommissionProcessConfigurationWS", description = "Commission Process Configuration model")
public class CommissionProcessConfigurationWS implements WSSecured, Serializable {
    private int id;
    private Integer entityId;
    @NotNull(message="validation.error.notnull")
    private Date nextRunDate;
    private Integer periodUnitId;
    @NotNull(message="validation.error.notnull")
    private Integer periodValue;

    public CommissionProcessConfigurationWS () {
    }

    @ApiModelProperty(value = "Unique ID of the configuration")
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Company ID")
    public Integer getEntityId () {
        return entityId;
    }

    public void setEntityId (Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "Next run date")
    public Date getNextRunDate () {
        return nextRunDate;
    }

    public void setNextRunDate (Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    @ApiModelProperty(value = "ID of the period unit used in this order period.")
    public Integer getPeriodUnitId () {
        return periodUnitId;
    }

    public void setPeriodUnitId (Integer periodUnitId) {
        this.periodUnitId = periodUnitId;
    }

    @ApiModelProperty(value = "Number of period units used in this order period.")
    public Integer getPeriodValue () {
        return periodValue;
    }

    public void setPeriodValue (Integer periodValue) {
        this.periodValue = periodValue;
    }

    

    @Override
    public String toString () {
        return "CommissionWS{"
                + "id=" + id
                + ", entityId=" + entityId
                + ", nextRunDate=" + nextRunDate
                + ", periodUnitId=" + periodUnitId
                + '}';

    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId () {
        return entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId () {
        return null;
    }
}
