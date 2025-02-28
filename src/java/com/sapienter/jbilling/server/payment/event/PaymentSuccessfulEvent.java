/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;

public class PaymentSuccessfulEvent extends AbstractPaymentEvent {
    public PaymentSuccessfulEvent(Integer entityId, PaymentDTOEx payment) {
        this(entityId, payment, false);
    }

    public PaymentSuccessfulEvent(Integer entityId, PaymentDTOEx payment, boolean newEnrollment) {
        super(entityId, payment, newEnrollment);
    }

    public String getName() {
        return "Payment Successful";
    }

    @Override
    public String toString() {
        return "PaymentSuccessfulEvent{"
                + "paymentId=" + getPayment().getId()
                + ", amount=" + getPayment().getAmount()
                + "}";
    }
    
}
