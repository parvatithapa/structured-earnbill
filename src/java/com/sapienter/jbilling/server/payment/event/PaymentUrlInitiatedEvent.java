package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.system.event.Event;

public class PaymentUrlInitiatedEvent implements Event {
    private final Integer paymentUrlLogId;
    private final Integer entityId;

    public PaymentUrlInitiatedEvent(Integer paymentUrlLogId, Integer entityId) {
        this.paymentUrlLogId = paymentUrlLogId;
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Process Payment";
    }

    public final Integer getPaymentUrlLogId() {
        return paymentUrlLogId;
    }


}
