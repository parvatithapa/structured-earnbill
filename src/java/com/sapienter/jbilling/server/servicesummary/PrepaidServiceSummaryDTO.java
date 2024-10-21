/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.servicesummary;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@TableGenerator(
        name = "prepaid_service_summary_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "prepaid_service_summary",
        allocationSize = 1)
@Table(name = "prepaid_service_summary")
public class PrepaidServiceSummaryDTO {
    private int id;
    private Integer serviceSummaryId;
    private BigDecimal dataQuantity;
    private BigDecimal voiceQuantity;
    private BigDecimal smsQuantity;
    private Date startDate;
    private Date endDate;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "prepaid_service_summary_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "service_summary_id", nullable = false)
    public Integer getServiceSummaryId() {
        return serviceSummaryId;
    }

    public void setServiceSummaryId(Integer serviceSummaryId) {
        this.serviceSummaryId = serviceSummaryId;
    }

    @Column(name = "data_quantity", nullable = true)
    public BigDecimal getDataQuantity() {
        return dataQuantity;
    }

    public void setDataQuantity(BigDecimal dataQuantity) {
        this.dataQuantity = dataQuantity;
    }

    @Column(name = "voice_quantity", nullable = true)
    public BigDecimal getVoiceQuantity() {
        return voiceQuantity;
    }

    public void setVoiceQuantity(BigDecimal voiceQuantity) {
        this.voiceQuantity = voiceQuantity;
    }

    @Column(name = "sms_quantity", nullable = true)
    public BigDecimal getSmsQuantity() {
        return smsQuantity;
    }

    public void setSmsQuantity(BigDecimal smsQuantity) {
        this.smsQuantity = smsQuantity;
    }

    @Column(name = "start_date", nullable = true)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "end_date", nullable = true)
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "PrepaidServiceSummaryDTO{" +
                "id=" + id +
                ", serviceSummaryId=" + serviceSummaryId +
                ", dataQuantity=" + dataQuantity +
                ", voiceQuantity=" + voiceQuantity +
                ", smsQuantity=" + smsQuantity +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
