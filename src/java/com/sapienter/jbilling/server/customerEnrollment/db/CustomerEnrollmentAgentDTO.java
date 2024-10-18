/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Agent and possible rate linked to customer enrollment.
 */
@Entity
@TableGenerator(
        name="customer_enrollment_agent_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_enrollment_agent",
        allocationSize = 10
)
// No cache, mutable and critical
@Table(name="customer_enrollment_agent")
public class CustomerEnrollmentAgentDTO {
    private int id;
    private String brokerId;
    private PartnerDTO partner;
    private CustomerEnrollmentDTO enrollment;
    private BigDecimal rate;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_enrollment_agent_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_enrollment_id", nullable = false)
    public CustomerEnrollmentDTO getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(CustomerEnrollmentDTO enrollment) {
        this.enrollment = enrollment;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = true)
    public PartnerDTO getPartner() {
        return partner;
    }

    public void setPartner(PartnerDTO partner) {
        this.partner = partner;
    }

    @Column(name = "broker_id", nullable = false)
    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    @Column(name = "rate")
    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
