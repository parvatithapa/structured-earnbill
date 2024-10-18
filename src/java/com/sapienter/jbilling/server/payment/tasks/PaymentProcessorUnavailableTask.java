package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.server.payment.db.PaymentProcessorUnavailableDAS;
import com.sapienter.jbilling.server.payment.db.PaymentProcessorUnavailableDTO;
import com.sapienter.jbilling.server.payment.event.PaymentProcessorUnavailableEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/**
 * PaymentProcessorUnavailableTask class
 * 
 * This task listens when a payment has an unavailable result and create a payment processor unavailable 
 * 
 * @author Leandro Bagur
 * @since 23/11/17.
 */
public class PaymentProcessorUnavailableTask extends PluggableTask implements IInternalEventsTask {

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        PaymentProcessorUnavailableEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() { return events; }
    
    @Override
    public void process(Event event) throws PluggableTaskException {
        PaymentProcessorUnavailableEvent paymentProcessorUnavailableEvent = (PaymentProcessorUnavailableEvent) event; 
        PaymentProcessorUnavailableDTO paymentProcessorUnavailableDTO = new PaymentProcessorUnavailableDTO();
        paymentProcessorUnavailableDTO.setEntityId(paymentProcessorUnavailableEvent.getEntityId());
        paymentProcessorUnavailableDTO.setPaymentId(paymentProcessorUnavailableEvent.getPayment().getId());
        new PaymentProcessorUnavailableDAS().save(paymentProcessorUnavailableDTO);
    }
}
