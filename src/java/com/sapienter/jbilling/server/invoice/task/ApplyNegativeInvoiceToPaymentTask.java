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

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

public class ApplyNegativeInvoiceToPaymentTask extends PluggableTask
        implements IInternalEventsTask {

    private static final Logger LOG =
            Logger.getLogger(ApplyNegativeInvoiceToPaymentTask.class);

    private static final Class<Event> events[] = new Class[]{
            InvoicesGeneratedEvent.class
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process (Event event) throws PluggableTaskException {

        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;

            for(Integer invoiceId: instantiatedEvent.getInvoiceIds()){
                fixNegativeInvoice(invoiceId);
            }

        } else {
            throw new PluggableTaskException("Unknown event: " +
                    event.getClass());
        }
    }

    private void fixNegativeInvoice(Integer invoiceId){
        InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();

        if (invoiceDTO.getTotal().compareTo(BigDecimal.ZERO) < 0 && invoiceDTO.getIsReview().equals(0)) {
            PaymentDTOEx creditPayment = new PaymentDTOEx();
            creditPayment.setIsRefund(0);
            creditPayment.setAmount(invoiceDTO.getBalance().negate());
            creditPayment.setCurrency(new CurrencyDAS().find(invoiceDTO.getCurrency().getId()));
            creditPayment.setPaymentDate(companyCurrentDate());
            creditPayment.setPaymentMethod(new PaymentMethodDTO(Constants.PAYMENT_METHOD_CREDIT));
            creditPayment.setUserId(invoiceDTO.getUserId());
            IPaymentSessionBean paymentSessionBean = Context.getBean(Context.Name.PAYMENT_SESSION);
            paymentSessionBean.applyPayment(creditPayment, null, invoiceDTO.getUserId());
            invoiceDTO.setTotal(BigDecimal.ZERO);
            invoiceDTO.setBalance(BigDecimal.ZERO);
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
