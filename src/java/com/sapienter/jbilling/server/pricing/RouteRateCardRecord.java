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

import static com.sapienter.jbilling.common.CommonConstants.BIGDECIMAL_ROUND;
import static com.sapienter.jbilling.common.CommonConstants.BIGDECIMAL_SCALE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;

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

    private static final Integer MINUTES_TO_SECONDS = 60;
    private int id;
    private String name;
    private BigDecimal eventSurcharge;
    private BigDecimal initialIncrement;
    private BigDecimal subsequentIncrement;
    private BigDecimal charge;
    private BigDecimal markup;
    private String useMarkup;
    private BigDecimal cappedCharge;
    private BigDecimal cappedDuration;
    private BigDecimal minimumCharge;
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

    public BigDecimal calculatePrice(BigDecimal quantity, PricingField callCharge) {
        return calculatePrice(quantity, callCharge, false);
    }

    public BigDecimal getMarkup() {
        return markup;
    }

    public void setMarkup(BigDecimal markup) {
        this.markup = markup;
    }

    public String getUseMarkup() {
        return useMarkup;
    }

    public void setUseMarkup(String useMarkup) {
        this.useMarkup = useMarkup;
    }

    public BigDecimal getCappedCharge() {
        return cappedCharge;
    }

    public void setCappedCharge(BigDecimal cappedCharge) {
        this.cappedCharge = cappedCharge;
    }

    public BigDecimal getCappedDuration() {
        return cappedDuration;
    }

    public void setCappedDuration(BigDecimal cappedDuration) {
        this.cappedDuration = cappedDuration;
    }

    public BigDecimal getMinimumCharge() {
        return minimumCharge;
    }

    public void setMinimumCharge(BigDecimal minimumCharge) {
        this.minimumCharge = minimumCharge;
    }

    public BigDecimal calculatePrice(BigDecimal quantity, PricingField callCharge, boolean isMediated) {
        BigDecimal chargeQuantity = quantity;
        if (!isMediated) {
            if (initialIncrement.compareTo(BigDecimal.ZERO) == 0) {
                chargeQuantity = quantity;
            } else if (quantity.compareTo(initialIncrement) > 0) {
                chargeQuantity = (initialIncrement).add(((quantity.subtract(initialIncrement)).divide(
                        subsequentIncrement, BIGDECIMAL_SCALE, BIGDECIMAL_ROUND)).setScale(0, RoundingMode.CEILING)
                        .multiply(subsequentIncrement));
            } else {
                chargeQuantity = initialIncrement;
            }
        }
        // Use_Markup is True, then Calculate Charge Using Markup Percentage
        if (shouldChargeMarkup(callCharge)) {
            BigDecimal cdrCharge = new BigDecimal(callCharge.getStrValue());
            BigDecimal markupPercentage = cdrCharge.multiply(validateAndReturn(markup)).divide(new BigDecimal(100), BIGDECIMAL_SCALE, BIGDECIMAL_ROUND);
            BigDecimal finalCharge = cdrCharge.add(markupPercentage);
            return finalCharge.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : finalCharge;
        }

        // charged Quantity * Charge Per price unit * adjustment rate coming from the rate card rating unit
        BigDecimal price = chargeQuantity.multiply(charge).multiply(
                routeRateCard.getRatingUnit().getUnitAdjustmentRate());

        if (eventSurcharge != null) {
            price = price.add(eventSurcharge).setScale(CommonConstants.BIGDECIMAL_SCALE, BigDecimal.ROUND_HALF_UP);
        }

        price = calculateCappedCharge(chargeQuantity, price);

        if (price.compareTo(validateAndReturn(minimumCharge)) < 0) {
            return minimumCharge;
        }

        return price;
    }

    private BigDecimal calculateCappedCharge(BigDecimal chargeQuantity, BigDecimal price) {
        if (validateAndReturn(cappedCharge).compareTo(BigDecimal.ZERO) > 0
                && validateAndReturn(cappedDuration).compareTo(BigDecimal.ZERO) > 0 && price.compareTo(cappedCharge) > 0) {
            BigDecimal cappedDurationInMinutes = cappedDuration.divide(new BigDecimal(MINUTES_TO_SECONDS), 0, BIGDECIMAL_ROUND);
            if (cappedDurationInMinutes.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal additionalQuantity = BigDecimal.ZERO;
                BigDecimal additionalCharge = BigDecimal.ZERO;
                if (chargeQuantity.compareTo(cappedDurationInMinutes) > 0) {
                    additionalQuantity = chargeQuantity.subtract(cappedDurationInMinutes);
                }
                if (additionalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    additionalCharge = additionalQuantity.multiply(charge).multiply(
                            routeRateCard.getRatingUnit().getUnitAdjustmentRate());
                }
                price = cappedCharge.add(additionalCharge);
            }
        }
        return price;
    }

    private boolean shouldChargeMarkup(PricingField callCharge) {
        return Boolean.valueOf(useMarkup)
                && null != callCharge
                && StringUtils.isNotBlank(callCharge.getStrValue())
                && NumberUtils.isCreatable(callCharge.getStrValue());
    }

    private BigDecimal validateAndReturn(BigDecimal value) {
        return (null != value) ? value : BigDecimal.ZERO;
    }

}
