package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.BeforeInvoiceDeleteEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.db.*;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * The task will negate all commission related to invoice.
 */
public class CommissionInvoiceDeleteTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CommissionInvoiceDeleteTask.class));

    private static final Class<Event> events[] = new Class[]{
            BeforeInvoiceDeleteEvent.class
    };



    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if(!(event instanceof BeforeInvoiceDeleteEvent)){
            return;
        }

        InvoiceDTO invoice=((BeforeInvoiceDeleteEvent) event).getInvoice();
        PartnerCommissionDAS partnerCommissionDAS = new PartnerCommissionDAS();
        List<InvoiceCommissionDTO> invoiceCommissionDTOs=partnerCommissionDAS.findInvoiceCommissionByInvoice(invoice.getId());
        new PartnerBL().reverseCommissions(invoiceCommissionDTOs, getEntityId());

        invoiceCommissionDTOs=partnerCommissionDAS.findInvoiceCommissionByInvoice(invoice.getId());
        invoiceCommissionDTOs.stream().forEach((InvoiceCommissionDTO invoiceCommissionDTO) -> invoiceCommissionDTO.setInvoice(null));

    }
}
