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

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.util.PreferenceBL;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;

/**
 * Capped, graduated pricing strategy.
 *
 * Only usage over the included quantity, and under the set maximum total $ amount will be billed.
 *
 * @author Brian Cowdery
 * @since 31/01/11
 */
public class CappedGraduatedPricingStrategy extends GraduatedPricingStrategy {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CappedGraduatedPricingStrategy.class));
	
    public CappedGraduatedPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition("included", DECIMAL, true),
                new AttributeDefinition("max", DECIMAL, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(true);
        setVariableUsagePricing(true);
    }

    /**
     * Graduated pricing strategy with a maximum total usage cap.
     *
     * @see GraduatedPricingStrategy
     *@param pricingOrder target order for this pricing request (not used by this strategy)
     * @param result pricing result to apply pricing to
     * @param fields pricing fields (not used by this strategy)
     * @param planPrice the plan price to apply
     * @param quantity quantity of item being priced
     * @param usage total item usage for this billing period
     * @param orderLineDTO
     */
    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
                        PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        if (usage == null || usage.getAmount() == null) {
            throw new IllegalArgumentException("Usage amount cannot be null for CappedGraduatedPricingStrategy.");
        }

        LOG.debug("Usage amount: " + usage.getAmount());
        BigDecimal freeUsageQuantityOfItem = BigDecimal.ZERO;
        if (null != pricingOrder) {
        	for (OrderLineDTO orderLine: pricingOrder.getLines()) {
            	if (orderLine.getItemId().intValue() == result.getItemId().intValue()) {
            		freeUsageQuantityOfItem = freeUsageQuantityOfItem.add(orderLine.getFreeUsagePoolQuantity());
            	}
            }
        }
        
        BigDecimal maximum = AttributeUtils.getDecimal(planPrice.getAttributes(), "max");
        BigDecimal currentUsageQuantity = usage.getCurrentQuantity();
        if (currentUsageQuantity.compareTo(BigDecimal.ZERO) > 0) {
        	currentUsageQuantity = currentUsageQuantity.subtract(freeUsageQuantityOfItem);
        }
        super.applyTo(pricingOrder, result, fields, planPrice, quantity, usage, singlePurchase);

        LOG.debug("Calculated result price: " + result.getPrice());

        // only bill up to the set maximum cap
        // calculate a unit price that brings the total cost back down to the maximum cap
        if (result.getPrice() != null) {

            BigDecimal pastUsageAmount = usage.getAmount().subtract(usage.getCurrentAmount());
            
            if (pastUsageAmount.compareTo(BigDecimal.ZERO) > 0) {
            	pastUsageAmount = pastUsageAmount.subtract(freeUsageQuantityOfItem);
            }
            
            if (pastUsageAmount.compareTo(BigDecimal.ZERO) < 0) {
                pastUsageAmount = BigDecimal.ZERO;
            }
            BigDecimal total = BigDecimal.ZERO;
            OrderLineDTO orderLine = null;
            if (null != pricingOrder) {
            	orderLine = pricingOrder.getLine(result.getItemId());
            }
            BigDecimal included = getIncludedQuantity(pricingOrder, planPrice, usage);
            if (null != orderLine && orderLine.getId() > 0) {
            	total = pastUsageAmount.add(usage.getCurrentQuantity().multiply(result.getPrice()));
            } else if(usage.getQuantity().compareTo(included) > 0 &&
                    usage.getQuantity().subtract(included).multiply(result.getPrice()).compareTo(maximum) < 0){
                total = currentUsageQuantity.multiply(result.getPrice());
            } else {
            	total = pastUsageAmount.add(currentUsageQuantity.multiply(result.getPrice()));
            }
            LOG.debug("Total Usage amount: " + total + " ... from which past usage amount: " + pastUsageAmount);
            if(0 != usage.getCurrentQuantity().compareTo(BigDecimal.ZERO)) {
                if (total.compareTo(maximum) >= 0) {
                    BigDecimal billable = maximum.subtract(pastUsageAmount);
                    BigDecimal price = billable.divide(usage.getCurrentQuantity(), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    result.setPrice(price);
                } else if (total.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal price = total.divide(usage.getCurrentQuantity(), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    result.setPrice(price);
                }
            }
            if(orderLineDTO!=null && PreferenceBL.isTierCreationAllowed(pricingOrder.getUser().getCompany().getId())){
                populateOrderLineTierForGraduatedCap(orderLineDTO, included, currentUsageQuantity, planPrice.getRate(),
                        maximum, usage, result.getPrice().multiply(currentUsageQuantity));
            }
        }
    }
}
