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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *  Incremental Unit
 *
 *  @author Panche Isajeski
 *  @see RatingUnitDTO
 *  @since 20-Aug-2013
 */
@Embeddable
public class IncrementUnit implements Serializable {

    private String name;
    private BigDecimal quantity;

    public IncrementUnit() {
    }

    @Column(name = "increment_unit_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "increment_unit_quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
