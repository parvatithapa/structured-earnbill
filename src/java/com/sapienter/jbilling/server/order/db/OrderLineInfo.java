package com.sapienter.jbilling.server.order.db;

import java.math.BigDecimal;

import lombok.ToString;

@ToString
public class OrderLineInfo {

    private BigDecimal quantity;
    private BigDecimal amount;
    private BigDecimal freeUsageQuantity;
    private BigDecimal price;

    public OrderLineInfo(BigDecimal quantity, BigDecimal amount, BigDecimal freeUsageQuantity, BigDecimal price) {
        this.quantity = quantity;
        this.amount = amount;
        this.freeUsageQuantity = freeUsageQuantity;
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFreeUsageQuantity() {
        return freeUsageQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
