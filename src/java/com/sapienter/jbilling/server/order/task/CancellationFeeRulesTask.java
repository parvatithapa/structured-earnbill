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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.tasks.RulesItemManager;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ResourceBundle;

@Deprecated
public class CancellationFeeRulesTask extends RulesItemManager implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CancellationFeeRulesTask.class));

    private enum EventType { NEW_ACTIVE_UNTIL_EVENT, NEW_QUANTITY_EVENT } 

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            NewActiveUntilEvent.class,
            NewQuantityEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        EventType eventType;
        OrderDTO order = null;

        // validate the type of the event
        if (event instanceof NewActiveUntilEvent) {
            NewActiveUntilEvent myEvent = (NewActiveUntilEvent) event;

            // if the new active until is later than the old one
            // or the new one is null
            // don't process
            if (myEvent.getNewActiveUntil() == null
                    || (myEvent.getOldActiveUntil() != null && !myEvent.getNewActiveUntil().before(
                            myEvent.getOldActiveUntil()))) {
                LOG
                        .debug("New active until is not earlier than old one. Skipping cancellation fees. "
                                + "Order id" + myEvent.getOrderId());
                return;
            }

            order = new OrderDAS().find(myEvent.getOrderId());
            eventType = EventType.NEW_ACTIVE_UNTIL_EVENT;
        } else if (event instanceof NewQuantityEvent) {
            NewQuantityEvent myEvent = (NewQuantityEvent) event;
            // don't process if new quantity has increased instead of decreased
            if (myEvent.getNewQuantity().compareTo(myEvent.getOldQuantity()) > 0) {
                    return;
            }

            // Create a copy of the order that had a line quantity changed
            // and add the changed line (with cancelled quantity) to it.
            OrderDTO changedOrder = new OrderDAS().find(myEvent.getOrderId());
            order = new OrderDTO(changedOrder);
            // clear the order lines
            order.getLines().clear();
            // add the changed line
            OrderLineDTO line = new OrderLineDTO(myEvent.getOrderLine());
            line.setPurchaseOrder(order);
            order.getLines().add(line);

            // set quantity as the difference between the old and new quantities
            BigDecimal quantity = myEvent.getOldQuantity().subtract(myEvent.getNewQuantity());
            line.setQuantity(quantity);

            eventType = EventType.NEW_QUANTITY_EVENT;
        } else {
            throw new SessionInternalError("Can't process anything but a new active until event");
        }

        LOG.debug("Processing event " + event + " for cancellation fee");

        helperOrder = new FeeOrderManager(order, order.getBaseUserByUserId().getLanguage().getId(),
                order.getBaseUserByUserId().getUserId(), order.getBaseUserByUserId().getEntity()
                        .getId(), order.getBaseUserByUserId().getCurrency().getId());

        if (event != null && eventType == EventType.NEW_ACTIVE_UNTIL_EVENT) {
            NewActiveUntilEvent myEvent = (NewActiveUntilEvent) event;
            ((FeeOrderManager) helperOrder).setNewActiveUntil(myEvent.getNewActiveUntil());
            ((FeeOrderManager) helperOrder).setOldActiveUntil(myEvent.getOldActiveUntil());
        } else if (eventType == EventType.NEW_QUANTITY_EVENT) {
            // default to now. This is needed to calculate the number of periods cancelled
            ((FeeOrderManager) helperOrder).setNewActiveUntil(companyCurrentDate());
            ((FeeOrderManager) helperOrder).setOldActiveUntil(order.getActiveUntil());
        }

        try {
            processRules(order);
        } catch (TaskException e) {
            throw new SessionInternalError("Exception processing rules for cancellation fee",
                    CancellationFeeRulesTask.class, e);
        }

    }

    public class FeeOrderManager extends OrderManager {

        private Date newActiveUntil = null;
        private Date oldActiveUntil = null;

        public FeeOrderManager(OrderDTO order, Integer language, Integer userId, Integer entityId, Integer currencyId) {
            super(order, language, userId, entityId, currencyId);
        }

        public void applyFee(Integer itemId, Double quantity, Integer daysInPeriod) {
            BigDecimal qty = new BigDecimal(quantity).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
            applyFee(itemId, qty, daysInPeriod);
        }

        // all the methods from OrderManager are actually unnecesary for this
        // helper but it is an instance or OrderManager that makes it into the working
        // memory
        public void applyFee(Integer itemId, BigDecimal quantity, Integer daysInPeriod) {
            ResourceBundle bundle;
            UserBL userBL;
            try {
                userBL = new UserBL(getOrder().getBaseUserByUserId().getId());
                bundle = ResourceBundle.getBundle("entityNotifications", userBL.getLocale());
                } catch (Exception e) {
                throw new SessionInternalError("Error when doing credit", RefundOnCancelTask.class, e);
            }

            BigDecimal periods;
            // calculate the number of periods that have been cancelled
            if (oldActiveUntil == null) {
                periods = new BigDecimal(1);
                LOG.info("Old active until not present. Period will be 1.");
            } else {
                long totalMills = oldActiveUntil.getTime() - newActiveUntil.getTime();
                BigDecimal periodMills = new BigDecimal(daysInPeriod)
                        .multiply(new BigDecimal(24))
                        .multiply(new BigDecimal(60))
                        .multiply(new BigDecimal(60))
                        .multiply(new BigDecimal(1000));

                BigDecimal calcPeriods = new BigDecimal(totalMills)
                        .divide(periodMills, Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);

                periods = new BigDecimal(calcPeriods.intValue()); // we do not want to charge for fractions            
            }
            
            if (BigDecimal.ZERO.equals(periods)) {
                LOG.debug("No a single compelte period cancelled: " + oldActiveUntil + " " + newActiveUntil);
                return;
            }

            if (quantity == null) {
                quantity = new BigDecimal(1);
            }
            quantity = quantity.multiply(periods);

            // now create a new order for the fee:
            // - one time
            // - item from the parameter * number of periods being cancelled
            OrderDTO feeOrder = new OrderDTO();
            feeOrder.setBaseUserByUserId(getOrder().getBaseUserByUserId());
            feeOrder.setCurrency(getOrder().getCurrency());
            feeOrder.setNotes(bundle.getString("order.cancelationFee.notes") + " " + getOrder().getId());
            feeOrder.setOrderPeriod(new OrderPeriodDAS().find(Constants.ORDER_PERIOD_ONCE));
            // now the line
            ItemDTO item = new ItemDAS().find(itemId);
            OrderLineDTO line = new OrderLineDTO();
            line.setDeleted(0);
            line.setDescription(item.getDescription(userBL.getEntity().getLanguageIdField()));
            line.setItem(item);
            line.setOrderLineType(new OrderLineTypeDAS().find(Constants.ORDER_LINE_TYPE_ITEM));
            line.setPurchaseOrder(feeOrder);
            feeOrder.getLines().add(line);
            line.setQuantity(quantity);
            
            ItemBL itemBL = new ItemBL(itemId);
            line.setPrice(itemBL.getPrice(getOrder().getBaseUserByUserId().getId(),
                                          getOrder().getCurrencyId(),
                                          quantity,
                                          getEntityId()));
            
            OrderBL orderBL = new OrderBL(feeOrder);
            try {
                orderBL.recalculate(getEntityId());
            } catch (ItemDecimalsException e) {
                throw new SessionInternalError(e);
            }
            Integer feeOrderId = orderBL.create(getEntityId(), null, feeOrder);
            LOG.debug("New fee order created: " + feeOrderId + " for cancel of " + getOrder().getId());
        }
        
        // convenience method for 30 days, which is the typical period of time (month) to 
        // calculate fees
        public void applyFee(Integer itemId, Double quantity) {
            applyFee(itemId, quantity, 30);
        }

        public void applyFee(Integer itemId, BigDecimal quantity) {
            applyFee(itemId, quantity, 30);
        }

        // convenience method for cancellation fee quantity of 1 and period of 30 days
        public void applyFee(Integer itemId) {
            applyFee(itemId, 1.0, 30);
        }

        public Date getNewActiveUntil() {
            return newActiveUntil;
        }

        public void setNewActiveUntil(Date activeSince) {
            this.newActiveUntil = activeSince;
        }

        public Date getOldActiveUntil() {
            return oldActiveUntil;
        }

        public void setOldActiveUntil(Date activeUntil) {
            this.oldActiveUntil = activeUntil;
        }
    }
}
