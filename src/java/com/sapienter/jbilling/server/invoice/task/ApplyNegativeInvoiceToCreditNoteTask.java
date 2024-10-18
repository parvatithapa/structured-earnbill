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
package com.sapienter.jbilling.server.invoice.task;

import java.math.BigDecimal;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.creditnote.CreditNoteBL;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDAS;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.event.BeforeInvoiceDeleteEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;

public class ApplyNegativeInvoiceToCreditNoteTask extends PluggableTask
        implements IInternalEventsTask {

    private static final Logger LOG =
            Logger.getLogger(ApplyNegativeInvoiceToCreditNoteTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            InvoicesGeneratedEvent.class,
            BeforeInvoiceDeleteEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    @Override
    public void process (Event event) throws PluggableTaskException {

        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;

            for (Integer invoiceId: instantiatedEvent.getInvoiceIds()) {
            	InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();
            	if (invoiceDTO.getBalance().compareTo(BigDecimal.ZERO) < 0 && invoiceDTO.getIsReview().equals(0)) {
	            	generateCreditNote(invoiceDTO);
            	}
            }

        } else if (event instanceof BeforeInvoiceDeleteEvent) {
            BeforeInvoiceDeleteEvent instantiatedEvent = (BeforeInvoiceDeleteEvent) event;
            InvoiceDTO invoiceDTO = instantiatedEvent.getInvoice();
            deleteCreditNote(invoiceDTO);
        } else {
            throw new PluggableTaskException("Unknown event: " +
                    event.getClass());
        }
    }

    private void generateCreditNote(InvoiceDTO invoiceDTO) {

    	CreditNoteBL creditNoteBl = new CreditNoteBL();
    	Integer creditNoteId = creditNoteBl.create(invoiceDTO);

        IBillingProcessSessionBean processBean = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
        BillingProcessConfigurationDTO configuration = processBean.getConfigurationDto(getEntityId());
        if (configuration.isTrueAutoCreditNoteApplication()) {
	        creditNoteBl.applyCreditNote(creditNoteId);
        }
    }

    private void deleteCreditNote(InvoiceDTO invoiceDTO) {

        if (invoiceDTO.getCreditNoteGenerated() != null) {
            new CreditNoteDAS().delete(invoiceDTO.getCreditNoteGenerated());
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
