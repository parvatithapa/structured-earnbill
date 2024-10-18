package com.sapienter.jbilling.server.sapphire.cis;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by pablo on 19/06/17.
 */
public class SappPayment {
    
    private String id;
    private Date date;
    private String paidOrRefund;
    private String amount;
    private String paymentMethod;
    private String paymentResult;


    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaidOrRefund() {
        return paidOrRefund;
    }

    public void setPaidOrRefund(String paidOrRefund) {
        this.paidOrRefund = paidOrRefund;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentResult() {
        return paymentResult;
    }

    public void setPaymentResult(String paymentResult) {
        this.paymentResult = paymentResult;
    }
}
