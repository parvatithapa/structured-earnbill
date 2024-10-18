package com.sapienter.jbilling.client.suretax.responsev2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class Group {
    @JsonProperty("LineNumber")
    public String lineNumber;
    @JsonProperty("StateCode")
    public String stateCode;
    @JsonProperty("InvoiceNumber")
    public String invoiceNumber;
    @JsonProperty("CustomerNumber")
    public String customerNumber;
    @JsonProperty("TaxList")
    public List<TaxItem> taxList;

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public void setTaxList(List<TaxItem> taxList) {
        this.taxList = taxList;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "Group [lineNumber=" + lineNumber + ", stateCode=" + stateCode + ", invoiceNumber="
                + invoiceNumber + ", customerNumber=" + customerNumber
                + ", taxList=" + taxList + "]";
    }
}
