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


import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * This class overrides the item's commission percentage for a particular partner
 * for a given period of time.
 */
public class PartnerCommissionExceptionWS implements WSSecured, Serializable {
    private int id;
    private Integer partnerId;
    private Date startDate;
    private Date endDate;
    private String percentage;
    private Integer itemId;
    private Integer userId;

    public PartnerCommissionExceptionWS() {
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Integer getPartnerId () {
        return partnerId;
    }

    public void setPartnerId (Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    public String getPercentage() {
        return percentage;
    }

    public BigDecimal getPercentageAsDecimal() {
        return Util.string2decimal(percentage);
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = (percentage != null ? percentage.toString() : null);
    }

    public Integer getItemId () {
        return itemId;
    }

    public void setItemId (Integer itemId) {
        this.itemId = itemId;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     *
     * @return null
     */
    public Integer getOwningEntityId () {
        return null;
    }

    public Integer getOwningUserId () {
       return userId;
    }

    public void setOwningUserId(Integer userId){
    	this.userId=userId;
    }
    
    @Override
    public String toString() {
        return "PartnerCommissionException{"
               + "id=" + id
               + ", partnerId=" + partnerId
               + ", startDate=" + startDate
               + ", endDate=" + endDate
               + ", percentage=" + percentage
               + ", itemId=" + itemId
               + '}';
    }

}
