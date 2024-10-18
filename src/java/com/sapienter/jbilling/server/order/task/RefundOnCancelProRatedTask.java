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
package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.PeriodCancelledEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class RefundOnCancelProRatedTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            PeriodCancelledEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
   
    public void process(Event event) {
        OrderDTO order;

        PeriodCancelledEvent periodCancelledEvent;
        // validate the type of the event
        if (event instanceof PeriodCancelledEvent) {
            periodCancelledEvent = (PeriodCancelledEvent) event;
            order = periodCancelledEvent.getOrder();
            logger.debug("Plug in processing period cancelled event for order {}", order.getId());
        } else {
            throw new SessionInternalError("Can't process anything but a period cancel event");
        }
        
        if (PreferenceBL.getPreferenceValueAsBoolean(periodCancelledEvent.getEntityId(), CommonConstants.PREFERENCE_APPLY_ONLY_TO_UPGRADE_ORDERS) &&
                order.getUpgradeOrderId() == null) {
            logger.info("Preference Apply Only To Upgrade Orders is enabled and order {} has upgradeOrderId null", order.getId());
            return;
        }

        // local variables
        Integer setupCategoryId = MetaFieldBL.getMetaFieldIntegerValueNullSafety(new EntityBL(getEntityId()).getEntity().getMetaField(com.sapienter.jbilling.client.util.Constants.SETUP_META_FIELD));
        Integer userId = new OrderDAS().find(order.getId()).getBaseUserByUserId().getUserId(); // the order might not be in the session
        Integer entityId = event.getEntityId();
        UserBL userBL;
        ResourceBundle bundle;
        try {
            userBL = new UserBL(userId);
            bundle = ResourceBundle.getBundle("entityNotifications", userBL.getLocale());
        } catch (Exception e) {
            throw new SessionInternalError("Error when doing credit", RefundOnCancelProRatedTask.class, e);
        }

        // create a new order that is the same as the original one, but all
        // negative prices
        OrderBL orderBL = new OrderBL(order);
        OrderDTO newOrder = new OrderDTO(order);
        // reset the ids, so it is a new order
        newOrder.setId(null);
        newOrder.setVersionNum(null);
        newOrder.setParentOrder(null);
        newOrder.getOrderProcesses().clear(); // no invoices created for a new order
        newOrder.getLines().clear();
        newOrder.getChildOrders().clear();
        // starts where the cancellation starts
        newOrder.setActiveSince(order.getActiveUntil());
        // ends where the original would invoice next
        newOrder.setActiveUntil(order.getNextBillableDay());
        newOrder.setNextBillableDay(null);
        // add some clarification notes
        newOrder.setNotes(bundle.getString("order.credit.notes") + " " + order.getId());
        newOrder.setNotesInInvoice(0);
        newOrder.setCreateDate(new Date());
        newOrder.getDiscountLines().clear();
        //
        // order lines:
        //
        getLinesWithoutSetupProducts(order.getLines(), setupCategoryId).forEach(line -> {
            OrderLineDTO newLine = new OrderLineDTO(line);
            newLine.getAssets().clear();
            // reset so they get inserted
            newLine.setId(0);
            newLine.setVersionNum(null);
            newLine.setUseItem(false);
            newLine.setPurchaseOrder(newOrder);
            newOrder.getLines().add(newLine);

            // make the order negative (refund/credit)
            newLine.setQuantity(line.getItem().isPlan() ? line.getQuantity().negate() : BigDecimal.ZERO);
            newLine.setPrice(newLine.getPrice().subtract(orderBL.prorateTotalAmount(newLine.getPrice(), new Date())));
        });

        // add extra lines with items from the parameters
        for (String name : parameters.keySet()) {
            if (!name.startsWith("item")) {
                logger.warn("parameter is not an item: {}", name);
                continue; // not an item parameter
            }
            int itemId = Integer.parseInt(parameters.get(name));
            logger.debug("adding item {} to new order", itemId);
            ItemDTO item = new ItemDAS().findNow(itemId);
            if (item == null || !getEntityId().equals(event.getEntityId())) {
                logger.error("Item {} not found", itemId);
                continue;
            }
            OrderLineDTO newLine = new OrderLineDTO();
            newLine.setDeleted(0);

            newLine.setDescription(item.getDescription(userBL.getEntity().getLanguageIdField()));
            newLine.setItem(item);
            newLine.setOrderLineType(new OrderLineTypeDAS().find(Constants.ORDER_LINE_TYPE_ITEM));
            newLine.setQuantity(1);
            newLine.setPurchaseOrder(newOrder);

            try {
                newLine.setPrice(new ItemBL(itemId).getPrice(userId, newLine.getQuantity(), entityId));
            } catch (Exception e) {
                throw new SessionInternalError("Error when doing credit", RefundOnCancelProRatedTask.class, e);
            }

            newOrder.getLines().add(newLine);
        }

        if (!order.getLines().isEmpty()) {
            // do the maths
            orderBL.set(newOrder);
            try {
                orderBL.recalculate(entityId);
            } catch (ItemDecimalsException e) {
                throw new SessionInternalError("Error when doing credit", RefundOnCancelProRatedTask.class, e);
            }

            // save
            orderBL.setAddBundleItems(isBundleItemsAllowed(order, setupCategoryId));
            Integer newCreditOrderId = orderBL.create(entityId, null, newOrder, new HashMap<>());

            // audit so we know why all these changes happened
            new EventLogger().auditBySystem(entityId, userId, Constants.TABLE_PUCHASE_ORDER, order.getId(),
                                            EventLogger.MODULE_ORDER_MAINTENANCE, EventLogger.ORDER_CANCEL_AND_CREDIT,
                                            newCreditOrderId, null, null);

            order.setNotes(order.getNotes() + " - " + bundle.getString("order.cancelled.notes") + " " + newCreditOrderId);
            logger.debug("Credit done with new order {}", newCreditOrderId);
        }

        // Update original order
        OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
        order.setOrderStatus(orderStatusDAS.find(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, entityId)));
    }

    public String toString() {
        return "RefundOnCancelProratedTask for events " + Arrays.toString(events);
    }

    private List<OrderLineDTO> getLinesWithoutSetupProducts(List<OrderLineDTO> orderLines, Integer setupCategoryId) {
        return orderLines.stream()
                .filter(line -> line.getItem() != null &&
                        line.getItem()
                                .getItemTypes()
                                .stream()
                                .noneMatch(itemType -> setupCategoryId != null && itemType.getId() == setupCategoryId))
                .collect(Collectors.toList());
    }

    private boolean isBundleItemsAllowed(OrderDTO bundleOrder, Integer setupCategoryId) {
        return !bundleOrder.getChildOrders()
                           .stream()
                           .flatMap(childOrder -> getLinesWithoutSetupProducts(childOrder.getLines(), setupCategoryId).stream())
                           .collect(Collectors.toList())
                           .isEmpty();

    }
}
