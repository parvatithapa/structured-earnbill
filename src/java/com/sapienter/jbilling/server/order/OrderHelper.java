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

package com.sapienter.jbilling.server.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections4.MapUtils;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.PriceModelResolutionContext;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.tasks.PriceModelPricingTask;
import com.sapienter.jbilling.server.util.Constants;

/**
 * OrderHelper
 *
 * Swap item
 * Swap excess
 * Merge lines (combine prices & quantities)
 * Flatten (merges all lines where the item id is equal)
 * Purge item (remove all lines of item)
 * Copy
 *
 *
 * @author Brian Cowdery
 * @since 28/06/11
 */
public class OrderHelper {

    /**
     * Replaces all existing order lines with a different item.
     *
     * @param order order containing lines to swap
     * @param oldItemId old item id (to be swapped out)
     * @param newItemId new item id
     * @return order with swapped lines
     */
    public static OrderDTO swap(OrderDTO order, Integer oldItemId, Integer newItemId) {
        throw new UnsupportedOperationException("Swap not yet implemented.");
    }

    /**
     * Replaces a quantity of existing order lines with a different item.
     *
     * If the order contains less than the desires swap quantity then the entire existing order
     * line will be removed an equivalent line will be added using the new item.
     *
     * If the order contains more than the desired swap quantity then the desired swap quantity
     * will be subtracted from the existing line and a new line will be added with the new item.
     *
     * @param order order containing lines to swap
     * @param oldItemId old item id (to be swapped out)
     * @param newItemId new item id
     * @param quantity quantity to swap if available
     * @return order with swapped lines
     */
    public static OrderDTO swap(OrderDTO order, Integer oldItemId, Integer newItemId, BigDecimal quantity) {
        throw new UnsupportedOperationException("Swap not yet implemented.");
    }

    /**
     * Replaces excess of an existing order lines with a different item when the total line quantity
     * exceeds the given quantity.
     *
     * @param order order containing lines to swap
     * @param oldItemId old item id (to be swapped out)
     * @param newItemId new item id
     * @param quantity target quantity, items will be swapped if the existing order quantity exceeds this value.
     * @return order with swapped lines
     */
    public static OrderDTO swapExcess(OrderDTO order, Integer oldItemId, Integer newItemId, BigDecimal quantity) {
        throw new UnsupportedOperationException("Swap excess not yet implemented.");
    }

    /**
     * Replaces excess of an existing order lines with a new line at a set price when the total line quantity
     * exceeds the given quantity.
     *
     * This operates the same as {@link #swapExcess(OrderDTO, Integer, Integer, BigDecimal)} only instead of
     * adding a line with a new item, the quantity is moved to a new line using the same item (but a different price).
     *
     * @param order order containing lines to swap
     * @param itemId item id
     * @param quantity target quantity, items will be swapped if the existing order quantity exceeds this value.
     * @param price price for the new line
     * @return order with swapped lines
     */
    public static OrderDTO swapExcess(OrderDTO order, Integer itemId, BigDecimal quantity, BigDecimal price) {
        throw new UnsupportedOperationException("Swap excess not yet implemented.");
    }

