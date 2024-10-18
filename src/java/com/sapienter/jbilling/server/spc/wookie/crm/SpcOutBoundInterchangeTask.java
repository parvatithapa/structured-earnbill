package com.sapienter.jbilling.server.spc.wookie.crm;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sapienter.jbilling.server.payment.event.PaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;

public class SpcOutBoundInterchangeTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final ParameterDescription PARAM_WOOKIE_URL =
            new ParameterDescription("wookie_url", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_CREATE_INVOICE_METHOD_NAME =
            new ParameterDescription("invoice_method_name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_CREATE_PAYMENT_METHOD_NAME =
            new ParameterDescription("payment_method_name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_SHIPPING_AIT_GROUP_NAME =
            new ParameterDescription("shipping_address_ait_group_name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_BILLING_AIT_GROUP_NAME =
            new ParameterDescription("billing_address_ait_group_name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_TIMEOUT =
            new ParameterDescription("timeout", false, ParameterDescription.Type.INT);

    static final ParameterDescription PARAM_USER_NAME =
            new ParameterDescription("user_name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_PASSWORD =
            new ParameterDescription("password", true, ParameterDescription.Type.STR, true);

    static final ParameterDescription PARAM_DISCOUNT_PRODUCT_CODE =
            new ParameterDescription("discount_product_code", true, ParameterDescription.Type.STR, false);

    static final ParameterDescription PARAM_CRM_INVOICE_ID =
            new ParameterDescription("crmInvoiceID_meta_field_name", true, ParameterDescription.Type.STR, false);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] EVENTS = new Class[] {
        InvoicesGeneratedEvent.class,
        PaymentSuccessfulEvent.class,
        PaymentFailedEvent.class
    };

    public SpcOutBoundInterchangeTask() {
        descriptions.add(PARAM_WOOKIE_URL);
        descriptions.add(PARAM_CREATE_INVOICE_METHOD_NAME);
        descriptions.add(PARAM_CREATE_PAYMENT_METHOD_NAME);
        descriptions.add(PARAM_BILLING_AIT_GROUP_NAME);
        descriptions.add(PARAM_SHIPPING_AIT_GROUP_NAME);
        descriptions.add(PARAM_USER_NAME);
        descriptions.add(PARAM_PASSWORD);
        descriptions.add(PARAM_TIMEOUT);
        descriptions.add(PARAM_DISCOUNT_PRODUCT_CODE);
        descriptions.add(PARAM_CRM_INVOICE_ID);
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return EVENTS;
    }


    @Override
    public void process(final Event event) throws PluggableTaskException {
        logger.debug("processing event {} for entity {}", event.getName(), event.getEntityId());
        // registering call back which executes after current transaction commits.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(final int status) {
                // only successfully committed invoices and payments will be transfered to wookie crm.
                if(TransactionSynchronization.STATUS_COMMITTED == status) {
                    logger.debug("executing event {} for entity {} after commiting transaction", event.getName(), event.getEntityId());
                    SpcOutBoundInterchangeHelperService helperService = Context.getBean(SpcOutBoundInterchangeHelperService.class);
                    helperService.postEventToWookieAsync(new SpcOutBoundInterchangeRequest(event, parameters));
                    return ;
                }
                logger.debug("skipping event {} transfer since transaction is roll back for entity {}",event.getName(), event.getEntityId());

            }
        });
    }

}
