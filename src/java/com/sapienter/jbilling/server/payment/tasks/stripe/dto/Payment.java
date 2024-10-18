package com.sapienter.jbilling.server.payment.tasks.stripe.dto;



public class Payment {
    private long amount;
    private String currencyCode;

    public Payment(long amount, String currencyCode) {
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}