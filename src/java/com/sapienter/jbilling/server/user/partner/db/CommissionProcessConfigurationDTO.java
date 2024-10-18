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

import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import javax.persistence.*;
import java.util.Date;

@Entity
@TableGenerator(
        name="partner_commission_proc_config_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission_proc_config",
        allocationSize=10
)
@Table(name="partner_commission_proc_config")
public class CommissionProcessConfigurationDTO {
    private int id;
    private CompanyDTO entity;
    private Date nextRunDate;
    private PeriodUnitDTO periodUnit;
    private int periodValue;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_proc_config_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name="next_run_date", length=13)
    public Date getNextRunDate () {
        return nextRunDate;
    }

    public void setNextRunDate (Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_unit_id")
    public PeriodUnitDTO getPeriodUnit () {
        return periodUnit;
    }

    public void setPeriodUnit (PeriodUnitDTO periodUnit) {
        this.periodUnit = periodUnit;
    }

    @Column(name = "period_value")
    public int getPeriodValue () {
        return periodValue;
    }

    public void setPeriodValue (int periodValue) {
        this.periodValue = periodValue;
    }

    public static class Builder {
        private int id;
        private CompanyDTO company;
        private Date nextRunDate;
        private PeriodUnitDTO periodUnit;
        private int periodValue;

        public CommissionProcessConfigurationDTO build() {
            CommissionProcessConfigurationDTO dto = new CommissionProcessConfigurationDTO();
            dto.setId(id);
            dto.setEntity(company);
            dto.setNextRunDate(nextRunDate);
            dto.setPeriodUnit(periodUnit);
            dto.setPeriodValue(periodValue);
            return dto;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder company(CompanyDTO company) {
            this.company = company;
            return this;
        }

        public Builder nextRunDate(Date nextRunDate) {
            this.nextRunDate = nextRunDate;
            return this;
        }

        public Builder periodUnit(PeriodUnitDTO periodUnit) {
            this.periodUnit = periodUnit;
            return this;
        }

        public Builder periodValue(int periodValue) {
            this.periodValue = periodValue;
            return this;
        }
    }
}
