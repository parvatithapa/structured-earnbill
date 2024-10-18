package com.sapienter.jbilling.server.spc.wookie.crm;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

class WookieProductWS {

    private String productCode;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;

    WookieProductWS() { }

    WookieProductWS(final String productCode, final BigDecimal quantity, final BigDecimal price,
            final BigDecimal taxRate, final BigDecimal taxAmount) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.price = price;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
    }

    @JsonProperty("taxrate")
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    @JsonProperty("taxamount")
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    @JsonProperty("productcode")
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    @JsonProperty("quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    @JsonProperty("listprice")
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProductWS [productCode=");
        builder.append(productCode);
        builder.append(", quantity=");
        builder.append(quantity);
        builder.append(", price=");
        builder.append(taxRate);
        builder.append(taxAmount);
        builder.append("]");
        return builder.toString();
    }

}
