package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

public class PopulateOrderLevelMetaFieldsTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME =
            new ParameterDescription("Subscription order id meta field name", true, ParameterDescription.Type.STR);

    public static final ParameterDescription PARAM_SERVICE_ID_MF_NAME =
            new ParameterDescription("Service id meta field name", true, ParameterDescription.Type.STR);

    public PopulateOrderLevelMetaFieldsTask() {
        descriptions.add(PARAM_SERVICE_ID_MF_NAME);
        descriptions.add(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        NewOrderEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("processing event {} for entity {}", event, getEntityId());
        try {
            if (!(event instanceof NewOrderEvent)) {
                throw new PluggableTaskException("Cannot process event " + event);
            }
            NewOrderEvent newOrderEvent = (NewOrderEvent) event;
            OrderDTO newOrder = new OrderDAS().find(newOrderEvent.getOrder().getId());
            String subscriptionOrderIdMFName = getMandatoryStringParameter(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME.getName());
            String serviceIdMFName = getMandatoryStringParameter(PARAM_SERVICE_ID_MF_NAME.getName());
            @SuppressWarnings("unchecked")
            MetaFieldValue subscriptionOrderIdMetaFieldValue = newOrder.getMetaField(subscriptionOrderIdMFName);
            if(null == subscriptionOrderIdMetaFieldValue || subscriptionOrderIdMetaFieldValue.isEmpty()) {
                logger.debug("Subscription order id metafield is not set to the new order");
                String serviceId = getServiceNumberFromOrder(newOrder, serviceIdMFName);
                logger.debug("new order's service id : {}", serviceId);
                if(null != serviceId) {
                    List<Integer> subscriptionOrderIds = new OrderDAS().findSubscriptionOrderIdsByUserIdentifierAndEffectiveDate(newOrder.getUserId(),
                            serviceId, newOrder.getActiveSince());
                    if(subscriptionOrderIds.isEmpty()) {
                        logger.debug("can't set subscription order id because no subscription order found");
                    } else if(subscriptionOrderIds.size() > 1) {
                        logger.debug("can't set subscription order id because multiple subscription orders found:{}", subscriptionOrderIds);
                    } else {
                        logger.debug("setting subscription order id {} metafield to the new order", subscriptionOrderIds.get(0));
                        newOrder.setMetaField(getEntityId(), null, subscriptionOrderIdMFName, subscriptionOrderIds.get(0));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("SPC set subscription order id failed");
            throw new PluggableTaskException("error in processing", e);
        }
    }

    private String getServiceNumberFromOrder(OrderDTO order, String orderLineServiceNumberMfName) {
        logger.debug("getting service number if present from the order");
        for(OrderLineDTO line : order.getLines()) {
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> orderLineServiceNumber = line.getMetaField(orderLineServiceNumberMfName);
            if(null != orderLineServiceNumber && StringUtils.isNotBlank(orderLineServiceNumber.getValue())) {
                return orderLineServiceNumber.getValue();
            }
        }
        return null;
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}

