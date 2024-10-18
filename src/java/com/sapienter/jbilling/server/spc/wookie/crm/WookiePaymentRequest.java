package com.sapienter.jbilling.server.spc.wookie.crm;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class WookiePaymentRequest {

    private String vendor;
    private Integer invoiceId;
    private Integer paymentId;
    private WookiePaymentStatus paymentStatus;
    private BigDecimal amountPaid;
    private WookiePaymentType paymentType;
    private WookiePaymentMethod paymentMethod;
    private Date paymentDate;
    private Integer paymentReference;
    private Integer parentPaymentId;

    @JsonProperty("vendor")
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @JsonProperty("vendor_invoice_id")
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    @JsonProperty("vendor_payment_id")
    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    @JsonProperty("payment_status")
    public WookiePaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(WookiePaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @JsonProperty("amount_paid")
    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    @JsonProperty("payment_type")
    public WookiePaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(WookiePaymentType paymentType) {
        this.paymentType = paymentType;
    }

    @JsonProperty("payment_method")
    public WookiePaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(WookiePaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @JsonProperty("date")
    @JsonSerialize(using = JsonDateSerializer.class)
    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    @JsonProperty("payment_reference")
    public Integer getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(Integer paymentReference) {
        this.paymentReference = paymentReference;
    }

    @JsonProperty("vendor_parent_payment_id")
    public Integer getParentPaymentId() {
        return parentPaymentId;
    }

    public void setParentPaymentId(Integer parentPaymentId) {
        this.parentPaymentId = parentPaymentId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WookiePaymentRequest [vendor=");
        builder.append(vendor);
        builder.append(", invoiceId=");
        builder.append(invoiceId);
        builder.append(", paymentId=");
        builder.append(paymentId);
        builder.append(", paymentStatus=");
        builder.append(paymentStatus);
        builder.append(", amountPaid=");
        builder.append(amountPaid);
        builder.append(", paymentType=");
        builder.append(paymentType);
        builder.append(", paymentMethod=");
        builder.append(paymentMethod);
        builder.append(", paymentDate=");
        builder.append(paymentDate);
        builder.append(", paymentReference=");
        builder.append(paymentReference);
        builder.append(", parentPaymentId=");
        builder.append(parentPaymentId);
        builder.append("]");
        return builder.toString();
    }

}
