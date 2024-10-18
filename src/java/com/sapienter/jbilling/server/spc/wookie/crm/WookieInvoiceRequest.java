package com.sapienter.jbilling.server.spc.wookie.crm;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author krunal bhavsar
 *
 */
class WookieInvoiceRequest {

    private String vendor;
    private String subject;
    private Date invoiceDate;
    private Date dueDate;
    private Integer accountId;
    private BigDecimal total;
    private String billStreet;
    private String shipStreet;
    private WookieInvoiceStatus invoiceStatus;
    private Integer vendorInvoiceId;
    private Integer[] salesOrders;
    private WookieProductWS[] products;
    private Map<String, String> otherProperties;

    @JsonProperty("vendor")
    public String getVendor() {
        return vendor;
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @JsonProperty("invoicedate")
    @JsonSerialize(using = JsonDateSerializer.class)
    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(final Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    @JsonProperty("duedate")
    @JsonSerialize(using = JsonDateSerializer.class)
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    @JsonProperty("account_id")
    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(final Integer accountId) {
        this.accountId = accountId;
    }

    @JsonProperty("total")
    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @JsonProperty("bill_street")
    public String getBillStreet() {
        return billStreet;
    }

    public void setBillStreet(final String billStreet) {
        this.billStreet = billStreet;
    }

    @JsonProperty("ship_street")
    public String getShipStreet() {
        return shipStreet;
    }

    public void setShipStreet(final String shipStreet) {
        this.shipStreet = shipStreet;
    }

    @JsonProperty("invoicestatus")
    public WookieInvoiceStatus getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(final WookieInvoiceStatus invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    @JsonProperty("vendor_invoice_id")
    public Integer getVendorInvoiceId() {
        return vendorInvoiceId;
    }

    public void setVendorInvoiceId(final Integer vendorInvoiceId) {
        this.vendorInvoiceId = vendorInvoiceId;
    }

    @JsonProperty("salesorder_ids")
    public Integer[] getSalesOrders() {
        return salesOrders;
    }

    public void setSalesOrders(final Integer[] salesOrders) {
        this.salesOrders = salesOrders;
    }

    @JsonProperty("product")
    public WookieProductWS[] getProducts() {
        return products;
    }

    public void setProducts(final WookieProductWS[] products) {
        this.products = products;
    }

    @JsonAnyGetter
    public Map<String, String> getOtherProperties() {
        return otherProperties;
    }

    @JsonAnySetter
    public void setOtherProperty(final String key, final String value) {
        if (null == this.otherProperties) {
            this.otherProperties = new HashMap<>();
        }
        this.otherProperties.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WookieInvoiceRequest [vendor=");
        builder.append(vendor);
        builder.append(", subject=");
        builder.append(subject);
        builder.append(", invoiceDate=");
        builder.append(invoiceDate);
        builder.append(", dueDate=");
        builder.append(dueDate);
        builder.append(", accountId=");
        builder.append(accountId);
        builder.append(", total=");
        builder.append(total);
        builder.append(", billStreet=");
        builder.append(billStreet);
        builder.append(", shipStreet=");
        builder.append(shipStreet);
        builder.append(", invoiceStatus=");
        builder.append(invoiceStatus);
        builder.append(", vendorInvoiceId=");
        builder.append(vendorInvoiceId);
        builder.append(", salesOrders=");
        builder.append(Arrays.toString(salesOrders));
        builder.append(", products=");
        builder.append(Arrays.toString(products));
        builder.append(", otherProperties=");
        builder.append(otherProperties);
        builder.append("]");
        return builder.toString();
    }
}
