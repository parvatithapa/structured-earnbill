package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;

/**
 * Created by wajeeha on 10/1/17.
 */
public class IgnitionPaymentSuccessfulEvent extends AbstractPaymentEvent {
    public IgnitionPaymentSuccessfulEvent(Integer entityId, PaymentDTOEx payment) {
        super(entityId, payment);
    }

    public String getName() {
        return "Ignition Payment Successful Event";
    }

    @Override
    public String toString() {
        return "PaymentSuccessfulEvent{"
                + "paymentId=" + getPayment().getId()
                + ", amount=" + getPayment().getAmount()
                + "}";
    }

}
