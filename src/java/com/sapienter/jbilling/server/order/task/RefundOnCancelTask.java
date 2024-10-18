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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.PeriodCancelledEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.task.SwapPlanFUPTransferTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

public class RefundOnCancelTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_ADJUSTMENT_PRODUCT =
            new ParameterDescription("adjustment_product_id", true, ParameterDescription.Type.INT);

    public RefundOnCancelTask() {
        descriptions.add(PARAM_ADJUSTMENT_PRODUCT);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        PeriodCancelledEvent.class,
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private Integer getAdjustmentItemId() {
        String itemId = getParameters().get(PARAM_ADJUSTMENT_PRODUCT.getName());
        if(itemId == null || itemId.isEmpty()) {
            throw new SessionInternalError("Please Enter adjustment_product_id for RefundOnCancelTask plugin ");
        }
        return Integer.valueOf(itemId);
    }

    private BigDecimal calculateSingleDayPrice(BigDecimal amount, int maxNoOfDays) {
        return amount.divide(new BigDecimal(maxNoOfDays), Constants.BIGDECIMAL_SCALE,
                Constants.BIGDECIMAL_ROUND);
    }

    private ResourceBundle getUserResourceBundle(UserDTO user) {
        try {
            return ResourceBundle.getBundle("entityNotifications", user.getLanguage().asLocale());
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new SessionInternalError("Error ", SwapPlanFUPTransferTask.class, e);
        }
    }

    private void addPriceAmountAndDescriptionOnLine(OrderLineDTO line, PeriodOfTime cycle, Integer languageId) {
        int noOfDays = cycle.getDaysInPeriod();
        int maxDaysOfMonth = DateConvertUtils.asLocalDate(cycle.getStart()).lengthOfMonth();
        BigDecimal calculatedPrice = calculateSingleDayPrice(line.getAmount(), maxDaysOfMonth).multiply(new BigDecimal(noOfDays));
        line.setPrice(calculatedPrice.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
        line.setAmount(line.getPrice().multiply(line.getQuantity()));
        line.setUseItem(Boolean.FALSE);
        line.setTypeId(Constants.ORDER_LINE_TYPE_ADJUSTMENT);
        ItemDTO item = new ItemDAS().findNow(line.getItemId());
        line.setDescription(item.getDescription(languageId));
    }

    private OrderLineDTO createNewRefundLine(OrderDTO order, Integer languageId, PeriodOfTime cycle, BigDecimal amount) {
        OrderLineDTO newLine = new OrderLineDTO();
        // reset so it gets inserted
        newLine.setId(0);
        newLine.setVersionNum(null);
        newLine.setPurchaseOrder(order);
        newLine.getAssets().clear();
        newLine.setItemId(getAdjustmentItemId());
        newLine.setCreateDatetime(TimezoneHelper.companyCurrentDate(getEntityId()));
        // make the order negative (refund/credit)
        newLine.setQuantity(BigDecimal.ONE);
        newLine.setAmount(amount.negate());
        addPriceAmountAndDescriptionOnLine(newLine, cycle, languageId);
        return newLine;
    }

    private void addExtraItemOnOrderFromParameters(OrderDTO order, Integer entityId, Integer languageId) {
        // add extra lines with items from the parameters
        for (Entry<String, String> paramEntry : parameters.entrySet()) {
            boolean skipParameter = false;
            String name = paramEntry.getKey();
            if (!name.startsWith("item")) {
                logger.warn("parameter is not an item: {}", name);
                skipParameter = true;
            }

            int itemId = Integer.parseInt(parameters.get(name));
            logger.debug("adding item {} to new order", itemId);
            ItemDTO item = new ItemDAS().findNow(itemId);
            if (item == null || getEntityId().equals(entityId)) {
                logger.error("Item {} not found", item);
                skipParameter = true;
            }

            if(skipParameter) {
                continue;
            }
            OrderLineDTO newLine = new OrderLineDTO();
            newLine.setDeleted(0);

            newLine.setDescription(item.getDescription(languageId));
            newLine.setItem(item);
            newLine.setOrderLineType(new OrderLineTypeDAS().find(Constants.ORDER_LINE_TYPE_ITEM));
            newLine.setQuantity(1);
            newLine.setPurchaseOrder(order);

            try {
                newLine.setPrice(new ItemBL(itemId).getPrice(order.getUserId(), newLine.getQuantity(), entityId));
            } catch (Exception e) {
                throw new SessionInternalError("Error when doing credit", RefundOnCancelTask.class, e);
            }

            order.getLines().add(newLine);
        }
    }

    @Override
    public void process(Event event) {
        OrderDTO order = ((PeriodCancelledEvent) event).getOrder();
        logger.debug("Plug in processing period cancelled event for order {}", order.getId());
        // local variables
        Integer userId = new OrderDAS().find(order.getId()).getBaseUserByUserId().getUserId(); // the order might not be in the session
        Integer entityId = event.getEntityId();
        UserBL userBL = new UserBL(userId);
        ResourceBundle bundle = getUserResourceBundle(userBL.getEntity());

        Integer languageId = userBL.getEntity().getLanguageIdField();
        // Order is non pro-rated then no need to create an order
        if(!order.getProrateFlagValue()) {
            return;
        }
        Optional<OrderDTO> refundOrder = findRefundOrderFromSubscriptionOrder(order);
        // create a new order that is the same as the original one, but all
        // negative prices
        OrderDTO newOrder = null;
        if(refundOrder.isPresent()) {
            newOrder = refundOrder.get();
            OrderLineDAS olDAS = new OrderLineDAS();
            for(OrderLineDTO line : newOrder.getLines()) {
                olDAS.delete(line);
            }
        } else {
            newOrder = new OrderDTO(order);
            newOrder.setCreateDate(TimezoneHelper.serverCurrentDate());
            // reset the ids, so it is a new order
            newOrder.setId(null);
            newOrder.setVersionNum(null);
            OrderPeriodDTO period = new OrderPeriodDTO();
            period.setId(Constants.ORDER_PERIOD_ONCE);
            newOrder.setOrderPeriod(period);
            newOrder.setProrateFlag(false);

            OrderBillingTypeDTO type = new OrderBillingTypeDTO();
            type.setId(Constants.ORDER_BILLING_POST_PAID);
            newOrder.setOrderBillingType(type);
            newOrder.setParentOrder(null);
        }

        newOrder.getChildOrders().clear(); // no child order need to add on new order from order.
        newOrder.getOrderProcesses().clear(); // no invoices created for a new order.
        newOrder.getDiscountLines().clear(); // clear discount lines from a new order.
        newOrder.getLines().clear();
        // starts where the cancellation starts
        Calendar activeSinceDate = Calendar.getInstance();
        activeSinceDate.setTime(order.getActiveUntil()!=null ?
                order.getActiveUntil() : TimezoneHelper.companyCurrentDate(entityId));
        activeSinceDate.add(Calendar.DATE, 1);

        newOrder.setActiveSince(activeSinceDate.getTime());
        newOrder.setActiveUntil(null);
        newOrder.setNextBillableDay(null);
        // add some clarification notes
        newOrder.setNotes(bundle.getString("order.credit.notes") + " " + order.getId());
        newOrder.setNotesInInvoice(0);
        //
        // order lines:
        //
        Calendar activeUntilDate = Calendar.getInstance();
        activeUntilDate.setTime(order.getNextBillableDay());
        PeriodOfTime cycle = new PeriodOfTime(newOrder.getActiveSince(), activeUntilDate.getTime(), 0);
        if(cycle.getDaysInPeriod() == 0 ) {
            return ;
        }

        // creating refund line
        newOrder.getLines().add(createNewRefundLine(newOrder, languageId, cycle, order.getTotal()));

        addExtraItemOnOrderFromParameters(newOrder, entityId, languageId);
        // do the maths
        OrderBL orderBL = new OrderBL(newOrder);
        try {
            orderBL.recalculate(entityId);
        } catch (ItemDecimalsException e) {
            throw new SessionInternalError("Error when doing credit", RefundOnCancelTask.class, e);
        }

        if(!refundOrder.isPresent()) {
            // save
            Integer newOrderId = orderBL.create(entityId, null, newOrder);

            // audit so we know why all these changes happened
            new EventLogger().auditBySystem(entityId, userId,
                    Constants.TABLE_PUCHASE_ORDER, order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE, EventLogger.ORDER_CANCEL_AND_CREDIT,
                    newOrderId, null, null);

            //
            // Update original order
            //
            newOrder = new OrderDAS().find(newOrderId);
            newOrder.setParentOrder(order);

            order.setNotes(order.getNotes() + " - " +
                    bundle.getString("order.cancelled.notes") + " " + newOrderId);

            logger.debug("Credit done with new order {}", newOrderId);
        } else {
            logger.debug("Credit Order Updated with new amount {}", newOrder.getTotal());
        }

    }

    private Optional<OrderDTO> findRefundOrderFromSubscriptionOrder(OrderDTO subscriptionOrder) {
        for(OrderDTO order : subscriptionOrder.getChildOrders()) {
            if (order.getDeleted() == 0) {
                for(OrderLineDTO line : order.getLines()) {
                    if (line.getDeleted() == 0) {
                        Integer itemId = line.getItemId();
                        if(null!= itemId && itemId.equals(getAdjustmentItemId())) {
                            return Optional.of(order);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "RefundOnCancelTask for events " + Arrays.toString(events);
    }


}
