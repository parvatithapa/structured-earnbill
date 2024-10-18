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
public class ExportInvoiceRow extends ExportRow {

    //(M) LDC Name.
    private String companyName;
    //(M) Utility Account Number.
    private String customerAcct;
    //(M) Invoice Id.
    private String invoiceId;
    //(M) Usage or Tax or Pass-thru Charge in the future ‘One-time charge” or other types could be included.
    private String lineType;
    //(M) lineType Id.
    private String lineProductId;
    //(O) Description.
    private String lineDescription;
    //(M) Date of the invoice.
    private String invoiceDate;
    //(O) Quantity.
    private String lineQuantity;
    //(O) Unit price
    private String lineUnitPrice;
    //(M) Total amount of line.
    private String lineTotal;
    //(O) Total amount of invoice.
    private String billTotal;
    //(O) Due date of invoice.
    private String dueDate;
    //(O) From date
    private String fromDate;
    //(O) To date
    private String toDate;
    //(O) ?
    private String readDate;
    //(O) ?
    private String billState;
    //(O) ?
    private String utilityInvoiceNumber;

    private String includeType;

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

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getLineProductId() {
        return lineProductId;
    }

    public void setLineProductId(String lineProductId) {
        this.lineProductId = lineProductId;
    }

    public String getLineDescription() {
        return lineDescription;
    }

    public void setLineDescription(String lineDescription) {
        this.lineDescription = lineDescription;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getLineQuantity() {
        return lineQuantity;
    }

    public void setLineQuantity(String lineQuantity) {
        this.lineQuantity = lineQuantity;
    }

    public String getLineUnitPrice() {
        return lineUnitPrice;
    }

    public void setLineUnitPrice(String lineUnitPrice) {
        this.lineUnitPrice = lineUnitPrice;
    }

    public String getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(String lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(String billTotal) {
        this.billTotal = billTotal;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getReadDate() {
        return readDate;
    }

    public void setReadDate(String readDate) {
        this.readDate = readDate;
    }

    public String getBillState() {
        return billState;
    }

    public void setBillState(String billState) {
        this.billState = billState;
    }

    public String getUtilityInvoiceNumber() {
        return utilityInvoiceNumber;
    }

    public void setUtilityInvoiceNumber(String utilityInvoiceNumber) {
        this.utilityInvoiceNumber = utilityInvoiceNumber;
    }

    public String getIncludeType() {
        return includeType;
    }

    public void setIncludeType(String includeType) {
        this.includeType = includeType;
    }

    @Override
    public String getRow() {
        return super.row = companyName + "," + customerAcct + "," + invoiceId + "," + lineType + "," + lineProductId + "," + lineDescription + "," + invoiceDate + "," + lineQuantity + "," + lineUnitPrice + "," + lineTotal + "," + billTotal + "," + dueDate + "," + fromDate + "," + toDate + "," + readDate + "," + billState + "," + utilityInvoiceNumber + "," + includeType;
    }

    public static String getHeader() {
        return "companyName,customerAccNr,invoiceId,lineType,lineProductId,lineDescription,invoiceDate,lineQuantity,lineUnitPrice,lineTotal,billTotal,dueDate,fromDate,toDate,readDate,billState,utilityInvoiceNumber,IncludeType";
    }

    public static String getErrorFileHeader() {
        return "invoice_line_id,error_message";
    }
}
