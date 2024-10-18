package com.sapienter.jbilling.server.system.event;

import com.sapienter.jbilling.server.diameter.event.ReservationCreatedEvent;
import com.sapienter.jbilling.server.diameter.event.ReservationReleasedEvent;
import com.sapienter.jbilling.server.order.event.*;
import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by marcolin on 29/10/15.
 */
public class EventServiceImpl implements EventService {

    static {
        Events.PaymentSuccessfulEvent.setImplementation(PaymentSuccessfulEvent.class);
        Events.OrderDeletedEvent.setImplementation(OrderDeletedEvent.class);
        Events.NewOrderEvent.setImplementation(NewOrderEvent.class);
        Events.PaymentDeletedEvent.setImplementation(PaymentDeletedEvent.class);
        Events.OrderAddedOnInvoiceEvent.setImplementation(OrderAddedOnInvoiceEvent.class);
        Events.NewQuantityEvent.setImplementation(NewQuantityEvent.class);
        Events.NewPriceEvent.setImplementation(NewPriceEvent.class);
        Events.ProcessTaxLineOnInvoiceEvent.setImplementation(ProcessTaxLineOnInvoiceEvent.class);
        Events.ReservationCreatedEvent.setImplementation(ReservationCreatedEvent.class);
        Events.ReservationReleasedEvent.setImplementation(ReservationReleasedEvent.class);
        Events.InvoiceDeletedEvent.setImplementation(InvoiceDeletedEvent.class);
    }

    @Override
    public Class<Event>[] retrieveServices(Events... events) {
        return Arrays.asList(events).stream()
                .map(e -> e.getImplementation()).collect(Collectors.toList())
                .toArray(new Class[0]);
    }
}
