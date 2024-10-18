/**
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTierDTO;
import com.sapienter.jbilling.server.pricing.util.TierRangeUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;


/**
 * @author Faizan
 * Abstract base class for tiered pricing strategy classes,
 * namely Tiered, Graduated and Capped Graduated.
 * Contains implementation of common functions.  
 */
public abstract class AbstractTieredPricingStrategy extends AbstractPricingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Method to be used for population of order line tiers for Graduated strategies.
     * @param orderLine
     * @param included
     * @param currentUsageQuantity
     * @param price
     */
    protected void populateOrderLineTiers(OrderLineDTO orderLine, BigDecimal included, BigDecimal currentUsageQuantity,
                                          BigDecimal price, BigDecimal totalQuantity) {
        List<OrderLineTierDTO> oltiers = new ArrayList<OrderLineTierDTO>();
        int tierNumber = 0;
        SortedMap<BigDecimal, BigDecimal> qtyPriceTiers = TierRangeUtils.buildGraduatedTiers(included, price);

        if(1 == totalQuantity.compareTo(currentUsageQuantity)) {
            tierNumber = getTierIndex(totalQuantity, currentUsageQuantity, qtyPriceTiers);
            BigDecimal oldQuantity = totalQuantity.subtract(currentUsageQuantity);
            if(0 <= oldQuantity.compareTo(included)){
                createTier(orderLine, ++tierNumber, currentUsageQuantity, price,
                        price.multiply(currentUsageQuantity), totalQuantity, oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
            } else {
                BigDecimal newIncludedQuantity = included.subtract(oldQuantity);
                boolean isLessThanIncluded = 0 <= newIncludedQuantity.compareTo(currentUsageQuantity);
                createTier(orderLine, ++tierNumber, isLessThanIncluded ? currentUsageQuantity : newIncludedQuantity,
                        BigDecimal.ZERO, BigDecimal.ZERO, totalQuantity, oltiers,
                        qtyPriceTiers, TierRangeUtils::getTierRange);
                if(!isLessThanIncluded) {
                    createTier(orderLine, ++tierNumber, currentUsageQuantity.subtract(included.subtract(oldQuantity)), price,
                            price.multiply(currentUsageQuantity.subtract(included.subtract(oldQuantity))), totalQuantity,
                            oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
                }
            }
        } else {
            createTier(orderLine, ++tierNumber, included.compareTo(currentUsageQuantity) > 0 ? currentUsageQuantity : included,
                    BigDecimal.ZERO, BigDecimal.ZERO, totalQuantity, oltiers,
                    qtyPriceTiers, TierRangeUtils::getTierRange);
            if(currentUsageQuantity.compareTo(included) > 0) {
                createTier(orderLine, ++tierNumber, currentUsageQuantity.subtract(included), price,
                        price.multiply(currentUsageQuantity.subtract(included)), totalQuantity, oltiers,
                        qtyPriceTiers, TierRangeUtils::getTierRange);
            }
        }
        setOrderLineTiersInOrderLine(orderLine, oltiers);
    }

    /**
     * Method to be used for population of order line tiers for Graduated cap strategies.
     * @param orderLine
     * @param included
     * @param currentUsageQuantity
     * @param price
     */
    protected void populateOrderLineTierForGraduatedCap(OrderLineDTO orderLine, BigDecimal included,
                                                        BigDecimal currentUsageQuantity, BigDecimal price,
                                                        BigDecimal maximum, Usage usage, BigDecimal totalAmount) {
        // no order line tiers exist, so create new ones
        List<OrderLineTierDTO> oltiers = new ArrayList<OrderLineTierDTO>();
        BigDecimal totalQuantity = usage.getQuantity();
        boolean isAlreadyCapped = TierRangeUtils.isAlreadyCapped(price, totalQuantity, currentUsageQuantity, included, maximum);
        boolean isPartialIncluded = (1 == totalQuantity.compareTo(currentUsageQuantity) &&
                1 == included.subtract(totalQuantity.subtract(currentUsageQuantity)).compareTo(BigDecimal.ZERO))
                ? true : false;
        boolean isFirstOrder = 0 == totalQuantity.compareTo(currentUsageQuantity);
        BigDecimal partiallyIncluded = included.subtract(totalQuantity.subtract(currentUsageQuantity));
        SortedMap<BigDecimal, BigDecimal> qtyPriceTiers = TierRangeUtils.buildGraduatedTiers(included, price);
        int tierNumber = getTierIndex(totalQuantity, currentUsageQuantity, qtyPriceTiers);
        // first ol tier is that of included quantity which is free.
        if ((totalQuantity.compareTo(included) > 0 || isAlreadyCapped) && !isPartialIncluded && !isFirstOrder) {
            createTier(orderLine, ++tierNumber, currentUsageQuantity, 
                    isAlreadyCapped ? BigDecimal.ZERO : price, totalAmount, totalQuantity, oltiers, 
                    qtyPriceTiers, TierRangeUtils::getTierRange);
        } else if(currentUsageQuantity.compareTo(included) <= 0  && !isPartialIncluded) {
            createTier(orderLine, ++tierNumber, currentUsageQuantity, BigDecimal.ZERO, totalAmount, totalQuantity, oltiers,
                    qtyPriceTiers, TierRangeUtils::getTierRange);
        }

        // second ol tier is that of remaining quantity after subtracting included quantity which is free.
        else
        {
            if(isPartialIncluded){
                if(totalQuantity.compareTo(included) > 0) {
                    createTier(orderLine, ++tierNumber, partiallyIncluded, BigDecimal.ZERO, BigDecimal.ZERO, totalQuantity,
                            oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
                    createTier(orderLine, ++tierNumber, currentUsageQuantity.subtract(partiallyIncluded), price,
                            totalAmount, totalQuantity, oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
                } else {
                    createTier(orderLine, ++tierNumber, currentUsageQuantity, BigDecimal.ZERO, totalAmount, totalQuantity, oltiers,
                            qtyPriceTiers, TierRangeUtils::getTierRange);
                }
            } else {
                tierNumber = 0;
                createTier(orderLine, ++tierNumber, included, BigDecimal.ZERO, BigDecimal.ZERO, totalQuantity, oltiers,
                        qtyPriceTiers, TierRangeUtils::getTierRange);

                if (currentUsageQuantity.subtract(included).multiply(price).compareTo(maximum) > 0) {
                    createTier(orderLine, ++tierNumber, currentUsageQuantity.subtract(included), price, totalAmount, totalQuantity,
                            oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
                } else {
                    createTier(orderLine, ++tierNumber, currentUsageQuantity.subtract(included), price,
                            totalAmount, totalQuantity, oltiers, qtyPriceTiers, TierRangeUtils::getTierRange);
                }
            }
        }
        setOrderLineTiersInOrderLine(orderLine, oltiers);
    }

    /**
     * Method used for population of order line tiers for Tiered Pricing Strategy
     * @param orderLine
     * @param qtyPriceTiers
     */
    @SuppressWarnings("null")
    protected void populateOrderLineTiers(OrderLineDTO orderLine, SortedMap<BigDecimal, BigDecimal> qtyPriceTiers, BigDecimal total) {
        List<OrderLineTierDTO> oltiers = new ArrayList<OrderLineTierDTO>();
        logger.debug("creating new order line Tier");
        oltiers = createNewTiers(orderLine, qtyPriceTiers, total);
        if(0 < oltiers.size())
            setOrderLineTiersInOrderLine(orderLine, oltiers);
    }

    /**
     * Create new tiers for tiered pricing strategy
     * @param orderLine
     * @param qtyPriceTiers
     * @param total
     * @return List of orderLineTiers
     */
    private List<OrderLineTierDTO> createNewTiers(OrderLineDTO orderLine, SortedMap<BigDecimal, BigDecimal> qtyPriceTiers,
                                                  BigDecimal total){

        logger.debug("Creating new order line tiers for Tiered Pricing strategy.");
        SortedMap<BigDecimal, BigDecimal> oldQtyPriceTiers = qtyPriceTiers;
        qtyPriceTiers = TierRangeUtils.getTotalRemainingTiers(total, orderLine.getQuantity(), qtyPriceTiers);
        List<OrderLineTierDTO> oltiers = new ArrayList<OrderLineTierDTO>(qtyPriceTiers.size());

        int index = getTierIndex(total, orderLine.getQuantity(), oldQtyPriceTiers);
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal totalQuantitySaved = BigDecimal.ZERO;

        qty = qtyPriceTiers.lastKey();
        price = qtyPriceTiers.get(qty);
        // There are no order line tiers present on the order line, so create new
        SortedMap<BigDecimal, BigDecimal> qtyPriceTierTemp = new TreeMap<BigDecimal, BigDecimal>();
        BigDecimal tempValue = null;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal tempQuantity = BigDecimal.ZERO;

        Integer count =0;

        for (Entry<BigDecimal, BigDecimal> qtyPriceTier : qtyPriceTiers.entrySet()){
            if(qtyPriceTier.getKey().compareTo(orderLine.getQuantity()) < 0) {
                if(qtyPriceTier.getKey().compareTo(BigDecimal.ZERO) == 0){
                    tempValue = qtyPriceTier.getValue();
                    continue;
                }
                if(tempValue!=null){
                    qtyPriceTierTemp.put(qtyPriceTier.getKey(), tempValue);
                    tempValue = qtyPriceTier.getValue();
                }
            }
            else{
                if(tempValue!=null){
                    qtyPriceTierTemp.put(qtyPriceTier.getKey(), tempValue);
                    tempValue = qtyPriceTier.getValue();
                }
                break;
            }

        }
        for (Entry<BigDecimal, BigDecimal> qtyPriceTier : qtyPriceTierTemp.entrySet()) {
            if(qtyPriceTier.getKey().compareTo(orderLine.getQuantity()) < 0 && count ==0){
                logger.debug("creating new orderLineTier 1 {}", index + 1);
                createTier(orderLine, index+1, qtyPriceTier.getKey(),
                        qtyPriceTier.getValue(), qtyPriceTier.getKey().multiply(qtyPriceTier.getValue()), total, oltiers,
                        oldQtyPriceTiers, TierRangeUtils::getTierRange);

                totalQuantitySaved= totalQuantitySaved.add(qtyPriceTier.getKey().subtract(totalQuantitySaved));
                index ++;

                quantity=qtyPriceTier.getKey();
                count++;

            }
            else if(qtyPriceTier.getKey().compareTo(orderLine.getQuantity()) < 0 && count !=0){
                tempQuantity=qtyPriceTier.getKey().subtract(quantity);
                logger.debug("creating new orderLineTier 2 {}", index + 1);
                createTier(orderLine, index+1, tempQuantity,
                        qtyPriceTier.getValue(), tempQuantity.multiply(qtyPriceTier.getValue()), total, oltiers,
                        oldQtyPriceTiers, TierRangeUtils::getTierRange);

                totalQuantitySaved= totalQuantitySaved.add(tempQuantity);
                index ++;
                quantity=qtyPriceTier.getKey();

            }
            else{
                if(totalQuantitySaved.compareTo(BigDecimal.ZERO) != 0){
                    logger.debug("creating new orderLineTier 3 {}", index + 1);
                    createTier(orderLine, index+1, orderLine.getQuantity().subtract(totalQuantitySaved),
                            qtyPriceTier.getValue(), orderLine.getQuantity().subtract(totalQuantitySaved).
                                    multiply(qtyPriceTier.getValue()), total, oltiers, oldQtyPriceTiers,TierRangeUtils::getTierRange);
                    index ++;
                }else{
                    logger.debug("creating new orderLineTier 4 {}", index + 1);
                    createTier(orderLine, index+1, orderLine.getQuantity(), qtyPriceTier.getValue(),
                            orderLine.getQuantity().multiply(qtyPriceTier.getValue()), total, oltiers, oldQtyPriceTiers,
                            TierRangeUtils::getTierRange);
                    index ++;

                }
            }
        }
        if(orderLine.getQuantity().compareTo(qty) > 0){
            logger.debug("creating new orderLineTier 5 {}", index + 1);
            createTier(orderLine, index+1, orderLine.getQuantity().subtract(qty), price,
                    orderLine.getQuantity().subtract(qty).multiply(price), total, oltiers, oldQtyPriceTiers, TierRangeUtils::getTierRange);
        }
        // set the updated order line tiers on the order line.
        logger.debug("oltiers are {}" , oltiers);
        return oltiers;
    }

    private int getTierIndex(BigDecimal totalQuantity, BigDecimal currentQuantity, SortedMap<BigDecimal, BigDecimal> qtyPriceTiers){
        BigDecimal usedQuantity = totalQuantity.subtract(currentQuantity);
        if(0 < usedQuantity.compareTo(BigDecimal.ZERO)) {
            int count = 0;
            BigDecimal previousQuantity = qtyPriceTiers.firstKey();
            for (Entry<BigDecimal, BigDecimal> qtyPriceTier : qtyPriceTiers.entrySet()) {
                BigDecimal quantity = qtyPriceTier.getKey();
                if(count > 0 && usedQuantity.compareTo(quantity) < 0 && usedQuantity.compareTo(previousQuantity) >= 0) {
                    return count - 1;
                }
                previousQuantity = quantity;
                count++;
            }
            return count - 1;
        }
        return 0;
    }

    /**
     * Creates a new orderLineTier
     * @param orderLine
     * @param tierNumber
     * @param quantity
     * @param price
     * @param amount
     * @param oltiers
     * @param qtyPriceTiers
     * @param tierRangeFunc
     */
    private void createTier(OrderLineDTO orderLine, Integer tierNumber, BigDecimal quantity, BigDecimal price,
                            BigDecimal amount, BigDecimal totalQuantity, List<OrderLineTierDTO> oltiers,
                            SortedMap<BigDecimal, BigDecimal> qtyPriceTiers,
                            BiFunction<SortedMap<BigDecimal, BigDecimal>, Integer,Pair<BigDecimal, BigDecimal>> tierRangeFunc){
        Pair<BigDecimal, BigDecimal> tierRange = tierRangeFunc.apply(qtyPriceTiers, tierNumber);
        int scale = 6;
        quantity = quantity.setScale(scale, BigDecimal.ROUND_HALF_UP);
        price = price.setScale(scale, BigDecimal.ROUND_HALF_UP);
        amount = amount.setScale(scale, BigDecimal.ROUND_HALF_UP);
        BigDecimal tierFrom = null == tierRange.getLeft() ? null : tierRange.getLeft().setScale(scale, BigDecimal.ROUND_HALF_UP);
        BigDecimal tierTo = (null != tierRange.getRight()) ? tierRange.getRight().setScale(scale, BigDecimal.ROUND_HALF_UP) : null;
        OrderLineTierDTO newTier = new OrderLineTierDTO(orderLine, tierNumber, quantity, price, amount, tierFrom, tierTo);

        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_ITEM);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas.findAll(table.getId(), orderLine.getItemId(),
                "description");
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            newTier.setDescription(descriptionDTO.getContent()+" 1",descriptionDTO.getId().getLanguageId() );
        }

        if(!oltiers.contains(newTier)){
            oltiers.add(newTier);
        }
    }

    void setOrderLineTiersInOrderLine(OrderLineDTO orderLineDTO, List<OrderLineTierDTO> orderLineTierDTOList){
        try {
            orderLineDTO.getOrderLineTiers().clear();
            orderLineDTO.getOrderLineTiers().addAll(orderLineTierDTOList);
        } catch (LazyInitializationException ex){
            orderLineDTO.setOrderLineTiers(orderLineTierDTOList);
            logger.debug("Encountered Lazy Initialization Exception due to no session. So setting the new orderLineTier " +
                    "list instead of clearing the old one", ex);
        }
    }
}
