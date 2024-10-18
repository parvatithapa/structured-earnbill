package com.sapienter.jbilling.server.customer.task;

import com.sapienter.jbilling.server.customer.event.UpdateDistributelCustomersInvoiceTemplateEvent;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import org.apache.log4j.Logger;

/**
 * Created by Fernando Sivila on 5/12/17.
 */
public class UpdateDistributelCustomersInvoiceTemplateTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(UpdateDistributelCustomersInvoiceTemplateTask.class);


    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            UpdateDistributelCustomersInvoiceTemplateEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof UpdateDistributelCustomersInvoiceTemplateEvent) {
            processUpdateCustomerInvoiceTemplate((UpdateDistributelCustomersInvoiceTemplateEvent) event);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private void processUpdateCustomerInvoiceTemplate(UpdateDistributelCustomersInvoiceTemplateEvent event) {
        CompanyDTO company = new CompanyDAS().find(event.getEntityId());
        Integer languageId = null;
        String invoiceTemplateName = SpaConstants.MF_ENGLISH_INVOICE_TEMPLATE_NAME;
        MetaFieldValue invoiceTemplateNameMF = company.getMetaField(invoiceTemplateName);
        if (invoiceTemplateNameMF.getValue().toString().equals(event.getTemplateName())) {
            languageId = SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE);
        }
        if (languageId == null) {
            invoiceTemplateName = SpaConstants.MF_FRENCH_INVOICE_TEMPLATE_NAME;
            invoiceTemplateNameMF = company.getMetaField(invoiceTemplateName);
            if (invoiceTemplateNameMF.getValue().toString().equals(event.getTemplateName())) {
                languageId = SpaImportHelper.getLanguageId(SpaConstants.FRENCH_LANGUAGE);
            }
        }
        if (languageId != null) {
            new CustomerDAS().updateDistributelCustomersInvoiceTemplate(event.getEntityId(), event.getTemplateId(), languageId);
        }
    }
}
