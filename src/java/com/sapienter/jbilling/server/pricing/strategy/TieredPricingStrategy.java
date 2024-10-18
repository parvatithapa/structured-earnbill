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
import com.sapienter.jbilling.common.SessionInternalError;
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
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;

/**
 * Tiered pricing strategy.
 *
 * @author Brian Cowdery
 * @since 16-Jan-2012
 */
public class TieredPricingStrategy extends AbstractTieredPricingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TieredPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition("0", DECIMAL, true)
        );

        setChainPositions(
                ChainPosition.START
        );
        setUsesDynamicAttributes(true);
        setRequiresUsage(true);
        setVariableUsagePricing(true);
    }

    /**
     * Calculates a price based on the amount being purchased. Prices are organized into "tiers", where
     * the price calculated depends on how much of the quantity purchased falls into each tier.
     * <p>
     * Example:
     * 0 - 500    @ $2
     * 500 - 1000 @ $1
     * > 1000     @ $0.5
     * <p>
     * The first 500 purchased would be at $2/unit, the next 500 would be priced at $1/unit, and
     * the remaining quantity over 1000 would be priced at $0.5/unit.
     * <p>
     * The final price is an aggregated total of the quantity priced in each tier.
     *  @param pricingOrder target order for this pricing request (may be null)
     * @param result       pricing result to apply pricing to
     * @param fields       pricing fields
     * @param planPrice    the plan price to apply
     * @param quantity     quantity of item being priced
     * @param usage        total item usage for this billing period
     * @param orderLineDTO
     */
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
                        PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        if (usage == null || usage.getQuantity() == null) {
            throw new IllegalArgumentException("Usage quantity cannot be null for TieredPricingStrategy.");
        }

        /*
           Usage quantity normally includes the quantity being purchased because we roll in the order
           lines. If there is no pricing order (populating a single ItemDTO price), add the quantity
           being purchased to the usage calc to get the total quantity.
        */
        BigDecimal total = getTotalQuantity(pricingOrder, usage, quantity, singlePurchase);
        BigDecimal existing = getExistingQuantity(pricingOrder, usage, quantity, singlePurchase);

        BigDecimal freeUsageQuantityOfItem = BigDecimal.ZERO;
        if (null != pricingOrder) {
            for (OrderLineDTO orderLine : pricingOrder.getLines()) {
                if (orderLine.getItemId().intValue() == result.getItemId().intValue()) {
                    freeUsageQuantityOfItem = freeUsageQuantityOfItem.add(orderLine.getFreeUsagePoolQuantity());
                }
            }
        }

        assert existing.add(quantity).equals(total);

        // parse pricing tiers
        SortedMap<BigDecimal, BigDecimal> tiers = getTiers(planPrice.getAttributes());
        logger.debug("Tiered pricing: {}", tiers);
        logger.debug("Calculating tiered price for purchase quantity {}, and {} existing.", quantity, existing);

        if (!tiers.isEmpty()) {
            // calculate price for entire quantity across all orders, and the price for all previously
            // existing orders. The difference is the price of the quantity being purchased now.
            BigDecimal existingPrice = BigDecimal.ZERO;
            BigDecimal totalPrice = getTotalForQuantity(tiers, total.subtract(freeUsageQuantityOfItem));

            if (existing.compareTo(BigDecimal.ZERO) > 0) {
                existingPrice = getTotalForQuantity(tiers, existing.subtract(freeUsageQuantityOfItem));
            }
            // calculate price per unit from the total
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal price = totalPrice.subtract(existingPrice).divide(quantity, Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                result.setPrice(price);
            } else {
                result.setPrice(BigDecimal.ZERO);
            }

        } else {
            // no pricing tiers given
            result.setPrice(BigDecimal.ZERO);
        }

        if(orderLineDTO!=null && PreferenceBL.isTierCreationAllowed(pricingOrder.getUser().getCompany().getId()))
            populateOrderLineTiers(orderLineDTO, tiers, usage.getQuantity());
    }

    /**
     * Calculates the total dollar value (quantity * price) of the given quantity for the
     * given pricing tiers.
     *
     * @param tiers    pricing tiers
     * @param quantity quantity to calculate total for
     * @return total dollar value of purchased quantity
     */
    public BigDecimal getTotalForQuantity(SortedMap<BigDecimal, BigDecimal> tiers, BigDecimal quantity) {
        // sort through each tier, adding up the price for the full quantity purchased.
        BigDecimal totalPrice = BigDecimal.ZERO;

        BigDecimal lower = null;
        for (BigDecimal upper : tiers.keySet()) {
            if (lower == null) {
                lower = upper;
                continue;
            }

            // the total quantity in this tier that gets a price
            BigDecimal tier = upper.subtract(lower);
            BigDecimal price = tiers.get(lower);

            // quantity less than total number of units in tier
            // totalPrice = totalPrice + (quantity * price)
            // break from loop
            if (quantity.compareTo(tier) < 0) {
                totalPrice = totalPrice.add(quantity.multiply(price));
                quantity = BigDecimal.ZERO;
                break;
            }

            // quantity is more than, or equal to, total number of units
            // subtract tier quantity from quantity being priced
            // totalPrice = totalPrice + (tier quantity * price)
            if (quantity.compareTo(tier) >= 0) {
                totalPrice = totalPrice.add(tier.multiply(price));
                quantity = quantity.subtract(tier);
            }

            // move up to the next tier and handle the
            // remaining quantity in the next pass.
            lower = upper;
        }


        // last tier
        // all remaining quantity > tier priced at a fixed rate
        BigDecimal price = tiers.get(lower);
        if (!quantity.equals(BigDecimal.ZERO)) {
            totalPrice = totalPrice.add(quantity.multiply(price));
        }

        return totalPrice;
    }

    /**
     * Parses the price model attributes and returns a map of tier quantities and corresponding
     * prices for each tier. The map is sorted in ascending order by quantity (smallest first).
     *
     * @param attributes attributes to parse
     * @return tiers of quantities and prices
     */
    protected SortedMap<BigDecimal, BigDecimal> getTiers(Map<String, String> attributes) {
        return attributes.entrySet()
                .stream()
                .filter(map -> NumberUtils.isNumber(map.getKey()) && NumberUtils.isNumber(map.getValue()))
                .collect(Collectors.toMap(
                        e -> AttributeUtils.parseDecimal(e.getKey()),
                        e -> AttributeUtils.parseDecimal(e.getValue()),
                        (v1, v2) -> {
                            throw new IllegalStateException();
                        },
                        TreeMap::new
                ));
    }

    @Override
    public void validate(PriceModelDTO priceModel) {

        SortedMap<BigDecimal, BigDecimal> tiers = getTiers(priceModel.getAttributes());

        //Check that all the attributes added dynamically are valid numbers
        if (!tiers.isEmpty() && (tiers.size() != priceModel.getAttributes().size())) {
            throw new SessionInternalError("All tiered pricing attributes should be numbers",
                    new String[]{"bean.TieredPricingStrategy.strategy.validation.error.all.attr.should.be.numbers"});
        }

        boolean containNegativeNumbers = tiers.entrySet()
                .stream()
                .anyMatch(entry -> entry.getKey().signum() < 0);

        if (containNegativeNumbers) {
            throw new SessionInternalError("All 'from' pricing tiered attributes should be positive integer numbers",
                    new String[]{"bean.TieredPricingStrategy.strategy.validation.error.from.attr"});
        }

        boolean containNonIntegers = tiers.entrySet()
                .stream()
                .anyMatch(entry -> entry.getKey().scale() > 0 ||
                        entry.getKey().stripTrailingZeros().scale() > 0);

        if (containNonIntegers) {
            throw new SessionInternalError("All 'from' pricing tiered attributes should be positive integer numbers",
                    new String[]{"bean.TieredPricingStrategy.strategy.validation.error.from.attr"});
        }
    }
}
