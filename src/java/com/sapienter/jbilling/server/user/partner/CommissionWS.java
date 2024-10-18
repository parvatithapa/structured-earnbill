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
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;

@ApiModel(value = "CommissionWS", description = "Commission model")
public class CommissionWS implements WSSecured, Serializable {
    private int id;
    
    private String amount;
    private String type;
    private Integer partnerId;
    private Integer commissionProcessRunId;
    private Integer currencyId;
    private Integer owningEntityId;

    public CommissionWS () {
    }

    @ApiModelProperty(value = "Unique identifier of the commission")
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Amount of received/calculated commission for partner represented by partnerId")
    public String getAmount () {
        return amount;
    }

    @JsonIgnore
    public BigDecimal getAmountAsDecimal() {
        return Util.string2decimal(amount);
    }

    public void setAmount (String amount) {
        this.amount = amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = (amount != null ? amount.toString() : null);
    }

    @ApiModelProperty(value = "Commission type. Instance of CommissionType. Possible values {DEFAULT_STANDARD_COMMISSION, DEFAULT_MASTER_COMMISSION, EXCEPTION_COMMISSION, REFERRAL_COMMISSION, CUSTOMER_COMMISSION}.")
    public String getType () {
        return type;
    }

    public void setType (String type) {
        this.type = type;
    }

    @ApiModelProperty(value = "ID of the partner for which this commission is defined.")
    public Integer getPartnerId () {
        return partnerId;
    }

    public void setPartnerId (Integer partnerId) {
        this.partnerId = partnerId;

    }

    @ApiModelProperty(value = "Unique identifier of the commission")
    public Integer getCommissionProcessRunId () {
        return commissionProcessRunId;
    }

    public void setCommissionProcessRunId (Integer commissionProcessRunId) {
        this.commissionProcessRunId = commissionProcessRunId;
    }

    @Override
    public String toString () {
        return "CommissionWS{"
                + "id=" + id
                + ", amount=" + amount
                + ", type=" + type
                + ", partnerId=" + partnerId
                + ", commissionProcessRunId=" + commissionProcessRunId
                + ", currencyId=" + currencyId
                + '}';

    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId () {
       return owningEntityId;
    }
    
    public void setOwningEntityId(Integer owningEntityId){
    	this.owningEntityId = owningEntityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId () {
        return null;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    
}
