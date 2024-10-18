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

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.*;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

/**
 * VariableRatePricingStrategy
 *
 * @author Neeraj Bhatt
 * @since 10/03/2016
 */
public class VariableRatePricingStrategy extends RouteBasedRateCardPricingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PARAM_EVENT_DATE = "event_date";

    public VariableRatePricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_ROUTE_RATE_CARD_ID, INTEGER, true),
                new AttributeDefinition(PARAM_EVENT_DATE, STRING, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
        setUsesDynamicAttributes(true);
        setVariableUsagePricing(true);
    }


    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice,
                        BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        if (pricingOrder != null) {
            if (pricingOrder.getActiveUntil() == null) {
                logger.error("Order ({}) should have active until date", pricingOrder.getId());
                result.setPrice(BigDecimal.ZERO);
                return;
            } else {
                if (null == fields) {
                    fields = new ArrayList<>();
                }
                PricingField.add(fields, new PricingField(PARAM_EVENT_DATE, pricingOrder.getActiveUntil()));
            }
        }
        super.applyTo(pricingOrder, result, fields, planPrice, quantity, usage, singlePurchase, orderLineDTO);
    }

}
