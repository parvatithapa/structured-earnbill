package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.system.event.Event;

import java.util.Map;

/**
 * Created by Taimoor Choudhary on 3/14/18.
 */
public class IgnitionPaymentEvent implements Event, AutoCloseable  {

    private final PaymentWS paymentWS;
    private final Integer entityId;
    private final Integer orderId;
    private Map<String, Map<String, ServiceProfile>> allServiceProfiles;

    @Override
    public String getName() {
        return "Ignition Custom Payment Event";
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

    public Map<String, Map<String, ServiceProfile>> getAllServiceProfiles() {
        return allServiceProfiles;
    }

    public IgnitionPaymentEvent(PaymentWS paymentWS, Integer entityId, Integer orderId, Map<String, Map<String, ServiceProfile>> allServiceProfiles) {
        this.paymentWS = paymentWS;
        this.entityId = entityId;
        this.orderId = orderId;
        this.allServiceProfiles = allServiceProfiles;
    }

    @Override
    public void close() throws Exception {
        if(paymentWS != null) {
            paymentWS.close();
        }
    }
}
