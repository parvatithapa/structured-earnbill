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
package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUnlinkedFromInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

public class GeneratePaymentCommissionTask extends PluggableTask
        implements IInternalEventsTask {

    private static final Logger LOG =
            Logger.getLogger(GeneratePaymentCommissionTask.class);

    private static final Class<Event> events[] = new Class[]{
            PaymentLinkedToInvoiceEvent.class,
            PaymentUnlinkedFromInvoiceEvent.class
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process (Event event) throws PluggableTaskException {

        if (event instanceof PaymentLinkedToInvoiceEvent) {
            PaymentLinkedToInvoiceEvent instantiatedEvent = (PaymentLinkedToInvoiceEvent) event;
            createPaymentCommission(instantiatedEvent.getInvoice(), instantiatedEvent.getTotalPaid());
        } else if (event instanceof PaymentUnlinkedFromInvoiceEvent) {
            PaymentUnlinkedFromInvoiceEvent instantiatedEvent = (PaymentUnlinkedFromInvoiceEvent) event;
            createPaymentCommission(instantiatedEvent.getInvoice(), instantiatedEvent.getTotalPaid());
        } else {
            throw new PluggableTaskException("Unknown event: " +
                    event.getClass());
        }
    }

    private void createPaymentCommission(InvoiceDTO invoice, BigDecimal amount){
        if(!invoice.getBaseUser().getCustomer().getPartners().isEmpty()){
            PaymentCommissionDTO paymentCommission = new PaymentCommissionDTO();
            paymentCommission.setInvoice(invoice);
            paymentCommission.setPaymentAmount(amount);

            new PaymentCommissionDAS().save(paymentCommission);
        }
    }
}
