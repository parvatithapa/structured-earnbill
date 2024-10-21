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

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;

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
    //This parameter is used to prorate FUP for subscription order.
    private static final ParameterDescription PRORATE_FIRST =
            new ParameterDescription("Prorate Usage Pool Quantity for First Period?", false, ParameterDescription.Type.BOOLEAN);

    //This parameter decides to rerate mediated usage order or not.
    private static final ParameterDescription RERATE_MEDIATED_ORDER =
            new ParameterDescription("Should rerate mediated order?", false, ParameterDescription.Type.BOOLEAN, "true");

    private static final boolean DEFAULT_PRORATE_FIRST = true;

    {
        descriptions.add(PRORATE_FIRST);
        descriptions.add(RERATE_MEDIATED_ORDER);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
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

        CustomerPlanSubscriptionEvent customerPlanSubscriptionEvent = (CustomerPlanSubscriptionEvent) event;
        //creating customer usage pool.
        CustomerPlanSubscriptionHelper.createCustomerUsagePool(customerPlanSubscriptionEvent,
                getParameter(PRORATE_FIRST.getName(), DEFAULT_PRORATE_FIRST));

        //Rerate usage order based on plun-in parameter value
        if (getParameter(RERATE_MEDIATED_ORDER.getName(), Boolean.parseBoolean(RERATE_MEDIATED_ORDER.getDefaultValue()))) {
            UsageOrderReRater.reRateUsageOrder(getEntityId(), customerPlanSubscriptionEvent.getOrder());
        }
        logger.debug("Customer Plan Subscription process");
    }

}