    /**
     * Merges two orders together to create a new order. The first orders attributes (active dates, status etc.)
     * will be used for the newly merged order, except where null. If an attribute is null then the value
     * from the second order will be used.
     *
     * Order lines from the two orders are merged together in the same manner as
     * {@link #merge(OrderLineDTO, OrderLineDTO)}.
     *
     * @param order1 order to merge
     * @param order2 other order to merge
     * @return merged order
     */
    public static OrderDTO merge(OrderDTO order1, OrderDTO order2) {
        OrderDTO merged = new OrderDTO(order1);

        if (merged.getBaseUserByUserId() == null) merged.setBaseUserByUserId(order2.getBaseUserByUserId());
        if (merged.getCurrency() == null) merged.setCurrency(order2.getCurrency());
        if (merged.getOrderStatus() == null) merged.setOrderStatus(order2.getOrderStatus());
        if (merged.getOrderPeriod() == null) merged.setOrderPeriod(order2.getOrderPeriod());
        if (merged.getOrderBillingType() == null) merged.setOrderBillingType(order2.getOrderBillingType());
        if (merged.getActiveSince() == null) merged.setActiveSince(order2.getActiveSince());
        if (merged.getActiveUntil() == null) merged.setActiveUntil(order2.getActiveUntil());
        if (merged.getCreateDate() == null) merged.setCreateDate(order2.getCreateDate());
        if (merged.getNextBillableDay() == null) merged.setNextBillableDay(order2.getNextBillableDay());
        if (merged.getLastNotified() == null) merged.setLastNotified(order2.getLastNotified());
        if (merged.getNotificationStep() == null) merged.setNotificationStep(order2.getNotificationStep());
        if (merged.getDueDateUnitId() == null) merged.setDueDateUnitId(order2.getDueDateUnitId());
        if (merged.getDueDateValue() == null) merged.setDueDateValue(order2.getDueDateValue());
        if (merged.getDfFm() == null) merged.setDfFm(order2.getDfFm());
        if (merged.getAnticipatePeriods() == null) merged.setAnticipatePeriods(order2.getAnticipatePeriods());
        if (merged.getOwnInvoice() == null) merged.setOwnInvoice(order2.getOwnInvoice());
        if (merged.getNotes() == null) merged.setNotes(order2.getNotes());
        if (merged.getNotesInInvoice() == null) merged.setNotesInInvoice(order2.getNotesInInvoice());
        if (merged.getProrateFlag() == null) merged.setProrateFlag(order2.getProrateFlag());

        merged.getOrderProcesses().clear();
        merged.setId(0);
        merged.setDeleted(0);

        merged.getLines().addAll(order2.getLines());
        for (OrderLineDTO line : merged.getLines()) {
            line.setId(0);
        }

        return flatten(merged);
    }

    /**
     * Merges the given order lines together. The merged order line will have a
     * quantity and amount that is the sum of the original two lines. The merged line
     * will not be associated with an order. The merged line will have all the assets, but
     * the assets will not be linked to the new line, they are still linked to the old line.
     *
     * The first line's attributes (description, item id etc.) will be used for the
     * newly merged line, except where null. If an attribute is null the the value
     * from the second line will be used.
     *
     * The unit price may no longer match the merged line total if the original two
     * lines had a different unit prices.
     *
     * @param line1 line to merge
     * @param line2 other line to merge
     * @return merged order line
     */
    public static OrderLineDTO merge(OrderLineDTO line1, OrderLineDTO line2) {
        OrderLineDTO merged = new OrderLineDTO(line1);

        merged.setAmount(add(line1.getAmount(), line2.getAmount()));
        merged.setQuantity(add(line1.getQuantity(), line2.getQuantity()));

        if (merged.getOrderLineType() == null) merged.setOrderLineType(line2.getOrderLineType());
        if (merged.getItem() == null) merged.setItem(line2.getItem());
        if (merged.getPrice() == null) merged.setPrice(line2.getPrice());
        if (merged.getCreateDatetime() == null) merged.setCreateDatetime(line2.getCreateDatetime());
        if (merged.getDescription() == null) merged.setDescription(line2.getDescription());
        if (merged.getUseItem() == null) merged.getUseItem();

        merged.getAssets().clear();
        merged.addAssets(line2.getAssets());
        line2.getAssets().clear();
        merged.addAssets(line1.getAssets());
        line1.getAssets().clear();

        merged.setPurchaseOrder(null);
        merged.setId(0);
        merged.setDeleted(0);

        return merged;
    }

    /**
     * Removes all lines from the order with a matching item ID.
     *
     * @param order order to purge items from
     * @param itemId item ID to remove
     * @return purged order
     */
    public static OrderDTO purge(OrderDTO order, Integer itemId) {
        for (Iterator<OrderLineDTO> it = order.getLines().iterator(); it.hasNext();) {
            OrderLineDTO line = it.next();
            if (line.getItemId().equals(itemId)) {
                it.remove();
            }
        }

        return order;
    }

    /**
     * Removes all lines from the order with an item ID contained in the given list of IDs.
     *
     * @param order order to purge items from
     * @param itemIds item IDs to remove
     * @return purged order
     */
    public static OrderDTO purge(OrderDTO order, Collection<Integer> itemIds) {
        for (Iterator<OrderLineDTO> it = order.getLines().iterator(); it.hasNext();) {
            OrderLineDTO line = it.next();
            if (itemIds.contains(line.getItemId())) {
                it.remove();
            }
        }

        return order;
    }

    /**
     * Marks all lines from the order with a matching item ID as deleted.
     *
     * @param order order to delete lines from
     * @param itemId item ID to delete
     * @return order with line deleted
     */
    public static OrderDTO delete(OrderDTO order, Integer itemId) {
        for (OrderLineDTO line : order.getLines()) {
            if (line.getItemId().equals(itemId)) {
                line.setDeleted(1);
            }
        }

        return order;
    }

