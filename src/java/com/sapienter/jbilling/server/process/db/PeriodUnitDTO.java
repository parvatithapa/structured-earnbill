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
package com.sapienter.jbilling.server.process.db;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

@Entity
@Table(name="period_unit")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class PeriodUnitDTO extends AbstractDescription implements java.io.Serializable {

    public static final int MONTH = 1;
    public static final int WEEK = 2;
    public static final int DAY = 3;
    public static final int YEAR = 4;
    public static final int SEMI_MONTHLY = 5;

    private int id;
    private Set<PartnerDTO> partners = new HashSet<PartnerDTO>(0);
    private Set<OrderPeriodDTO> orderPeriodDTOs = new HashSet<OrderPeriodDTO>(0);
    private Set<BillingProcessDTO> billingProcesses = new HashSet<BillingProcessDTO>(0);
    private Set<BillingProcessConfigurationDTO> billingProcessConfigurations = new HashSet<BillingProcessConfigurationDTO>(0);

    public PeriodUnitDTO() {
    }

    public PeriodUnitDTO(int id) {
        this.id = id;
    }

    public PeriodUnitDTO(int id, Set<OrderPeriodDTO> orderPeriodDTOs, Set<BillingProcessDTO> billingProcesses, Set<BillingProcessConfigurationDTO> billingProcessConfigurations) {
        this.id = id;
        this.orderPeriodDTOs = orderPeriodDTOs;
        this.billingProcesses = billingProcesses;
        this.billingProcessConfigurations = billingProcessConfigurations;
    }

    @Override
    @Transient
    protected String getTable() {
        return Constants.TABLE_PERIOD_UNIT;
    }

    @Override
    @Id
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "periodUnit")
    public Set<OrderPeriodDTO> getOrderPeriods() {
        return this.orderPeriodDTOs;
    }

    public void setOrderPeriods(Set<OrderPeriodDTO> orderPeriodDTOs) {
        this.orderPeriodDTOs = orderPeriodDTOs;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "periodUnit")
    public Set<BillingProcessDTO> getBillingProcesses() {
        return this.billingProcesses;
    }

    public void setBillingProcesses(Set<BillingProcessDTO> billingProcesses) {
        this.billingProcesses = billingProcesses;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "periodUnit")
    public Set<BillingProcessConfigurationDTO> getBillingProcessConfigurations() {
        return this.billingProcessConfigurations;
    }

    public void setBillingProcessConfigurations(Set<BillingProcessConfigurationDTO> billingProcessConfigurations) {
        this.billingProcessConfigurations = billingProcessConfigurations;
    }

    @Override
    public String toString() {
        return "PeriodUnitDTO: " + id;
    }

    public String getAuditKey(Serializable id) {
        return id.toString();
    }

    @Transient
    public boolean isYearly() {
        return id == YEAR;
    }
}


