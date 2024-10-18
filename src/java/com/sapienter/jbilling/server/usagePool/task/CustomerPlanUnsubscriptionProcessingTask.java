/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanUnsubscriptionEvent;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;

/**
 * CustomerPlanUnsubscriptionProcessingTask
 * This is an internal events task that subscribes to CustomerPlanUnsubscriptionEvent.
 * When a customer unsubscribes from a plan, this task updates the customer usage pool
 * to set the cycle end date as today's date in case of deletion of plan order or as
 * active until date in case of update to the order with active until date.
 * @author Amol Gadre
 * @since 23-Jan-2014
 */

public class CustomerPlanUnsubscriptionProcessingTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        CustomerPlanUnsubscriptionEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    /**
     * This method updates the customer usage pools for all the
     * usage pools attached to the plan being unsubscribed.
     */
    @Override
    public void process(Event event) throws PluggableTaskException {

        logger.debug("Entering Customer Plan Unsubscription processing - event: {} ", event);

        CustomerPlanUnsubscriptionEvent customerPlanUnsubribeEvent = (CustomerPlanUnsubscriptionEvent) event;

        Integer orderId = customerPlanUnsubribeEvent.getOrder().getId();
        OrderDTO subscriptionOrder = customerPlanUnsubribeEvent.getOrder();
        String action = customerPlanUnsubribeEvent.getTriggeringAction();

        // we need to update the cycle end date to today's date if the plan order is deleted.
        // if the active until is updated on plan order, the cycle end date = active until

        List<CustomerUsagePoolDTO> customerUsagePools = new CustomerUsagePoolDAS().getCustomerUsagePoolsByOrderId(orderId);
        for (CustomerUsagePoolDTO customerUsagePool : customerUsagePools) {
            CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
            Date cycleEndDate = Util.getEpochDate();
            Date cycleStartDate = Util.getEpochDate();
            if (Constants.CUSTOMER_PLAN_UNSUBSCRIBE_UPDATE_ACTIVE_UNTIL.
                    equals(action)) {
                OrderDTO newOrder = customerPlanUnsubribeEvent.getOrder();
                Date newActiveSince;

                try {
                    newActiveSince = new BasicOrderPeriodTask().calculateStart(newOrder);
                } catch (Exception e) {
                    throw new SessionInternalError("Error occurs in CustomerPlanUnsubscriptionProcessingTask.calculateStart", CustomerPlanUnsubscriptionProcessingTask.class, e);
                }

                cycleStartDate = newActiveSince;
                cycleEndDate = bl.getCycleEndDateForPeriod(customerUsagePool.getUsagePool().getCyclePeriodUnit(),
                        customerUsagePool.getUsagePool().getCyclePeriodValue(), newActiveSince, newOrder.getOrderPeriod(), newOrder.getActiveUntil());
            }

            logger.debug("cycleEndDate: {}", cycleEndDate);

            boolean isPlanRemove = Constants.CUSTOMER_PLAN_UNSUBSCRIBE_PLAN_REMOVE.equals(action) ||
                    Constants.CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_DELETE.equals(action);

            boolean isPrePaid = Constants.ORDER_BILLING_PRE_PAID.equals(subscriptionOrder.getBillingTypeId());
            for (OrderLineDTO line : subscriptionOrder.getLines()) {
                if (null != line.getItem() && line.getItem().hasPlans()
                        && line.getPurchaseOrder().getUser().getCustomer().equals(customerUsagePool.getCustomer())
                        && (isPlanRemove || (!isPrePaid && Constants.CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_FINISHED.equals(action)))
                        && !customerPlanUnsubribeEvent.isPlanSwap()) {
                    // this line contains a plan
                    customerUsagePool.setCycleStartDate(cycleStartDate);
                    customerUsagePool.setCycleEndDate(cycleEndDate);
                    bl.createOrUpdateCustomerUsagePool(customerUsagePool);
                }
            }

        }

        if(!customerPlanUnsubribeEvent.isPlanSwap() &&
                Constants.CUSTOMER_PLAN_UNSUBSCRIBE_PLAN_REMOVE.equals(action)) {
            UsageOrderReRater.reRateUsageOrder(getEntityId(), subscriptionOrder);
        }

    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
