package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;

/**
 * Created by wajeeha on 9/13/17.
 */
public class IgnitionPaymentFailedEvent extends AbstractPaymentEvent  {

    private String fileType= null;

    public IgnitionPaymentFailedEvent(Integer entityId, PaymentDTOEx payment, String fileType) {
        super(entityId, payment);
        this.fileType = fileType;
    }

    @Override
    public String getName() {
        return "Ignition Payment Failed Event";
    }

    public String getFileType() {
        return fileType;
    }

}
