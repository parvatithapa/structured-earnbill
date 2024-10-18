/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.nges.export.batch.processor;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.nges.export.row.ExportPaymentRow;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;

import java.util.Set;

/**
 * Created by hitesh on 3/8/16.
 */
public class NGESExportPaymentProcessor extends AbstractNGESExportProcessor {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportPaymentProcessor.class));

    private PaymentDAS paymentDAS;
    private IWebServicesSessionBean webServicesSessionSpringBean;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("*****BEFORE STEP*****");
        paymentDAS = new PaymentDAS();
    }

    @Override
    public ExportRow process(Integer paymentId) throws Exception {
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        LOG.debug("find payment for id:" + paymentId);
        PaymentDTO paymentDTO = paymentDAS.find(paymentId);
        if (paymentDTO == null) {
            LOG.debug("payment not found for id:" + paymentId);
            throw new SessionInternalError("payment not found for id:" + paymentId);
        }
        return prepare(paymentDTO);
    }

    private ExportRow prepare(PaymentDTO paymentDTO) {
        LOG.debug("prepare row for payment");
        ExportPaymentRow paymentRow = new ExportPaymentRow();
        UserWS userWS = webServicesSessionSpringBean.getUserWS(paymentDTO.getBaseUser().getUserId());
        InvoiceDTO invoice = getInvoiceByPayment(paymentDTO);

        paymentRow.setCompanyName(userWS.getCompanyName());
        paymentRow.setCustomerAcct(validate(FileConstants.UTILITY_CUST_ACCT_NR, getMetaFieldValue(FileConstants.UTILITY_CUST_ACCT_NR, null, userWS.getMetaFields()), true));
        paymentRow.setPayDate(getFormattedDate(validate(FieldName.PAYMENT_DATE, paymentDTO.getPaymentDate(), true)));
        paymentRow.setPayType(validate(FieldName.PAYMENT_METHOD, paymentDTO.getPaymentMethod().getDescription(), true));
        paymentRow.setPayAmount(validate(FieldName.PAYMENT_AMOUNT, paymentDTO.getAmount(), true));
        paymentRow.setDescription(validate(FieldName.PAYMENT_NOTES, paymentDTO.getPaymentNotes(), false));
        paymentRow.setInvoiceDate(invoice != null ? getFormattedDate(validate(FieldName.INVOICE_CREATION_DATE, invoice.getCreateDatetime(), false)) : "");
        MetaFieldValue invoiceNrMfv = invoice.getMetaField(FileConstants.INVOICE_NR);
        paymentRow.setUtiliyInvNr(validate(FileConstants.INVOICE_NR, invoiceNrMfv != null ? invoiceNrMfv.getValue() : null, false));
        MetaFieldValue metaFieldValue = paymentDTO.getMetaField(FileConstants.META_FIELD_PAYMENT_TYPE_CODE);
        paymentRow.setTypeCode(validate(FileConstants.META_FIELD_PAYMENT_TYPE_CODE, metaFieldValue != null ? metaFieldValue.getValue() : null, false));
        paymentRow.setMetaField4("");
        paymentRow.setMetaField5("");
        paymentRow.getRow();

        return paymentRow;
    }

    private InvoiceDTO getInvoiceByPayment(PaymentDTO paymentDTO) {
        Set<PaymentInvoiceMapDTO> paymentInvoices = paymentDTO.getInvoicesMap();
        for (PaymentInvoiceMapDTO paymentInvoice : paymentInvoices) {
            if (paymentInvoice.getPayment().getId() == paymentDTO.getId()) {
                return paymentInvoice.getInvoiceEntity();
            }
        }
        return null;
    }
}
