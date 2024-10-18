package com.sapienter.jbilling.server.pricing.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Faizan
 * Util class containing the utility functions for AbstractTieredPricingStrategy
 */
public class TierRangeUtils {

    /**
     * Create pair of inclusive from and to range for the tier from quantity and price map.
     * @param qtyPriceTiers
     * @param tierNumber
     * @return Returns a pair of inclusive from and to range for the tier.
     * Final tier has 'null' to to indicate open ended tier
     */
    public static Pair<BigDecimal,BigDecimal> getTierRange(SortedMap<BigDecimal, BigDecimal> qtyPriceTiers , Integer tierNumber) {
        BigDecimal from = null;
        BigDecimal to = null;

        // Boundary condition
        if(MapUtils.isEmpty(qtyPriceTiers)) {
            return Pair.of(from, to);
        }
        Object[] tiers = qtyPriceTiers.keySet().toArray();

        if(tierNumber >= 1 && tierNumber <= tiers.length) {
            from = (BigDecimal) tiers[tierNumber-1];
            if(tierNumber < tiers.length) {
                to = (BigDecimal) tiers[tierNumber];
            }
        }
        return Pair.of(from, to);
    }

    /**
     * Build quantity and price map for Graduated and Capped Graduated Pricing Strategy
     * @param included
     * @param rate
     * @return Returns a new instance of TreeMap of quantity and price for Graduated and Capped Graduated Pricing Strategy
     */
    public static SortedMap<BigDecimal, BigDecimal> buildGraduatedTiers(BigDecimal included, BigDecimal rate) {
        SortedMap<BigDecimal, BigDecimal> qtyTiers = new TreeMap<>();
        qtyTiers.put(BigDecimal.ZERO, BigDecimal.ZERO);
        qtyTiers.put(included, rate);
        return qtyTiers;
    }

    /**
     * Checks if the user has already capped for maximum price in Capped Graduated Pricing Strategy
     * @param rate
     * @param totalQuantity
     * @param currentQuantity
     * @param freeQuantity
     * @param cappedAmount
     * @return Returns a boolean
     */
    public static boolean isAlreadyCapped(BigDecimal rate, BigDecimal totalQuantity, BigDecimal currentQuantity,
                                          BigDecimal freeQuantity, BigDecimal cappedAmount){
        BigDecimal amount = rate.multiply((totalQuantity.subtract(currentQuantity).subtract(freeQuantity)));
        return (1 == amount.compareTo(cappedAmount));
    }

    /**
     * Builds a TreeMap with updated quantity and price for Tiered Pricing Strategy
     * @param totalQuantity
     * @param currentQuantity
     * @param qtyPriceTiers
     * @return Returns a TreeMap of quantity and price for Tiered Pricing Strategy
     */
    public static SortedMap<BigDecimal, BigDecimal> getTotalRemainingTiers(BigDecimal totalQuantity, BigDecimal currentQuantity,
                                                                           SortedMap<BigDecimal, BigDecimal> qtyPriceTiers){
        BigDecimal usedQuantity = totalQuantity.subtract(currentQuantity);
        if(0 < usedQuantity.compareTo(BigDecimal.ZERO)) {
            SortedMap<BigDecimal, BigDecimal> newQtyPriceTiers = new TreeMap<BigDecimal, BigDecimal>();
            BigDecimal previousQty = qtyPriceTiers.firstKey();
            BigDecimal previousAmount = qtyPriceTiers.get(previousQty);
            boolean areInitialTiersAdded = false;
            BigDecimal quantitySum = BigDecimal.ZERO;
            BigDecimal quantity = BigDecimal.ZERO;
            for (Map.Entry<BigDecimal, BigDecimal> qtyPriceTier : qtyPriceTiers.entrySet()) {
                //Adding remaining tiers if any
                if(currentQuantity.compareTo(usedQuantity) > 0 && currentQuantity.compareTo(quantitySum.add(usedQuantity)) <= 0){
                    break;
                }
                quantity = qtyPriceTier.getKey().compareTo(usedQuantity) > 0 ? qtyPriceTier.getKey().subtract(usedQuantity) :
                        quantity;
                if(areInitialTiersAdded){
                    newQtyPriceTiers.put(quantity, qtyPriceTier.getValue());
                    quantitySum = quantitySum.add(quantity);
                }
                //Adjusting tiers if customer already bought product
                if (qtyPriceTier.getKey().compareTo(usedQuantity) > 0 && !areInitialTiersAdded) {
                    newQtyPriceTiers.put(BigDecimal.ZERO, previousAmount);
                    newQtyPriceTiers.put(quantity, qtyPriceTier.getValue());
                    areInitialTiersAdded = true;
                    quantitySum = quantitySum.add(quantity);
                }
                previousQty = qtyPriceTier.getKey();
                previousAmount = qtyPriceTier.getValue();
            }

            //Adjusting tier if customer bought product quantity till last tier
            if(!areInitialTiersAdded){
                newQtyPriceTiers.put(BigDecimal.ZERO, previousAmount);
            }
            return newQtyPriceTiers;
        } else {
            //if customer is buying product for first time.
            return qtyPriceTiers;
        }
    }
}