    /**
     * Marks all lines from the order with an item ID contained in the given list of IDs.
     *
     * @param order order to delete lines from
     * @param itemIds item IDs to delete
     * @return order with line deleted
     */
    public static OrderDTO delete(OrderDTO order, Collection<Integer> itemIds) {
        for (OrderLineDTO line : order.getLines()) {
            if (itemIds.contains(line.getItemId())) {
                line.setDeleted(1);
            }
        }

        return order;
    }

    /**
     * Flattens an order by removing all empty order lines, and merging all order
     * lines down so that there are no duplicate item IDs.
     *
     * @param order order to flatten
     * @return flattened order
     */
    public static OrderDTO flatten(OrderDTO order) {

        // remove all empty lines
        for (Iterator<OrderLineDTO> it = order.getLines().iterator(); it.hasNext();) {
            OrderLineDTO line = it.next();

            if (line.getDeleted() == 1
                || (line.getQuantity().compareTo(BigDecimal.ZERO) == 0 && line.getAmount().compareTo(BigDecimal.ZERO) == 0)) {

                it.remove();
            }
        }

        // merge lines with the same item
        List<OrderLineDTO> mergedLines = new ArrayList<OrderLineDTO>();
        for (OrderLineDTO line : order.getLines()) {

            OrderLineDTO merged = find(mergedLines, line.getItemId(), line.getPrice());
            if (merged == null) {
                // new line, add to list of merged lines
                mergedLines.add(line);

            } else {
                // existing line, merge with line from list
                mergedLines.remove(merged);
                mergedLines.add(merge(merged, line));
            }
        }

        order.setLines(mergedLines);

        return order;
    }

    /**
     * Returns all order lines from the given order where the item ID matches.
     *
     * @param order order
     * @param itemId item id of lines to collect
     * @return list of collected order lines, empty list if none found.
     */
    public static List<OrderLineDTO> collect(OrderDTO order, Integer itemId) {
        List<OrderLineDTO> lines = new ArrayList<OrderLineDTO>();

        for (OrderLineDTO line : order.getLines()) {
            if (line.getItemId().equals(itemId) && line.getDeleted() == 0) {
                lines.add(line);
            }
        }

        return lines;
    }

    /**
     * Returns the line matching the given item ID and price. If no price is given (price is null),
     * then the first line with a matching item ID will be returned.
     *
     * @param lines lines
     * @param itemId item ID to find
     * @param price optional item price
     * @return found line, null if no line found
     */
    public static OrderLineDTO find(Collection<OrderLineDTO> lines, Integer itemId, BigDecimal price) {
        for (OrderLineDTO line : lines) {
            if (line.getItemId().equals(itemId)) {
                if (price != null) {
                    if (line.getPrice().compareTo(price) == 0) {
                        return line;
                    }
                } else {
                    return line;
                }
            }
        }

        return null;
    }

    /**
     * Returns the line matching the given item ID.
     *
     * @param lines lines
     * @param itemId item ID to find
     * @return found line, null if no line found
     */
    public static OrderLineWS find(OrderLineWS[] lines, Integer itemId) {
        for (OrderLineWS line : lines) {
            if (line.getDeleted() == 0 && line.getItemId().equals(itemId)) {
                return line;
            }
        }

        return null;
    }

    /**
     * Find the order line by id
     *
     * @param lines - lines to search through
     * @param id - line id to find
     * @return order line if found, null otherwise
     */
    public static OrderLineDTO findOrderLineWithId(Collection<OrderLineDTO> lines, int id) {
        for(OrderLineDTO dto : lines) {
            if(dto.getId() == id) return dto;
        }
        return null;
    }

