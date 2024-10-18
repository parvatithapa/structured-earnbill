package com.sapienter.jbilling.server.user.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.util.csv.DynamicExport;
import com.sapienter.jbilling.server.util.csv.ExportableWrapper;

/**
 * @author Harshad Pathan
 * @since 20/07/2017
 */
public class InvoiceExportableWrapper implements ExportableWrapper<InvoiceDTO> {

    private Integer invoiceId;
    @SuppressWarnings("unused")
    private DynamicExport dynamicExport = DynamicExport.NO;

    private void init(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public InvoiceExportableWrapper(Integer invoiceId) {
        init(invoiceId);
    }

    public InvoiceExportableWrapper(Integer invoiceId, DynamicExport dynamicExport) {
        init(invoiceId);
        setDynamicExport(dynamicExport);
    }

    @Override
    public InvoiceDTO getWrappedInstance() {
        return new InvoiceDAS().find(invoiceId);
    }

    @Override
    public void setDynamicExport(DynamicExport dynamicExport) {
        this.dynamicExport = dynamicExport;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{
                "id",
                "invoiceNumber",
                "userId",
                "userName",
                "status",
                "currency",
                "delegatedInvoices",
                "carriedBalance",
                "invoiceAmount",
                "balance",
                "invoiceDate",
                "dueDate",
                "paymentAttempts",
                "paymentID",
                "isReview",
                "notes",
                "primaryAccountNumber",

                // invoice lines
                "lineItemId",
                "lineProductCode",
                "lineQuantity",
                "linePrice",
                "lineAmount",
                "lineDescription",
                "callCounter",
                "assetIdentifier",
                "planId"
        };
    }

    @Override
    public Object[][] getFieldValues() {
        return getInvoiceFieldValues();
    }


    public Object[][] getInvoiceFieldValues() {
        InvoiceDTO invoice = getWrappedInstance();
        String delegatedInvoiceIds = invoice.getInvoices()
                .stream()
                .map(InvoiceDTO::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(";"));

        String paymentIds = invoice.getPaymentMap()
                .stream()
                .map(PaymentInvoiceMapDTO::getPayment)
                .map(PaymentDTO::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(";"));


        List<Object[]> values = new ArrayList<>();
        // main invoice row
        values.add(
                new Object[]{
                        invoice.getId(),
                        invoice.getPublicNumber(),
                        invoice.getUserId(),
                        invoice.getBaseUser().getUserName(),
                        invoice.getInvoiceStatus().getDescription(1),
                        invoice.getCurrency().getCode(),
                        delegatedInvoiceIds,
                        invoice.getCarriedBalance(),
                        invoice.getTotal(),
                        invoice.getBalance(),
                        invoice.getCreateDatetime(),
                        invoice.getDueDate(),
                        invoice.getPaymentAttempts(),
                        paymentIds,
                        invoice.getIsReview(),
                        invoice.getCustomerNotes(),
                        (invoice.getBaseUser() != null ? (null != invoice.getBaseUser().getCustomer().getMetaField("primaryAccountNumber") ?
                                invoice.getBaseUser().getCustomer().getMetaField("primaryAccountNumber").getValue() : null) : null)
                }
        );

        // indented row for each invoice line
        for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
            if (line.getDeleted().equals(0)) {
                values.add(
                        new Object[]{
                                // padding for the main invoice columns
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,

                                // invoice line
                                (line.getItem() != null ? line.getItem().getId() : null),
                                (line.getItem() != null ? line.getItem().getInternalNumber() : null),
                                line.getQuantity(),
                                line.getPrice(),
                                line.getAmount(),
                                line.getDescription(),
                                line.getCallCounter(),
                                (line.getAssetIdentifier() != null ? line.getAssetIdentifier() :
                                        line.getCallIdentifier() != null ? line.getCallIdentifier() : null),
                                line.getUsagePlanId()
                        }
                );
            }
        }
        return values.toArray(new Object[values.size()][]);
    }

}