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

package com.sapienter.jbilling.server.pluggableTask;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Basic tasks that takes the quantity and multiplies it by the price to 
 * get the lines total. It also updates the order total with the addition
 * of all line totals
 * 
 */
public class BasicLineTotalTask extends PluggableTask implements OrderProcessingTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BasicLineTotalTask.class));

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");


    public void doProcessing(OrderDTO order) throws TaskException {
        validateLinesQuantity(order.getLines());
        clearLineTotals(order.getLines());

        ItemDAS itemDas = new ItemDAS();
        ItemDTO item;

        /*
            Calculate non-percentage items, calculating price as $/unit
         */
        calculatePrice(order);
        for(OrderDTO orderDTO: order.getChildOrders()){
            calculatePrice(orderDTO);
        }

        /*
            Calculate non-tax percentage items (fees).
            Percentages are not compounded and charged only on normal item lines
         */
        for (OrderLineDTO line : order.getLines()) {
            if (line.getDeleted() == 1 || line.getTotalReadOnly()) continue;

            // calculate line total
            item = itemDas.find(line.getItemId());

            if (item != null
                && line.isPercentage()
                && !line.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX)) {

                // sum of applicable item charges * percentage
                BigDecimal total = getTotalForPercentage(order, item.getExcludedTypes());
                total = applyTotalForPercentageToChilds(order, total, item.getExcludedTypes());
                line.setAmount(line.getPrice().divide(ONE_HUNDRED, Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND).multiply(total));
                LOG.debug("percentage line total: %" + line.getPrice() + ";  "
                          + "( " + line.getPrice() + " / 100 ) x " + total  + " = " + line.getAmount());
            }
        }


        /*
            Calculate tax percentage items.
            Taxes are not compounded and charged on all normal item lines and non-tax percentage amounts (fees).
         */
        for (OrderLineDTO line : order.getLines()) {
            if (line.getDeleted() == 1 || line.getTotalReadOnly()) continue;

            // calculate line total
            item = itemDas.find(line.getItemId());

            if (item != null
                && item.isPercentage()
                && line.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX)) {

                // sum of applicable item charges + fees * percentage
                BigDecimal total = getTotalForTax(order, item.getExcludedTypes());
                total =  applyTotalForTaxToChilds(order, total, item.getExcludedTypes());
                line.setAmount(line.getPrice().divide(ONE_HUNDRED, Constants.BIGDECIMAL_SCALE, BigDecimal.ROUND_HALF_EVEN).multiply(total));

                LOG.debug("tax line total: %" + line.getPrice() + ";  "
                          + "( " + line.getPrice() + " / 100 ) x " + total  + " = " + line.getAmount());
            }
        }


        // order total
        order.setTotal(getTotal(order.getLines()));
        LOG.debug("Order total = %s", order.getTotal());
    }

    /**
     * Returns the sum total amount of all lines with items that do NOT belong to the given excluded type list.
     *
     * This total only includes normal item lines and not tax or penalty lines.
     *
     * @param order order
     * @param excludedTypes excluded item types
     * @return total amount
     */
    public BigDecimal getTotalForPercentage(OrderDTO order, Set<ItemTypeDTO> excludedTypes) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineDTO line : order.getLines()) {
            if (line.getDeleted() == 1) continue;

            // add line total for non-percentage & non-tax lines
            if (line.getItem() != null && !line.isPercentage() && line.getTypeId().equals(Constants.ORDER_LINE_TYPE_ITEM)
                    || line.getTypeId().equals(Constants.ORDER_LINE_TYPE_SUBSCRIPTION)) {

                // add if type is not in the excluded list
                if (!isItemExcluded(line.getItem(), excludedTypes)) {
                    total = total.add(line.getAmount());
                    if(line.getItem().isPlan() && line.getId() == 0){
                        total = applyTotalForPercentageToPlanItems(line.getItem().firstPlan().getPlanItems(),
                                                                   total, order.getActiveSince(), excludedTypes);
                    }
                } else {
                    LOG.debug("item %s excluded from percentage.", line.getItem().getId());
                }
            }
        }
        LOG.debug("total amount applicable for percentage: %s", total);

        return total;
    }

    /**
     * Returns the sum total amount of all lines with items that do NOT belong to the given excluded type list.
     *
     * This total includes all non tax lines (i.e., normal items, percentage fees and penalty lines).
     *
     * @param order order
     * @param excludedTypes excluded item types
     * @return total amount
     */
    public BigDecimal getTotalForTax(OrderDTO order, Set<ItemTypeDTO> excludedTypes) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineDTO line : order.getLines()) {
            if (line.getDeleted() == 1) continue;

            // add line total for all non-tax items
            if (!line.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX)) {

                // add if type is not in the excluded list
                if (!isItemExcluded(line.getItem(), excludedTypes)) {
                    total = total.add(line.getAmount());
                    if(line.getItem().isPlan() && line.getId() == 0){
                        total = applyTotalForTaxToPlanItems(line.getItem().firstPlan().getPlanItems(), total,
                                                            order.getActiveSince(), excludedTypes);
                    }
                } else {
                    LOG.debug("item %s excluded from tax.", line.getItem().getId());
                }
            }
        }
        LOG.debug("total amount applicable for tax: %s", total);

        return total;
    }

    /**
     * Returns true if the item is in the excluded item type list.
     *
     * @param item item to check
     * @param excludedTypes list of excluded item types
     * @return true if item is excluded, false if not
     */
    private boolean isItemExcluded(ItemDTO item, Set<ItemTypeDTO> excludedTypes) {
        for (ItemTypeDTO excludedType : excludedTypes) {
            for (ItemTypeDTO itemType : item.getItemTypes()) {
                if (itemType.getId() == excludedType.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the total of all given order lines.
     *
     * @param lines order lines
     * @return total amount
     */
    public BigDecimal getTotal(List<OrderLineDTO> lines) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 1) continue;

            // add total
            if (line.getAmount() != null)
            	total = total.add(line.getAmount());
        }

        return total;
    }

    /**
     * Sets all order line amounts to null.
     *
     * @param lines order lines to clear
     */
    public void clearLineTotals(List<OrderLineDTO> lines) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 1 || line.getTotalReadOnly()) continue;

            // clear amount
            if (line.getItem() != null)
            	line.setAmount(null);
        }
    }

    /**
     * Validates that only order line items with {@link ItemDTO#hasDecimals} set to true has
     * a decimal quantity.
     *
     * @param lines order lines to validate
     * @throws TaskException thrown if an order line has decimals without the item hasDecimals flag
     */
    public void validateLinesQuantity(List<OrderLineDTO> lines) throws TaskException {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 1) continue;

            // validate line quantity
            if (line.getItem() != null && line.getQuantity() != null
                    && line.getQuantity().remainder(Constants.BIGDECIMAL_ONE).compareTo(BigDecimal.ZERO) != 0.0
                    && line.getItem().getHasDecimals() == 0) {

                throw new TaskException(new ItemDecimalsException("Item does not allow Decimals"));
            }
        }
    }

    public BigDecimal applyTotalForTaxToChilds(OrderDTO order, BigDecimal total, Set<ItemTypeDTO> excludedTypes) throws TaskException {
        for(OrderDTO orderDTO: order.getChildOrders()){
            total = total.add(getTotalForTax(orderDTO, excludedTypes));
            if(orderDTO.getChildOrders() != null && !orderDTO.getChildOrders().isEmpty()){
                total = applyTotalForTaxToChilds(orderDTO, total, excludedTypes);
            }
        }

        return total;
    }

    public BigDecimal applyTotalForTaxToPlanItems(List<PlanItemDTO> planItems, BigDecimal total, Date pricingDate,
                                                        Set<ItemTypeDTO> excludedTypes){
        for(PlanItemDTO planItem: planItems){
            if(!planItem.getItem().getOrderLineTypeId().equals(Constants.ORDER_LINE_TYPE_TAX) &&
                planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) != 0 && !isItemExcluded(planItem.getItem(), excludedTypes)){
                total = total.add(planItem.getBundle().getQuantity()
                        .multiply(PriceModelBL.getPriceForDate(planItem.getModels(), pricingDate).getRate()));
            }
        }

        return total;
    }

    public BigDecimal applyTotalForPercentageToChilds(OrderDTO order, BigDecimal total, Set<ItemTypeDTO> excludedTypes) throws TaskException {
        for (OrderDTO orderDTO : order.getChildOrders()) {
            total = total.add(getTotalForPercentage(orderDTO, excludedTypes));
            if(orderDTO.getChildOrders() != null && !orderDTO.getChildOrders().isEmpty()){
                total = applyTotalForPercentageToChilds(orderDTO, total, excludedTypes);
            }
        }

        return total;
    }

    public BigDecimal applyTotalForPercentageToPlanItems(List<PlanItemDTO> planItems, BigDecimal total, Date pricingDate,
                                                         Set<ItemTypeDTO> excludedTypes){
        for(PlanItemDTO planItem: planItems){
            if(!planItem.getItem().isPercentage() && planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) != 0
                && !isItemExcluded(planItem.getItem(), excludedTypes)) {
                total = total.add(planItem.getBundle().getQuantity()
                             .multiply(PriceModelBL.getPriceForDate(planItem.getModels(), pricingDate).getRate()));
            }
        }

        return total;
    }

    public void calculatePrice(OrderDTO order){
        for (OrderLineDTO line : order.getLines()) {
            if (line.getDeleted() == 1 || line.getTotalReadOnly()) continue;

            // calculate line total
            if ( null != line.getPrice() && line.getQuantity() != null && !line.isPercentage()) {
                line.setAmount(line.getQuantity().multiply(line.getPrice()));

                LOG.debug("normal line total: %s x %s = %s", line.getQuantity(), line.getPrice(), line.getAmount());
            }
        }
    }
}
