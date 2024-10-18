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
package com.sapienter.jbilling.server.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Route Rate Card Record
 * <p>
 * Represent a record from the route rate card table
 *
 * @author Panche Isajeski
 * @since 21-Aug-2013
 */
@SuppressWarnings("serial")
public class RouteRateCardRecord implements MatchingRecord {

    private int id;
    private String name;
    private BigDecimal eventSurcharge;
    private BigDecimal initialIncrement;
    private BigDecimal subsequentIncrement;
    private BigDecimal charge;
    // route rate card record attributes
    private Map<String, String> attributes = new HashMap<>();

    // route rate card table Id (RouteRateCardDTO) that this record belongs to
    private RouteRateCardDTO routeRateCard;

    public RouteRateCardRecord() {
    }

    public RouteRateCardRecord(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getEventSurcharge() {
        return eventSurcharge;
    }

    public void setEventSurcharge(BigDecimal eventSurcharge) {
        this.eventSurcharge = eventSurcharge;
    }

    public BigDecimal getInitialIncrement() {
        return initialIncrement;
    }

    public void setInitialIncrement(BigDecimal initialIncrement) {
        this.initialIncrement = initialIncrement;
    }

    public BigDecimal getSubsequentIncrement() {
        return subsequentIncrement;
    }

    public void setSubsequentIncrement(BigDecimal subsequentIncrement) {
        this.subsequentIncrement = subsequentIncrement;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public RouteRateCardDTO getRouteRateCard() {
        return routeRateCard;
    }

    public void setRouteRateCard(RouteRateCardDTO routeRateCard) {
        this.routeRateCard = routeRateCard;
    }

    public BigDecimal calculatePrice(BigDecimal quantity) {
        return calculatePrice(quantity, false);
    }

    public BigDecimal calculatePrice(BigDecimal quantity, boolean isMediated) {
        BigDecimal chargeQuantity = quantity;
        if(!isMediated) {
            if (initialIncrement.compareTo(BigDecimal.ZERO) == 0) {
                chargeQuantity = quantity;
            } else if (quantity.compareTo(initialIncrement) > 0) {
                chargeQuantity = (initialIncrement).add(((quantity.subtract(initialIncrement)).divide(subsequentIncrement,
                        Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND))
                        .setScale(0, RoundingMode.CEILING).multiply(subsequentIncrement));
            } else {
                chargeQuantity = initialIncrement;
            }
        }
        // charged Quantity * Charge Per price unit * adjustment rate coming from the rate card rating unit
        BigDecimal price = chargeQuantity.multiply(charge).multiply(routeRateCard.getRatingUnit().getUnitAdjustmentRate());

        if (eventSurcharge != null) {
            price = price.add(eventSurcharge);
        }
        return price;
    }
}
