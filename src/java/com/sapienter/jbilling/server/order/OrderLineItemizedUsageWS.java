package com.sapienter.jbilling.server.order;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;


@SuppressWarnings("serial")
public class OrderLineItemizedUsageWS implements Serializable {

    private Integer id;
    private String amount;
    private String quantity;
    private Integer orderLineId;
    private String separator;

    public OrderLineItemizedUsageWS(Integer id, String amount, String quantity, Integer orderLineId, String separator) {
        this.id = id;
        this.amount = amount;
        this.quantity = quantity;
        this.orderLineId = orderLineId;
        this.separator = separator;
    }

    public Integer getId() {
        return id;
    }

    public String getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public BigDecimal getAmountAsDecimal() {
        return Util.string2decimal(amount);
    }

    public String getQuantity() {
        return quantity;
    }

    @JsonProperty("quantity")
    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public String getSeparator() {
        return separator;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OrderLineItemizedUsageWS [id=");
        builder.append(id);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", quantity=");
        builder.append(quantity);
        builder.append(", orderLineId=");
        builder.append(orderLineId);
        builder.append(", separator=");
        builder.append(separator);
        builder.append("]");
        return builder.toString();
    }

}
