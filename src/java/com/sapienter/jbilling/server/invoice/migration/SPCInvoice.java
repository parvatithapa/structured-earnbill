package com.sapienter.jbilling.server.invoice.migration;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.StringJoiner;


public class SPCInvoice {

    private Integer    userId ;
    private String     crmAccountNumber;
    private String     billingCycle;
    private String 	   billRunTiming;
    private BigDecimal openingBalance;
    private BigDecimal payments;
    private BigDecimal adjustments;
    private BigDecimal newCharges;
    private BigDecimal balance;
    private BigDecimal checkBalance;
    private String     exception;


    public static final String COMMA_DELIMITER = ",";
    public static final String NEW_LINE_SEPARATOR = "\n";
    public static final String HYPHEN = " - ";
    public static final String PLUS = "+";
    public static final String QUOTES = "\"";


    public SPCInvoice() {
        // TODO Auto-generated constructor stub
    }

    public static String getHeaders() {
        Field[] fields = SPCInvoice.class.getDeclaredFields();
        StringJoiner joiner = new StringJoiner(QUOTES + COMMA_DELIMITER + QUOTES, QUOTES, QUOTES);
        for (Field field : fields) {
            joiner.add(field.getName());
        }
        return joiner.toString();
    }

    public String getBillRunTiming() {
        return billRunTiming;
    }

    public void setBillRunTiming(String billRunTiming) {
        this.billRunTiming = billRunTiming;
    }

    public String getCrmAccountNumber() {
        return crmAccountNumber;
    }

    public void setCrmAccountNumber(String crmAccountNumber) {
        this.crmAccountNumber = crmAccountNumber;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getPayments() {
        return payments;
    }

    public void setPayments(BigDecimal payments) {
        this.payments = payments;
    }

    public BigDecimal getAdjustments() {
        return adjustments;
    }

    public void setAdjustments(BigDecimal adjustments) {
        this.adjustments = adjustments;
    }

    public BigDecimal getNewCharges() {
        return newCharges;
    }

    public void setNewCharges(BigDecimal newCharges) {
        this.newCharges = newCharges;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }


    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setCheckBalance(BigDecimal checkBalance) {
        this.checkBalance = checkBalance;
    }

    public BigDecimal getCheckBalance() {
        return checkBalance;
    }

    @Override
    public String toString() {
        return this.userId + ","+ this.crmAccountNumber +","+ this.billingCycle + "," + this.billRunTiming + 
                "," + this.openingBalance + "," + this.payments + "," + this.adjustments + "," + this.newCharges
                + "," + this.balance + "," +  this.checkBalance + " ," + this.exception;
    }

}
