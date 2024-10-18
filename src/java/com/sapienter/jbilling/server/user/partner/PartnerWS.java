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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

import org.apache.commons.lang.StringUtils;

/**
 * PartnerWS
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
public class PartnerWS implements WSSecured, Serializable {

    @Min(value = 1, message = "validation.error.min,1", groups = UpdateValidationGroup.class)
    @Max(value = 0, message = "validation.error.max,0", groups = CreateValidationGroup.class)
    private Integer id;
    private Integer userId;
    private String totalPayments;
    private String totalRefunds;
    private String totalPayouts;
    private String duePayout;
    private String brokerId;
    private int deleted;

    private List<PartnerPayoutWS> partnerPayouts = new ArrayList<PartnerPayoutWS>(0);
    private List<Integer> customerIds = new ArrayList<Integer>(0);
    private String type;
    private Integer parentId = null;
    private Integer[] childIds = null;
    private CommissionWS[] commissions = null;
    private PartnerCommissionExceptionWS[] commissionExceptions = null;
    private PartnerReferralCommissionWS[] referralCommissions = null;
    private PartnerReferralCommissionWS[] referrerCommissions = null;
    private PartnerCommissionValueWS[] commissionValues = null;
    private String commissionType;

    public PartnerWS() {
    }

    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public PartnerCommissionValueWS[] getCommissionValues() {
        if(commissionValues == null) {
            commissionValues = new PartnerCommissionValueWS[0];
        }
        return commissionValues;
    }

    public void setCommissionValues(PartnerCommissionValueWS[] commissionValues) {
        this.commissionValues = commissionValues;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTotalPayments() {
        return totalPayments;
    }

    public BigDecimal getTotalPaymentsAsBigDecimal() {
        return totalPayments != null ? new BigDecimal(totalPayments) : null;
    }

    public void setTotalPayments(String totalPayments) {
        this.totalPayments = totalPayments;
    }

    public void setTotalPayments(BigDecimal totalPayments) {
        this.totalPayments = (totalPayments != null ? totalPayments.toPlainString() : null);
    }

    public String getTotalRefunds() {
        return totalRefunds;
    }

    public BigDecimal getTotalRefundsAsDecimal() {
        return Util.string2decimal(totalRefunds);
    }

    public void setTotalRefunds(String totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    public void setTotalRefunds(BigDecimal totalRefunds) {
        this.totalRefunds = (totalRefunds != null ? totalRefunds.toPlainString() : null);
    }

    public String getTotalPayouts() {
        return totalPayouts;
    }

    public BigDecimal getTotalPayoutsAsDecimal() {
        return Util.string2decimal(totalPayouts);
    }

    public void setTotalPayouts(String totalPayouts) {
        this.totalPayouts = totalPayouts;
    }

    public void setTotalPayouts(BigDecimal totalPayouts) {
        this.totalPayouts = (totalPayouts != null ? totalPayouts.toPlainString() : null);
    }

    public String getDuePayout() {
        return duePayout;
    }

    public BigDecimal getDuePayoutAsDecimal() {
        return Util.string2decimal(duePayout);
    }

    public void setDuePayout(String duePayout) {
        this.duePayout = duePayout;
    }

    public void setDuePayout(BigDecimal duePayout) {
        this.duePayout = (duePayout != null ? duePayout.toPlainString() : null);
    }

    public List<PartnerPayoutWS> getPartnerPayouts() {
        return partnerPayouts;
    }

    public void setPartnerPayouts(List<PartnerPayoutWS> partnerPayouts) {
        this.partnerPayouts = partnerPayouts;
    }

    public List<Integer> getCustomerIds() {
        return customerIds;
    }

    public void setCustomerIds(List<Integer> customerIds) {
        this.customerIds = customerIds;
    }

    public String getType () {
        return type;
    }

    public void setType (String type) {
        this.type = type;
    }

    public Integer getParentId () {
        return parentId;
    }

    public Integer[] getChildIds () {
        return childIds;
    }

    public void setChildIds (Integer[] childIds) {

        this.childIds = childIds;
    }

    public void setParentId (Integer parentId) {
        this.parentId = parentId;
    }

    public CommissionWS[] getCommissions() {
        return commissions;
    }

    public PartnerCommissionExceptionWS[] getCommissionExceptions () {
        return commissionExceptions;
    }

    public PartnerReferralCommissionWS[] getReferralCommissions () {
        return referralCommissions;
    }

    public void setReferralCommissions (PartnerReferralCommissionWS[] referralCommissions) {
        this.referralCommissions = referralCommissions;
    }

    public PartnerReferralCommissionWS[] getReferrerCommissions () {
        return referrerCommissions;
    }

    public void setReferrerCommissions (PartnerReferralCommissionWS[] referrerCommissions) {
        this.referrerCommissions = referrerCommissions;
    }

    public void setCommissions(CommissionWS[] commissions) {
        this.commissions = commissions;
    }

    public void setCommissionExceptions (PartnerCommissionExceptionWS[] commissionExceptions) {
        this.commissionExceptions = commissionExceptions;
    }

    public String getCommissionType () {
        return commissionType;
    }

    public void setCommissionType (String commissionType) {
        this.commissionType = commissionType;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    public Integer getOwningEntityId() {
        return null;
    }

    public Integer getOwningUserId() {
        return getUserId();
    }

   
    
    
    @Override
    public String toString() {
        return "PartnerWS{"
               + "id=" + id
               + ", userId=" + userId
               + ", totalPayments=" + totalPayments
               + ", totalRefunds=" + totalRefunds
               + ", totalPayouts=" + totalPayouts
               + ", duePayout=" + duePayout
               + ", partnerPayouts=" + (partnerPayouts != null ? partnerPayouts.size() : null)
               + ", customerIds=" + (customerIds != null ? customerIds.size() : null)
               + ", type=" + type
               + ", parentId=" + parentId
               + ", childIds=" + Arrays.toString(childIds)
               + ", commissions=" + Arrays.toString(commissions)
               + ", commissionExceptions=" + Arrays.toString(commissionExceptions)
               + ", referralCommissions=" + Arrays.toString(referralCommissions)
               + ", referrerCommissions=" + Arrays.toString(referrerCommissions)
               + ", commissionType=" + commissionType
               + '}';
    }
}
