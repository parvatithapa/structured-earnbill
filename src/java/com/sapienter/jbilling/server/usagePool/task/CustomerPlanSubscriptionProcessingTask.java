/*
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

package com.sapienter.jbilling.server.usagePool.task;

import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomerPlanSubscriptionProcessingTask
 * This is an internal events task that subscribes to CustomerPlanSubscriptionEvent.
 * When a customer subscribes to plan, this task creates the customer usage pool
 * association for all usage pools attached on the plan.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */
public class CustomerPlanSubscriptionProcessingTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        CustomerPlanSubscriptionEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    /**
     * This method creates the customer usage pool associations for all the
     * usage pools attached to the plan. The plan is obtained from the plan order
     * through which customer is subscribing to the plan.
     */
    @Override
    public void process(Event event) throws PluggableTaskException {

        logger.debug("Entering Customer Plan Subscription process - event: {}", event);

        CustomerPlanSubscriptionEvent customerPlanSubEvent = (CustomerPlanSubscriptionEvent) event;

        Integer customerId = customerPlanSubEvent.getOrder().getBaseUserByUserId().getCustomer().getId();
        PlanDTO plan = customerPlanSubEvent.getPlan();

        OrderDTO subScriptionOrder = customerPlanSubEvent.getOrder();
        Date startDate;
        try {
            startDate = new BasicOrderPeriodTask().calculateStart(customerPlanSubEvent.getOrder());
        } catch (TaskException e) {
            throw new PluggableTaskException(e);
        }

        if(subScriptionOrder.getNextBillableDay() != null &&
                Constants.ORDER_BILLING_PRE_PAID.equals(subScriptionOrder.getBillingTypeId())) {
            MainSubscriptionDTO mainSubscription = subScriptionOrder.getUser().getCustomer().getMainSubscription();
            OrderPeriodDTO orderPeriodDTO = mainSubscription.getSubscriptionPeriod();
            int periodUnitId = orderPeriodDTO.getUnitId();
            int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
            startDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(startDate),
                    orderPeriodDTO.getValue() * -1L));
        }
        CustomerUsagePoolBL customerUsagePoolBl = new CustomerUsagePoolBL();
        for (UsagePoolDTO usagePool: plan.getUsagePools()) {
            CustomerUsagePoolDTO customerUsagePool = customerUsagePoolBl.getCreateCustomerUsagePoolDto(usagePool.getId(),
                    customerId,
                    startDate,
                    customerPlanSubEvent.getOrder().getOrderPeriod(),
                    plan,
                    customerPlanSubEvent.getOrder().getActiveUntil(),
                    customerPlanSubEvent.getOrder().getCreateDate(),
                    customerPlanSubEvent.getOrder());

            customerUsagePoolBl.createOrUpdateCustomerUsagePool(customerUsagePool);
        }
        UsageOrderReRater.reRateUsageOrder(getEntityId(), subScriptionOrder);
        logger.debug("Customer Plan Subscription process");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
