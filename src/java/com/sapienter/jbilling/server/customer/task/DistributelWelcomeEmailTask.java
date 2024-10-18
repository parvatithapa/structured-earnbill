package com.sapienter.jbilling.server.customer.task;

import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.event.DistributelNewCustomerEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.spa.ServiceType;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pablo_galera on 04/02/2017.
 */
public class DistributelWelcomeEmailTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAMETER_DSL_CUSTOMER_WELCOME_NOTIFICATION_ID =
            new ParameterDescription("dsl_customer_welcome_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_CABLE_CUSTOMER_WELCOME_NOTIFICATION_ID =
            new ParameterDescription("cable_customer_welcome_notification_id", false, ParameterDescription.Type.INT);

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_DSL_CUSTOMER_WELCOME_NOTIFICATION_ID);
        descriptions.add(PARAMETER_CABLE_CUSTOMER_WELCOME_NOTIFICATION_ID);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            DistributelNewCustomerEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        if (event instanceof DistributelNewCustomerEvent) {
            fireNewCustomerEventNotification((DistributelNewCustomerEvent) event, notificationSession);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private boolean fireNewCustomerEventNotification(DistributelNewCustomerEvent distributelNewCustomerEvent, INotificationSessionBean notificationSession) {
        String serviceTypeNotificationId = null;
        String serviceType = distributelNewCustomerEvent.getParameters().get(ServiceType.class.getName());

        if (ServiceType.DSL.name().equals(serviceType)) {
                serviceTypeNotificationId = PARAMETER_DSL_CUSTOMER_WELCOME_NOTIFICATION_ID.getName();
        } else if (ServiceType.CABLE.name().equals(serviceType)) {
            serviceTypeNotificationId = PARAMETER_CABLE_CUSTOMER_WELCOME_NOTIFICATION_ID.getName();
        }

        if (serviceTypeNotificationId == null || distributelNewCustomerEvent.getUser() == null) {
            return false;
        }
        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(serviceTypeNotificationId));

        MessageDTO message = null;
        Integer userId = distributelNewCustomerEvent.getUser().getId();
        try {
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    distributelNewCustomerEvent.getEntityId(),
                    userId,
                    distributelNewCustomerEvent.getUser().getLanguageIdField());

        } catch (NotificationNotFoundException e) {
            logger.debug("Custom notification id: {} does not exist for the user id {} ",
                    notificationMessageTypeId, userId);
        }
        if (message == null) {
            return false;
        }

        for (String propertyKey : distributelNewCustomerEvent.getParameters().keySet()) {
            message.addParameter(propertyKey, distributelNewCustomerEvent.getParameters().get(propertyKey));
        }

        logger.debug("Notifying user: {} for a {} new customer event", userId, serviceType);
        notificationSession.notify(userId, message);
        return true;
    }


}