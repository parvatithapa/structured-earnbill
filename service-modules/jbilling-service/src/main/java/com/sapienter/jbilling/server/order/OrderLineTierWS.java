package com.sapienter.jbilling.server.order;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author Faizan
 */

public class OrderLineTierWS implements Serializable{

    private int id;
    private Integer orderLineId;
    private Integer tierNumber;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal tierFrom;
    private BigDecimal tierTo;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Integer getOrderLineId() {
        return orderLineId;
    }
    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }
    public Integer getTierNumber() {
        return tierNumber;
    }
    public void setTierNumber(Integer tierNumber) {
        this.tierNumber = tierNumber;
    }
    public BigDecimal getQuantity() {
        return quantity;
    }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public BigDecimal getTierFrom() { return tierFrom;}
    public void setTierFrom(BigDecimal fromTier) { this.tierFrom = fromTier;}
    public BigDecimal getTierTo() { return tierTo; }
    public void setTierTo(BigDecimal toTier) { this.tierTo = toTier;}

    @Override public String toString() {
        return new StringBuilder("OrderLineTierWS{")
                .append( "id=").append(id)
                .append(", orderLine=").append(orderLineId)
                .append( ", amount='").append(amount)
                .append(", quantity='").append(quantity)
                .append(", price='").append( price)
                .append(", tierNumber=").append( tierNumber)
                .append(", tierFrom=").append(tierFrom)
                .append(", tierTo=").append(tierTo)
                .append('}').toString();
    }

}
