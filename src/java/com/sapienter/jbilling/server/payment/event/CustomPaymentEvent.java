package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by taimoor on 8/5/17.
 */
public class CustomPaymentEvent implements Event, AutoCloseable  {

    private final PaymentWS paymentWS;
    private final Integer entityId;
    private final Integer orderId;

    @Override
    public String getName() {
        return "Custom Payment Event";
    }

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public PaymentWS getPaymentWS() {
        return paymentWS;
    }

    public CustomPaymentEvent(PaymentWS paymentWS, Integer entityId, Integer orderId) {
        this.paymentWS = paymentWS;
        this.entityId = entityId;
        this.orderId = orderId;
    }

    @Override
    public void close() throws Exception {
        if(paymentWS != null) {
            paymentWS.close();
        }
    }
}
