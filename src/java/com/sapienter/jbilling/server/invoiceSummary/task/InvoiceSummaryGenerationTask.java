/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.invoiceSummary.task;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryBL;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/**
 * This task will generate invoices summary for new invoice, To populate all parameters
 * it will considering the periods in between 2 invoices (current invoice and last invoice)
 * @author Ashok Kale Created on 11-Jan-2017
 */
public class InvoiceSummaryGenerationTask extends PluggableTask implements IInternalEventsTask {


    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        InvoicesGeneratedEvent.class,
        InvoiceDeletedEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    @Override
    public void process (Event event) throws PluggableTaskException {
        InvoiceSummaryBL invoiceSummaryBL = new InvoiceSummaryBL();
        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;

            for(Integer invoiceId: instantiatedEvent.getInvoiceIds()){
                invoiceSummaryBL.deleteByInvoice(invoiceId);
                createInvoiceSummary(invoiceId);
            }
        } else if (event instanceof InvoiceDeletedEvent) {
            InvoiceDeletedEvent invoiceDeletedEvent = (InvoiceDeletedEvent) event;
            invoiceSummaryBL.deleteByInvoice(invoiceDeletedEvent.getInvoice().getId());
        } else {
            throw new PluggableTaskException("Unknown event: " + event.getClass());
        }
    }

    /**
     * Create Invoice summary for generated invoice
     * Invoice summary will not generate for review and zero amount & balance invoices
     * @param invoiceId
     */
    private void createInvoiceSummary(Integer invoiceId) {
        InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();
        if (invoiceDTO.isCreditInvoice() && null == invoiceDTO.getBillingProcess()) {
            logger.debug("Invoice summary will not generate for review status invoice and zero amount and balance invoice (Credit Invoice) "
                    + "except credit invoice generated through billing process");
            return;
        }
        logger.debug("Creating Invoice Summary for Invoice Id: {} ", invoiceId);
        Integer invoiceSummaryId = new InvoiceSummaryBL().create(invoiceDTO);
        logger.debug("Invoice Summary Generated: {}", invoiceSummaryId);
    }
}
