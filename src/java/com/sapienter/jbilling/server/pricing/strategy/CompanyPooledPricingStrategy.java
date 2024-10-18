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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.UsageBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CompanyPooledPricingStrategy extends AbstractPricingStrategy {

    public static final String POOL_CATEGORY_ATTR_NAME = "pool_item_category_id";
    public static final String INCLUDED_QUANTITY = "included_quantity";

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CompanyPooledPricingStrategy.class));

    public CompanyPooledPricingStrategy () {
        setAttributeDefinitions(
                new AttributeDefinition(POOL_CATEGORY_ATTR_NAME, Type.INTEGER, true),
                new AttributeDefinition(INCLUDED_QUANTITY, Type.INTEGER, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
        setVariableUsagePricing(true);
    }

    /**
     * Applies the plan's pricing strategy to the given pricing request.
     * <p/>
     * The usage totals will already include quantity being priced if pricing an order
     * during a create/update or order rating call.
     *
     * @param pricingOrder   target order for this pricing request (may be null)
     * @param result         pricing result to apply pricing to
     * @param fields         pricing fields
     * @param planPrice      the plan price to apply
     * @param quantity       quantity of item being priced
     * @param usage          total item usage for this billing period
     * @param singlePurchase true if pricing a single purchase/addition to an order, false if pricing a quantity that already exists on the pricingOrder
     * @param orderLineDTO
     * @throws IllegalArgumentException if strategy requires usage, and usage was given as null
     */
    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        if (pricingOrder != null) {

            ItemBL itemBL = new ItemBL();

            Integer poolCategory = Integer.parseInt(planPrice.getAttributes().get(POOL_CATEGORY_ATTR_NAME));

            //USAGE
            //get all the items in the pool category
            ItemDTOEx[] items = itemBL.getAllItemsByType(poolCategory, pricingOrder.getUser().getEntity().getId());
            BigDecimal totalUsage = getTotalUsage(pricingOrder, items);

            //POOL SIZE
            BigDecimal totalPool = BigDecimal.ZERO;
            //get all the plans with an item with CompanyPooledPriceStrategy configured
            //check that they have the same pool category as a parameter
            List<PlanDTO> plans = new PlanBL().getPoolContributingPlans(poolCategory);

            for (PlanDTO plan : plans) {

                //get the usage of that plan for all the users in the same company
                ItemDTOEx planItem = itemBL.getWS(new PlanBL(plan.getId()).getEntity().getItem());
                BigDecimal planUsage = getTotalUsage(pricingOrder, new ItemDTOEx[]{planItem});

                totalPool = calculatePoolSize(plan, poolCategory, totalPool, planUsage);
            }

            //SET THE FINAL PRICE

            if (totalUsage.compareTo(totalPool) <= 0) { //for free
                result.setPrice(BigDecimal.ZERO);
            } else if (totalUsage.subtract(totalPool).compareTo(quantity) < 0) { //mix
                result.setPrice(planPrice.getRate().multiply(totalUsage.subtract(totalPool)).divide(quantity, RoundingMode.HALF_UP).setScale(CommonConstants.BIGDECIMAL_SCALE, RoundingMode.HALF_UP));
            } else { //charge all
                result.setPrice(planPrice.getRate());
            }

        }
    }

    private BigDecimal calculatePoolSize (PlanDTO plan, Integer poolCategory, BigDecimal totalPool, BigDecimal planUsage){
        //for each of those pooled items
        for(PlanItemDTO planItem:plan.getPlanItems()){
            for(PriceModelDTO priceModel: planItem.getModels().values()){
                if (priceModel.getType()== PriceModelStrategy.COMPANY_POOLED){
                    String planItemPoolCat = priceModel.getAttributes().get(POOL_CATEGORY_ATTR_NAME);
                    if(planItemPoolCat.equals(poolCategory.toString())){
                        // get the included quantity in the plan and multiply it with the plan usage
                        // add this value to the total pool size
                        BigDecimal includedQuantity = new BigDecimal(priceModel.getAttributes().get(INCLUDED_QUANTITY));
                        totalPool = totalPool.add(includedQuantity.multiply(planUsage));
                        break;
                    }
                }
            }
        }
        return totalPool;
    }

    private BigDecimal getTotalUsage (OrderDTO order, ItemDTOEx[] items) {
        UsageBL usageBL = new UsageBL(order.getUserId(), order);

        BigDecimal usage = BigDecimal.ZERO;


        for (ItemDTOEx item : items) {
            usage = usage.add(usageBL.getItemUsage(item.getId(), true).getQuantity());
        }

        return usage;
    }

}
