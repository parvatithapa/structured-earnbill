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

import com.sapienter.jbilling.server.user.db.CompanyDTO;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@TableGenerator(
        name="partner_commission_process_run_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission_process_run",
        allocationSize=10
)
@Table(name="partner_commission_process_run")
public class CommissionProcessRunDTO {
    private int id;
    private Date runDate;
    private Date periodStart;
    private Date periodEnd;
    private int errorCount = 0;
    private List<CommissionDTO> commissions;
    private CompanyDTO entity;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_process_run_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @Column(name="run_date", length=13)
    public Date getRunDate () {
        return runDate;
    }

    public void setRunDate (Date runDate) {
        this.runDate = runDate;
    }

    @Column(name="period_start", length=13)
    public Date getPeriodStart () {
        return periodStart;
    }

    public void setPeriodStart (Date periodStart) {
        this.periodStart = periodStart;
    }

    @Column(name="period_end", length=13)
    public Date getPeriodEnd () {
        return periodEnd;
    }

    public void setPeriodEnd (Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    @Column(name="error_count")
    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "commissionProcessRun")
    public List<CommissionDTO> getCommissions () {
        return commissions;
    }

    public void setCommissions (List<CommissionDTO> commissions) {
        this.commissions = commissions;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity () {
        return entity;
    }

    public void setEntity (CompanyDTO entity) {
        this.entity = entity;
    }
}