    /**
     * Copy the collection of order lines to new one. Each line will be copied too.
     * @param persistedOrderLines  Order lines to copy
     * @return new collection of order lines copies
     */
    public static List<OrderLineDTO> copyOrderLinesToDto(List<OrderLineDTO> persistedOrderLines) {
        List<OrderLineDTO> result = new LinkedList<OrderLineDTO>();
        for (OrderLineDTO line : persistedOrderLines) {
            OrderLineDTO dto = new OrderLineDTO(line);
            Date effectiveDate = dto.getPurchaseOrder().getActiveSince();
            dto.getOrderLineUsagePools().clear();
            if (line.hasOrderLineUsagePools()) {
	            Set<OrderLineUsagePoolDTO> olUsagePools = new HashSet<OrderLineUsagePoolDTO>();
	            for (OrderLineUsagePoolDTO olUsagePool : line.getOrderLineUsagePools()) {
	            	olUsagePools.add(new OrderLineUsagePoolDTO(olUsagePool.getId(), olUsagePool.getOrderLine(), 
	            		olUsagePool.getQuantity(), olUsagePool.getCustomerUsagePool(), effectiveDate));
	            }
	            dto.setOrderLineUsagePools(olUsagePools);
            }
            
            result.add(dto);
        }
        return result;
    }

    /**
     * Converts an OrderDTO to use a thread-safe <code>CopyOnWriteArrayList</code> for order lines instead
     * of an <code>ArrayList</code>. This allows safe concurrent modification of the order lines during
     * iteration.
     *
     * You can also use Collections.synchronizedList, however the this method will provide better
     * performance unless the number of writes to the list vastly outnumber the reads.
     *
     * @param order order to synchronize
     */
    public static void synchronizeOrderLines(OrderDTO order) {
        order.setLines(new CopyOnWriteArrayList<OrderLineDTO>(order.getLines()));
    }

    /**
     * Removes synchronization on the OrderDTO order lines collection, copying the elements back
     * into an <code>ArrayList</code>.
     *
     * @param order order to desynchronize
     */
    public static void desynchronizeOrderLines(OrderDTO order) {
        order.setLines(new ArrayList<OrderLineDTO>(order.getLines()));
    }


    public static OrderDTO findRootOrderIfPossible(OrderDTO order) {
        OrderDTO rootOrder = order;
        // find root order, prevent cycle if it exists in hierarchy
        while (rootOrder.getParentOrder() != null && !rootOrder.getParentOrder().equals(order)) {
            rootOrder = rootOrder.getParentOrder();
        }
        return rootOrder;
    }

    public static OrderWS findRootOrderIfPossible(OrderWS order) {
        OrderWS rootOrder = order;
        // find root order, prevent cycle if it exists in hierarchy
        while (rootOrder.getParentOrder() != null && !rootOrder.getParentOrder().equals(order)) {
            rootOrder = rootOrder.getParentOrder();
        }
        return rootOrder;
    }

    public static LinkedHashSet<OrderDTO> findOrdersInHierarchyFromRootToChild(OrderDTO rootOrder) {
        LinkedHashSet<OrderDTO> orders = new LinkedHashSet<OrderDTO>();
        Set<OrderDTO> visitedOrder = new HashSet<OrderDTO>();
        orders.add(rootOrder);
        findOrdersInHierarchy(rootOrder, orders, visitedOrder);
        return orders;
    }

    private static void findOrdersInHierarchy(OrderDTO order, LinkedHashSet<OrderDTO> orders, Set<OrderDTO> visitedOrders) {
        if (visitedOrders.contains(order)) return;
        // this collection does not preserve order from root to child level by level, but prevents cycles
        visitedOrders.add(order);
        if (order.getChildOrders() != null) {
            // add to collection to preserve hierarchy ordering level by level
            orders.addAll(order.getChildOrders());
            for (OrderDTO childOrder : order.getChildOrders()) {
                findOrdersInHierarchy(childOrder, orders, visitedOrders);
            }
        }
    }

    public static LinkedHashSet<OrderWS> findAllOrdersInHierarchy(OrderWS order) {
        OrderWS rootOrder = findRootOrderIfPossible(order);
        LinkedHashSet<OrderWS> result = new LinkedHashSet<OrderWS>();
        result.add(rootOrder);
        result.addAll(findAllChildren(rootOrder));
        return result;
    }

    public static LinkedHashSet<OrderWS> findAllChildren(OrderWS rootOrder) {
        LinkedHashSet<OrderWS> orders = new LinkedHashSet<OrderWS>();
        findChildren(rootOrder, orders);
        return orders;
    }

    private static void findChildren(OrderWS order, LinkedHashSet<OrderWS> orders) {
        if (order == null) return;
        if (order.getChildOrders() != null) {
            List<OrderWS> newChildren = new LinkedList<OrderWS>(Arrays.asList(order.getChildOrders()));
            newChildren.removeAll(orders);
            orders.addAll(newChildren);
            for (OrderWS childOrder : newChildren) {
                findChildren(childOrder, orders);
            }
        }
    }

