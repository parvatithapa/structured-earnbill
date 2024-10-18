package com.sapienter.jbilling.server.invoice.task;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.sapienter.jbilling.server.util.BetaCustomerConstants.*;


public class AdHocInvoiceUpdaterTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(AdHocInvoiceUpdaterTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            InvoicesGeneratedEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;
            InvoiceDAS invoiceDAS = new InvoiceDAS();
            try {
                for (Integer invoiceId : instantiatedEvent.getInvoiceIds()) {
                    InvoiceDTO invoiceDTO = new InvoiceDAS().findNow(invoiceId);
                    OrderDTO orderDTO = new OrderDAS().findNow(invoiceDAS.getFirstOrderIdByInvoiceId(invoiceId));
                    List<MetaFieldValue> metaFields = orderDTO.getMetaFields();
                    for (MetaFieldValue metaFieldValue : metaFields) {
                        if (metaFieldValue.getFieldName().equals(INVOICE_DATE) && StringUtils.isNotBlank((String) metaFieldValue.getValue())) {
                            invoiceDTO.setCreateDatetime(new SimpleDateFormat("MM/dd/yyyy").parse((String) metaFieldValue.getValue()));
                        }
                        if (metaFieldValue.getFieldName().equals(INVOICE_DUE_DATE) && StringUtils.isNotBlank((String) metaFieldValue.getValue())) {
                            invoiceDTO.setDueDate(new SimpleDateFormat("MM/dd/yyyy").parse((String) metaFieldValue.getValue()));
                        }
                        if (metaFieldValue.getFieldName().equals(CUSTOM_INVOICE_NUMBER) && StringUtils.isNotBlank((String) metaFieldValue.getValue())) {
                            invoiceDTO.setPublicNumber((String) metaFieldValue.getValue());
                        }
                    }
                    invoiceDAS.save(invoiceDTO);
                }
            } catch (ParseException e) {
                logger.error("Error while parsing date {}", e);
                throw new SessionInternalError("Error while parsing date");
            }
        }
    }
}
