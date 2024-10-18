package com.sapienter.jbilling.server.payment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@SuppressWarnings("serial")
public class PaymentInvoiceMapWS implements Serializable {

    private Integer id;
    private Integer invoiceId;
    private Integer paymentId;
    private BigDecimal amount;
    private Date createDatetime;
    private String paymentType;
    private String paymentStatus;

    public PaymentInvoiceMapWS(Integer id, Integer invoiceId, Integer paymentId, Date createDatetime, BigDecimal amount) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
        this.createDatetime = createDatetime;
        this.amount = amount;
    }

    public Integer getId() {
        return id;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd hh-mm-ss")
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setPaymentType(int paymentType) {
        if(paymentType == 1){
            setPaymentType("Refund");
        }else{
            setPaymentType("Payment");
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PaymentInvoiceMapWS [id=");
        builder.append(id);
        builder.append(", invoiceId=");
        builder.append(invoiceId);
        builder.append(", paymentId=");
        builder.append(paymentId);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", createDatetime=");
        builder.append(createDatetime);
        builder.append(", paymentType=");
        builder.append(paymentType);
        builder.append(", paymentStatus=");
        builder.append(paymentStatus);
        builder.append("]");
        return builder.toString();
    }

}