    /**
     * Search for target order in given hierarchy
     * @param order any order from hierarchy to search target order
     * @param targetOrderId target order
     * @return order if found, null otherwise
     */
    public static OrderWS findOrderInHierarchy(OrderWS order, Integer targetOrderId) {
        OrderWS rootOrder = findRootOrderIfPossible(order);
        if (rootOrder.getId() != null && rootOrder.getId().equals(targetOrderId)) {
            return rootOrder;
        }
        OrderWS result = null;
        if (rootOrder.getChildOrders() != null) {
            for (OrderWS childOrder : rootOrder.getChildOrders()) {
                result = findOrderInHierarchy(childOrder, targetOrderId, rootOrder);
                if (result != null) return result;
            }
        }
        return result;
    }

    private static OrderWS findOrderInHierarchy(OrderWS order, int targetOrderId, OrderWS stopOnOrder) {
        // prevent cycle
        if (order.equals(stopOnOrder)) return null;
        if (order.getId() != null && order.getId().equals(targetOrderId)) return order;
        OrderWS result = null;
        if (order.getChildOrders() != null) {
            for (OrderWS childOrder : order.getChildOrders()) {
                result = findOrderInHierarchy(childOrder, targetOrderId, order);
                if (result != null) return result;
            }
        }
        return result;
    }

    /**
     * Search for target order in given hierarchy
     * @param order any order from hierarchy to search target order
     * @param targetOrderId target order
     * @return order if found, null otherwise
     */
    public static OrderDTO findOrderInHierarchy(OrderDTO order, Integer targetOrderId) {
        OrderDTO rootOrder = findRootOrderIfPossible(order);
        if (rootOrder.getId() != null && rootOrder.getId().equals(targetOrderId)) {
            return rootOrder;
        }
        OrderDTO result = null;
        if (rootOrder.getChildOrders() != null) {
            for (OrderDTO childOrder : rootOrder.getChildOrders()) {
                result = findOrderInHierarchy(childOrder, targetOrderId, rootOrder);
                if (result != null) return result;
            }
        }
        return result;
    }

    private static OrderDTO findOrderInHierarchy(OrderDTO order, int targetOrderId, OrderDTO stopOnOrder) {
        // prevent cycle
        if (order.equals(stopOnOrder)) return null;
        if (order.getId() != null && order.getId().equals(targetOrderId)) return order;
        OrderDTO result = null;
        if (order.getChildOrders() != null) {
            for (OrderDTO childOrder : order.getChildOrders()) {
                result = findOrderInHierarchy(childOrder, targetOrderId, stopOnOrder);
                if (result != null) return result;
            }
        }
        return result;
    }

    /**
     * Null safe convenience method for adding two BigDecimal objects.
     *
     * @param a big decimal two
     * @param b big decimal one
     * @return sum of the two big decimals
     */
    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;

        return a.add(b);
    }

    public static boolean itemHasTeaserPricing(Integer userId, Integer itemId, Date pricingDate, Integer entityId) {
        PluggableTaskManager taskManager = null;
        PriceModelPricingTask priceModelPricingTask = null;
        boolean hasTeaserPricing = false;
        try {
            taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_ITEM_PRICING);
            priceModelPricingTask = (PriceModelPricingTask) taskManager.getNextClass();
        }catch(PluggableTaskException e){
            throw new SessionInternalError("Error Handling Price Model Pricing Task");
        }
        if(null != priceModelPricingTask){
            PriceModelResolutionContext modelResolutionContext = PriceModelResolutionContext.builder(itemId)
                    .user(userId)
                    .pricingDate(pricingDate)
                    .attributes(Collections.emptyMap())
                    .build();

            SortedMap<Date, PriceModelDTO> models = priceModelPricingTask.getPricesByHierarchy(modelResolutionContext);
            if(MapUtils.isNotEmpty(models)) {
                PriceModelDTO model = PriceModelBL.getPriceForDate(models, pricingDate);
                hasTeaserPricing = model.getType().equals(PriceModelStrategy.TEASER_PRICING);
            }
        }
        ItemDTO item = new ItemDAS().find(itemId);
        PriceModelDTO priceModelDto =  item.getPrice(new Date());
        if(null != priceModelDto){
            hasTeaserPricing = priceModelDto.getType().equals(PriceModelStrategy.TEASER_PRICING);
        }
        return hasTeaserPricing;
    }

}
