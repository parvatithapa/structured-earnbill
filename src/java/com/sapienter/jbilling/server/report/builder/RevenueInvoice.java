package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * RevenueInvoice class
 * 
 * This class represents the data retrieved from the InvoiceDAS in the getRevenueInvoices method.
 * 
 * @author Leandro Bagur
 * @since 30/01/18.
 */
public class RevenueInvoice {

    private int invoiceId;
    private String invoiceNumber;
    private int userId;
    private Date createdDate;
    private String currency;
    private int invoiceLineId;
    private int itemId;
    private BigDecimal amount;
    private int orderType;
    private int monthTerm;
    private String province;
    private Boolean taxExempt;

    public RevenueInvoice() {}
    
    public int getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public int getUserId() {
        return userId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getCurrency() {
        return currency;
    }

    public int getInvoiceLineId() {
        return invoiceLineId;
    }

    public int getItemId() {
        return itemId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getOrderType() {
        return orderType;
    }

    public int getMonthTerm() {
        return monthTerm;
    }

    public String getProvince() {
        return province;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setInvoiceLineId(int invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setOrderType(int orderType) {
        this.orderType = orderType;
    }

    public void setMonthTerm(int monthTerm) {
        this.monthTerm = monthTerm;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public Boolean isTaxExempt() {
        if (taxExempt == null) {
            return Boolean.FALSE;
        }
        return taxExempt;
    }

    public void setTaxExempt(Boolean taxExempt) {
        this.taxExempt = taxExempt;
    }
}
