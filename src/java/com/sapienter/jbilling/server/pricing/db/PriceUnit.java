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

/**
 *  Price Unit
 *
 *  @author Panche Isajeski
 *  @see RatingUnitDTO
 *  @since 20-Aug-2013
 */
@Embeddable
public class PriceUnit implements Serializable {

    private String name;

    public PriceUnit() {
    }

    @Column(name = "price_unit_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
