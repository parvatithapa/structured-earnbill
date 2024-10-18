/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2013] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *  Rating Unit
 *  <p>
 *  Used in the route rate cards to provide
 *  measurement units for the calculation.
 *  It consist of pricing unit and increment unit.
 *  <p>
 *  The pricing unit specifies the unit in which the price is being given,
 *  while the increment unit specifies the unit of the quantity which is being rated
 *  <p>
 *  Example:
 *  Rating Unit - Name: Time
 *  Price Unit - Name: Minute
 *  Increment Unit - Name: Seconds; Quantity: 60; 1 min=60 sec
 *
 *  @author Panche Isajeski
 *  @since 20-Aug-2013
 */
@Entity
@Table(name = "rating_unit")
@TableGenerator(
        name = "rating_unit_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "rating_unit",
        allocationSize = 1
)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class RatingUnitDTO implements Serializable {

    private int id;
    private String name;
    private CompanyDTO company;
    private IncrementUnit incrementUnit;
    private PriceUnit priceUnit;
    private boolean canBeDeleted = true;
    private Integer versionNum;

    public RatingUnitDTO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "rating_unit_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return this.company;
    }

    public void setCompany(CompanyDTO entity) {
        this.company = entity;
    }

    @Embedded
    public IncrementUnit getIncrementUnit() {
        return incrementUnit;
    }

    public void setIncrementUnit(IncrementUnit incrementUnit) {
        this.incrementUnit = incrementUnit;
    }

    @Embedded
    public PriceUnit getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(PriceUnit priceUnit) {
        this.priceUnit = priceUnit;
    }

    @Column(name = "can_be_deleted", updatable = false)
    public boolean isCanBeDeleted() {
        return canBeDeleted;
    }

    public void setCanBeDeleted(boolean canBeDeleted) {
        this.canBeDeleted = canBeDeleted;
    }

    @Version
    @Column(name = "optlock")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Transient
    public BigDecimal getUnitAdjustmentRate() {
        // 1 PriceUnit = quantity * 1 Incremental unit
        return BigDecimal.ONE.divide(getIncrementUnit() != null ?
                getIncrementUnit().getQuantity() : BigDecimal.ONE,
                Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
    }
}
