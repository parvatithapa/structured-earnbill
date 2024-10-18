package com.sapienter.jbilling.server.system.event;

/**
 * Created by marcolin on 29/10/15.
 */
public enum Events {
    PaymentSuccessfulEvent,
    OrderDeletedEvent,
    NewOrderEvent,
    PaymentDeletedEvent,
    OrderAddedOnInvoiceEvent,
    NewQuantityEvent,
    NewPriceEvent,
    ProcessTaxLineOnInvoiceEvent,
    ReservationCreatedEvent,
    ReservationReleasedEvent,
    InvoiceDeletedEvent;
    private Class<? extends Event> implementation;


    public void setImplementation(Class<? extends Event> implementation) {
        this.implementation = implementation;
    }

    public Class<Event> getImplementation() {
        return (Class<Event>) implementation;
    }
}