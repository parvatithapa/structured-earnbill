package com.sapienter.jbilling.server.usagePool.task;

import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import grails.plugin.springsecurity.SpringSecurityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.event.FreeTrialConsumptionEvent;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class FreeTrialConsumptionTask extends PluggableTask implements IInternalEventsTask {

    Logger logger = LoggerFactory.getLogger(FreeTrialConsumptionTask.class);
    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        FreeTrialConsumptionEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("processing free trial consumption event!");
        if (!(event instanceof FreeTrialConsumptionEvent))
            throw new PluggableTaskException("Cannot process event " + event);
        FreeTrialConsumptionEvent freeTrialConsumptionEvent = (FreeTrialConsumptionEvent) event;
        Integer userId = freeTrialConsumptionEvent.getUserId();
        Integer entityId = freeTrialConsumptionEvent.getEntityId();
        updateActiveUntilAndGenerateInvoiceForFreeTrialOrders(userId, entityId, new Date());

    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * Helper method to update the Active Until date and generate invoice for free trial orders
     * when the free usage consumption is 100% reached
     *
     * @param userId
     * @param date
     */
    private void updateActiveUntilAndGenerateInvoiceForFreeTrialOrders(Integer userId, Integer entityId, Date date) {
        logger.debug("Updating active until date and generating invoice for free trial order for user {}", userId);
        try {
            IWebServicesSessionBean service = Context.getBean(Name.WEB_SERVICES_SESSION);
            UserDTO user = new UserDAS().findByUserId(userId, entityId);
            SpringSecurityUtils.reauthenticate(user.getUserName()+";"+entityId, null);

            new OrderDAS().findByUserSubscriptionsAndFreeTrialSubscription(userId).stream()
            .forEach(order -> {
                order.setActiveUntil(date);
                OrderBL bl = new OrderBL(order);
                bl.updateActiveUntil(null, date, order);

                Integer[] invoiceIds = service.createInvoice(userId, false);
                Arrays.asList(invoiceIds).stream().forEach(service::notifyInvoiceByEmail);
            });

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }finally {
            // logout user once api calls are done.
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

}
