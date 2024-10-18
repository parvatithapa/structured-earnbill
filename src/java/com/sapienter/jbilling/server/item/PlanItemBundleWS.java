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

package com.sapienter.jbilling.server.item;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.validator.NonNegative;
import com.sapienter.jbilling.server.util.Constants;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * PlanItemBundleWS
 *
 * @author Brian Cowdery
 * @since 25/03/11
 */
@ApiModel(value = "Plan Item Bundle Data", description = "PlanItemBundleWS model")
public class PlanItemBundleWS implements Serializable {

    public static final String TARGET_SELF = "SELF";
    public static final String TARGET_BILLABLE = "BILLABLE";

    private Integer id;
    @NotNull(message = "validation.error.notnull")
    @NonNegative(message = "validation.error.nonnegative")
    private String quantity = "0";
    private Integer periodId = Constants.ORDER_PERIOD_ONCE;
    private String targetCustomer = TARGET_SELF;
    private boolean addIfExists = true;

    public PlanItemBundleWS() {
    }

    @ApiModelProperty(value = "The id of the bundle entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @JsonIgnore
    public String getQuantity() {
        return quantity;
    }

    @ApiModelProperty(value = "The quantity in the plan item bundle", required = true)
    @JsonProperty(value = "quantity")
    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    @JsonIgnore
    public void setQuantityAsDecimal(BigDecimal quantity) {
        setQuantity(quantity);
    }

    @JsonIgnore
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @JsonProperty(value = "quantity")
    public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toString() : null);
    }

    @ApiModelProperty(value = "The id of the order period used, default = one time")
    public Integer getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Integer periodId) {
        this.periodId = periodId;
    }

    @ApiModelProperty(value = "Determines the targeted customer", allowableValues = "SELF, BILLABLE")
    public String getTargetCustomer() {
        return targetCustomer;
    }

    public void setTargetCustomer(String targetCustomer) {
        this.targetCustomer = targetCustomer;
    }

    @ApiModelProperty(value = "Even if it exists, should it be added or not")
    @JsonProperty(value = "addIfExists")
    public boolean addIfExists() {
        return addIfExists;
    }

    public void setAddIfExists(boolean addIfExists) {
        this.addIfExists = addIfExists;
    }

    @Override
    public String toString() {
        return "PlanItemBundleWS{"
               + "id=" + id
               + ", quantity='" + quantity + '\''
               + ", periodId=" + periodId
               + ", targetCustomer='" + targetCustomer + '\''
               + ", addIfExists=" + addIfExists
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanItemBundleWS)) return false;

        PlanItemBundleWS that = (PlanItemBundleWS) o;

        return addIfExists == that.addIfExists &&
                nullSafeEquals(id, that.id) &&
                Util.decimalEquals(getQuantityAsDecimal(), that.getQuantityAsDecimal()) &&
                nullSafeEquals(periodId, that.periodId) &&
                nullSafeEquals(targetCustomer, that.targetCustomer);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getQuantityAsDecimal()));
        result = 31 * result + nullSafeHashCode(periodId);
        result = 31 * result + nullSafeHashCode(targetCustomer);
        result = 31 * result + (addIfExists ? 1 : 0);
        return result;
    }
}
