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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.lang.String;
import java.math.BigDecimal;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Customer Commission Definition", description = "CustomerCommissionDefinitionWS Model")
public class CustomerCommissionDefinitionWS implements Serializable {
    private int partnerId;
    private int customerId;
    @Digits(integer=12, fraction=10, message="validation.error.not.a.number")
    private String rate;

    @ApiModelProperty(value = "Partner id")
    public int getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(int partnerId) {
        this.partnerId = partnerId;
    }

    @ApiModelProperty(value = "Customer id")
    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    @JsonIgnore
    public String getRate() {
        return rate;
    }

    @JsonIgnore
    public void setRate(String rate) {
        this.rate = rate;
    }

    @JsonProperty("rate")
    public void setBigDecimalRate(BigDecimal rate) {
        this.rate = rate != null ? rate.toPlainString() : null;
    }

    @ApiModelProperty(value = "Decimal rate")
    @JsonProperty("rate")
    public BigDecimal getRateAsDecimal() {
        return Util.string2decimal(rate);
    }

    @Override
    public String toString() {
        return "CustomerCommissionDefinitionWS{" +
                "partnerId=" + partnerId +
                ", customerId=" + customerId +
                ", rate='" + rate + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerCommissionDefinitionWS)) return false;

        CustomerCommissionDefinitionWS that = (CustomerCommissionDefinitionWS) o;

        return partnerId == that.partnerId &&
                customerId == that.customerId &&
                nullSafeEquals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        int result = partnerId;
        result = 31 * result + customerId;
        result = 31 * result + nullSafeHashCode(rate);
        return result;
    }
}
