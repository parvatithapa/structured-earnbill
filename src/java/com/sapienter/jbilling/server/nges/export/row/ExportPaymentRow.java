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

package com.sapienter.jbilling.server.nges.export.row;

/**
 * Created by hitesh on 3/8/16.
 */
public class ExportPaymentRow extends ExportRow {

    //(M) LDC Name
    private String companyName;
    //(M) Utility Account Number
    private String customerAcct;
    //(M) Payment Date
    private String payDate;
    //(M) Form of the payment(Payment Method)
    private String payType;
    //(M) Amount
    private String payAmount;
    //(O) Payment Notes
    private String description;
    //(O) Invoice Date
    private String invoiceDate;
    //(O) TRANSFER_NR (Invoice_Nr metaField at Invoice level)
    private String utiliyInvNr;
    //(O) 'Type Code' meta field at payment level.
    private String typeCode;
    //?
    private String metaField4;
    //?
    private String metaField5;


    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCustomerAcct() {
        return customerAcct;
    }

    public void setCustomerAcct(String customerAcct) {
        this.customerAcct = customerAcct;
    }

    public String getPayDate() {
        return payDate;
    }

    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(String payAmount) {
        this.payAmount = payAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getUtiliyInvNr() {
        return utiliyInvNr;
    }

    public void setUtiliyInvNr(String utiliyInvNr) {
        this.utiliyInvNr = utiliyInvNr;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setMetaField4(String metaField4) {
        this.metaField4 = metaField4;
    }

    public String getMetaField4() {
        return metaField4;
    }

    public void setMetaField5(String metaField5) {
        this.metaField5 = metaField5;
    }

    public String getMetaField5() {
        return metaField5;
    }

    public static String getHeader() {
        return "companyName,customerAccNr,date,type,amount,description,invoiceDate,utiliyInvNr,paymentCode,metaField4,metaField5";
    }

    public static String getErrorFileHeader() {
        return "payment_id,error_message";
    }

    @Override
    public String getRow() {
        String row = companyName + "," + customerAcct + "," + payDate + "," + payType + "," + payAmount + "," + description + "," + invoiceDate + "," + utiliyInvNr + "," + typeCode + "," + metaField4 + "," + metaField5;
        super.row = row;
        return row;
    }
}
