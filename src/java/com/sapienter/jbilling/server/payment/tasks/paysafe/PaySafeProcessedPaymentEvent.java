package com.sapienter.jbilling.server.payment.tasks.paysafe;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by Matías Cabezas on 08/11/17.
 */
public class PaySafeProcessedPaymentEvent implements Event {
    private PaySafeResultType result;
    private Integer entityId;
    private PaymentDTOEx paymentDTOEx;
    private boolean newSession;


    public PaySafeProcessedPaymentEvent(Integer entityId, PaySafeResultType result, PaymentDTOEx paymentDTOEx, boolean newSession) {
        this.entityId = entityId;
        this.result = result;
        this.paymentDTOEx = paymentDTOEx;
        this.newSession = newSession;
    }

    @Override
    public String getName() {
        return "PaySafe Notification for user " + paymentDTOEx.getUserId();
    }

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }

    public PaySafeResultType getResult() {
        return result;
    }

    public PaymentDTOEx getPaymentDTOEx() {
        return paymentDTOEx;
    }
    
    public boolean getIsNewSession() {
        return newSession;
    }
}
