/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;

/**
 * A new pricing model which work on route rate card(same as NYMEXPlusMonthlyPricingStrategy pricing model).
 * But rate will be calculated as percentage instead of per unit. Blending the rates
 * <p>
 * ----------------------
 * This pricing model will calculate average percentage for different spans divided by Break Point.
 * <p>
 * This calculation based on existing line.
 * If linePrice = 2.0581904
 * then lineAmount = (linePrice*Total usage) = (2.0581904*1848) =3803.5358592
 * <p>
 * For example, From : 20 May, To : 10 June. Total usage : 1848.
 * <p>
 * No. of days = 21
 * usage per day = (Total usage/No. of days) = (1848/21)=88
 * <p>
 * And route rate card has rows :
 * 1 May - 1 June : 5%
 * 1 June - 1 July : 6% : End date(right date is exclusive)
 * Here 1 June is break point.
 * <p>
 * First Part of usage = usage for 20 May to 1 June = (usage per day*days from 20 May to 1 June) = 88*11 = 968
 * Second Part of usage = usage for 1 June to 10 June = (usage per day*days from 1 June to 10 June) = 88*10 = 880
 * <p>
 * Percentage will be calculated as :
 * =>[{((First Part of usage*linePrice*(1 May - 1 June))/100) + ((Second Part of usage*linePrice*(1 June - 1 July))/100)}*100]/lineAmount
 * =>[{((968*2.0581904*5)/100) + ((880*2.0581904*6)/100)}*100]/3803.5358592
 * =>5.47619
 * <p>
 * ----------------------
 *
 * @author Hitesh Yadav
 * @version 4.1
 * @since 2016-08-24
 */

public class RRCPercentagePricingStrategy extends NYMEXPlusMonthlyPricingStrategy {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RRCPercentagePricingStrategy.class));

    private BigDecimal linePrice;
    private BigDecimal lineAmount;
    private static final BigDecimal BIG_DECIMAL_HUNDRED = new BigDecimal(100);

    public RRCPercentagePricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_ROUTE_RATE_CARD_ID, INTEGER, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
    }

    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {
        if (pricingOrder == null || pricingOrder.getUser() == null) {
            LOG.debug("User not found.");
            if (result != null) {
                result.setPrice(BigDecimal.ZERO);
            }
            return;
        }
        initializeLineData(pricingOrder);
        super.applyTo(pricingOrder, result, fields, planPrice, quantity, usage, singlePurchase, orderLineDTO);
    }


    @Override
    public BigDecimal calculateCharge(BigDecimal price, BigDecimal rate, BigDecimal adderFee, BigDecimal usageUpToBreakPoint) {
        rate = usageUpToBreakPoint.multiply(linePrice).multiply(rate).divide(BIG_DECIMAL_HUNDRED, MathContext.DECIMAL64);
        return price.add(rate);
    }

    @Override
    public void calculateUnitPrice(PricingResult result, BigDecimal quantity, BigDecimal fupQty, BigDecimal price) {
        calculatePercentage(result, price);
    }

    /**
     * This method will calculate the percentage based on percentagePrice.
     *
     * @param result          This is use for set the final price as percentage.
     * @param percentagePrice This is used for calculate the percentage.
     * @return Nothing.
     */
    private void calculatePercentage(PricingResult result, BigDecimal percentagePrice) {
        BigDecimal percentage = percentagePrice.multiply(BIG_DECIMAL_HUNDRED).divide(lineAmount, MathContext.DECIMAL64);
        result.setPrice(percentage);
        result.setIsPercentage(true);
    }

    /**
     * This method used for initialize the line amount and price.
     *
     * @param pricingOrder This is use for get the order line.
     * @return Nothing.
     */
    public void initializeLineData(OrderDTO pricingOrder) {
        LOG.debug("initialize line data");
        OrderLineDTO orderLineDTO = getLineByUserCommodity(pricingOrder);
        if (orderLineDTO == null) {
            LOG.error("Configuration Error: Order line not found.");
            throw new SessionInternalError("Configuration Error: Order line not found.");
        }
        this.linePrice = orderLineDTO.getPrice();
        if (linePrice == null) {
            LOG.error("Configuration Error: " + orderLineDTO.getDescription() + " line price not found. RRCPercentage worked on commodity product line for applying the charges on line price.");
            throw new SessionInternalError("Configuration Error: " + orderLineDTO.getDescription() + " line price not found. RRCPercentage worked on commodity product line for applying the charges on line price.");
        }
        BigDecimal quantity = orderLineDTO.getQuantity();
        if (quantity == null) {
            LOG.debug("Configuration Error: " + orderLineDTO.getDescription() + " line quantity not found.");
            throw new SessionInternalError("Configuration Error: " + orderLineDTO.getDescription() + " line quantity not found.");
        }
        this.lineAmount = quantity.multiply(linePrice);

    }

    /**
     * This method used for find the order line by commodity AIT meta field at user level.
     *
     * @param pricingOrder This is use for find the order line.
     * @return OrderLineDTO.
     */
    private OrderLineDTO getLineByUserCommodity(OrderDTO pricingOrder) {
        CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = pricingOrder.getUser().getCustomer().getCustomerAccountInfoTypeMetaField(FileConstants.COMMODITY);
        if (customerAccountInfoTypeMetaField == null || customerAccountInfoTypeMetaField.getMetaFieldValue() == null || customerAccountInfoTypeMetaField.getMetaFieldValue().getValue() == null) {
            LOG.error("commodity not found for user.");
            throw new SessionInternalError("commodity not found for user.");
        }
        for (OrderLineDTO orderLineDTO : pricingOrder.getLines()) {
            if (orderLineDTO.getDescription().equals(customerAccountInfoTypeMetaField.getMetaFieldValue().getValue().toString())) {
                return orderLineDTO;
            }
        }
        return null;
    }
}
