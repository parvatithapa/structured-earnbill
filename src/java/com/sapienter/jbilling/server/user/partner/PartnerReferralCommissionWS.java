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
 * This class defines the commission percentage that one partner (referral) will give to another partner (referrer)
 */
public class PartnerReferralCommissionWS implements WSSecured, Serializable {
    private int id;
    private Integer referralId;
    private Integer referrerId;
    private Date startDate;
    private Date endDate;
    private String percentage;
    private Integer userId;

    public PartnerReferralCommissionWS () {
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Integer getReferralId () {
        return referralId;
    }

    public void setReferralId (Integer referralId) {
        this.referralId = referralId;
    }

    public Integer getReferrerId () {
        return referrerId;
    }

    public void setReferrerId (Integer referrerId) {
        this.referrerId = referrerId;
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
    	this.userId = userId;
    }

    @Override
    public String toString() {
        return "PartnerReferralCommissionWS{"
               + "id=" + id
               + ", referralId=" + referralId
               + ", referrerId=" + referrerId
               + ", startDate=" + startDate
               + ", endDate=" + endDate
               + ", percentage=" + percentage
               + '}';
    }
}
