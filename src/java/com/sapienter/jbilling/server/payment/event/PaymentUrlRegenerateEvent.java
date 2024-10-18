/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.system.event.Event;

public class PaymentUrlRegenerateEvent implements Event {
    private final Integer invoiceId;
    private final Integer entityId;

    public PaymentUrlRegenerateEvent(Integer invoiceId, Integer entityId) {
        this.invoiceId = invoiceId;
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Payment Url Regenerate";
    }

    public final Integer getInvoiceId() {
        return invoiceId;
    }

}
