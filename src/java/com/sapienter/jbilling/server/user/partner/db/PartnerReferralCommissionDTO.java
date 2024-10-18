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
package com.sapienter.jbilling.server.user.partner.db;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@TableGenerator(
        name="partner_referral_commission_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_referral_commission",
        allocationSize=10
)
@Table(name="partner_referral_commission")
public class PartnerReferralCommissionDTO {
    private int id;
    private PartnerDTO referral;
    private PartnerDTO referrer;
    private Date startDate;
    private Date endDate;
    private BigDecimal percentage;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_referral_commission_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="referral_id", nullable=false)
    public PartnerDTO getReferral () {
        return referral;
    }

    public void setReferral (PartnerDTO partner) {
        this.referral = partner;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="referrer_id", nullable=false)
    public PartnerDTO getReferrer () {
        return referrer;
    }

    public void setReferrer (PartnerDTO partner) {
        this.referrer = partner;
    }

    @Column(name="start_date", length=13)
    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    @Column(name="end_date", length=13)
    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    @Column(name="percentage", nullable=false, precision=17, scale=17)
    public BigDecimal getPercentage () {
        return percentage;
    }

    public void setPercentage (BigDecimal percentage) {
        this.percentage = percentage;
    }

}
