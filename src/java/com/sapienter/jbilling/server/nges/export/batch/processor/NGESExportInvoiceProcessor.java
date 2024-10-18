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
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.nges.export.row.ExportInvoiceRow;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

/**
 * Created by hitesh on 3/8/16.
 */
public class NGESExportInvoiceProcessor extends AbstractNGESExportProcessor {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportInvoiceProcessor.class));

    private IWebServicesSessionBean webServicesSessionSpringBean;
    private UserWS userWS;

    @Override
    public ExportRow process(Integer invoiceLineId) throws Exception {
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        LOG.debug("find invoiceLine for id:" + invoiceLineId);
        InvoiceLineDTO invoiceLine = new InvoiceLineDAS().findNow(invoiceLineId);
        if (invoiceLine == null) throw new SessionInternalError("invoiceLine line not found for id:" + invoiceLineId);
        userWS = webServicesSessionSpringBean.getUserWS(invoiceLine.getInvoice().getUserId());
        return prepare(invoiceLine);
    }

    private ExportRow prepare(InvoiceLineDTO invoiceLine) {
        LOG.debug("prepare row for invoice line");
        ExportInvoiceRow invoiceRow = new ExportInvoiceRow();
        InvoiceDTO invoice = invoiceLine.getInvoice();

        invoiceRow.setCompanyName(validate(FieldName.COMPANY_NAME, userWS.getCompanyName(), true));
        invoiceRow.setCustomerAcct(validate(FileConstants.UTILITY_CUST_ACCT_NR, getMetaFieldValue(FileConstants.UTILITY_CUST_ACCT_NR, null, userWS.getMetaFields()), true));
        invoiceRow.setInvoiceId(validate(FieldName.INVOICE_ID, invoice.getId(), true));
        invoiceRow.setLineType(getLineType(invoiceLine));
        invoiceRow.setLineProductId(validate(FieldName.INVOICE_LINE_TYPE_ID, invoiceLine.getTypeId(), true));
        invoiceRow.setLineDescription(getLineDescription(invoiceLine));
        invoiceRow.setInvoiceDate(getFormattedDate(validate(FieldName.INVOICE_CREATION_DATE, invoice.getCreateDatetime(), true)));
        invoiceRow.setLineQuantity(validate(FieldName.INVOICE_LINE_QUANTITY, invoiceLine.getQuantity(), false));
        invoiceRow.setLineUnitPrice(getLineUnitPrice(invoiceLine));
        invoiceRow.setLineTotal(validate(FieldName.INVOICE_LINE_AMOUNT, invoiceLine.getAmount(), true));
        invoiceRow.setBillTotal(validate(FieldName.BILL_TOTAL, invoice.getTotal(), false));
        invoiceRow.setDueDate(getFormattedDate(validate(FieldName.INVOICE_DUE_DATE, invoice.getDueDate(), false)));
        invoiceRow.setFromDate(invoiceLine.getOrder() != null ? getFormattedDate(validate(FieldName.ORDER_ACTIVE_SINCE_DATE, invoiceLine.getOrder().getActiveSince(), true)) : "");
        invoiceRow.setToDate(invoiceLine.getOrder() != null ? getFormattedDate(validate(FieldName.ORDER_ACTIVE_UNTIL_DATE, invoiceLine.getOrder().getActiveUntil(), true)) : "");
        //TODO: Need to discuss
        invoiceRow.setReadDate("");
        invoiceRow.setBillState("");
        MetaFieldValue invoiceNrMfv = invoice.getMetaField(FileConstants.INVOICE_NR);
        invoiceRow.setUtilityInvoiceNumber(validate(FileConstants.INVOICE_NR, invoiceNrMfv != null ? invoiceNrMfv.getValue() : null, false));
        invoiceRow.setIncludeType("");

        invoiceRow.getRow();
        return invoiceRow;
    }

    private String getLineType(InvoiceLineDTO invoiceLine) {
        validate(FieldName.INVOICE_LINE_TYPE_ID, invoiceLine.getTypeId(), true);
        switch (invoiceLine.getTypeId()) {
            case 1:
                return "Usage";
            case 2:
                return "Tax";
            case 6:
                CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = invoiceLine.getInvoice().getBaseUser().getCustomer().getCustomerAccountInfoTypeMetaField(FileConstants.PASS_THROUGH_CHARGES_META_FIELD);
                if (customerAccountInfoTypeMetaField != null && customerAccountInfoTypeMetaField.getMetaFieldValue() != null && customerAccountInfoTypeMetaField.getMetaFieldValue().getValue() != null) {
                    String passThroughLineLossMfv = customerAccountInfoTypeMetaField.getMetaFieldValue().getValue().toString();
                    if (!passThroughLineLossMfv.isEmpty() && passThroughLineLossMfv.equals(invoiceLine.getDescription())) {
                        return "Pass-thru Charge";
                    }
                }
                return "One-time";
            default:
                return "";
        }
    }

    private String getLineDescription(InvoiceLineDTO invoiceLine) {
        String percentageSymbol = "";
        if (invoiceLine.getIsPercentage() == 1) percentageSymbol = "%";
        return validate(FieldName.INVOICE_LINE_PRICE, invoiceLine.getPrice().setScale(5, BigDecimal.ROUND_HALF_UP), false) + percentageSymbol + " " + validate(FieldName.INVOICE_LINE_DESCRIPTION, invoiceLine.getDescription(), false);
    }

    private String getLineUnitPrice(InvoiceLineDTO invoiceLine) {
        BigDecimal price = invoiceLine.getPrice();
        if (invoiceLine.getIsPercentage() == 1 && price != null && price != BigDecimal.ZERO) {
            price = price.divide(new BigDecimal(100));
        }
        return validate(FieldName.INVOICE_LINE_PRICE, price.setScale(5, BigDecimal.ROUND_HALF_UP), false);
    }
}
