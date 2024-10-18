/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.order.db;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

@Entity
@TableGenerator(
        name="order_line_tier_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="order_line_tier",
        allocationSize = 10
)
@Table(name="order_line_tier")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderLineTierDTO extends AbstractDescription implements Serializable{

    private int id;
    private OrderLineDTO orderLine;
    private Integer tierNumber;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal tierFrom;
    private BigDecimal tierTo;

    public OrderLineTierDTO() {

    }

    public OrderLineTierDTO(Integer id, OrderLineDTO orderLine, Integer tierNumber, BigDecimal quantity, BigDecimal price, BigDecimal amount, BigDecimal tierFrom, BigDecimal tierTo) {
        this.setId(id);
        this.setOrderLine(orderLine);
        this.setTierNumber(tierNumber);
        this.setQuantity(quantity);
        this.setPrice(price);
        this.setAmount(amount);
        this.setTierFrom(tierFrom);
        this.setTierTo(tierTo);
    }

    public OrderLineTierDTO(OrderLineDTO orderLine, Integer tierNumber, BigDecimal quantity, BigDecimal price, BigDecimal amount, BigDecimal tierFrom, BigDecimal tierTo) {
        this.setOrderLine(orderLine);
        this.setTierNumber(tierNumber);
        this.setQuantity(quantity);
        this.setPrice(price);
        this.setAmount(amount);
        this.setTierFrom(tierFrom);
        this.setTierTo(tierTo);
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="order_line_tier_GEN")
    @Column(name="id",unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_line_id", nullable=false)
    public OrderLineDTO getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLineDTO orderLine) {
        this.orderLine = orderLine;
    }

    @Column(name="tier_number",nullable=false)
    public Integer getTierNumber() {
        return tierNumber;
    }

    public void setTierNumber(Integer tierNumber) {
        this.tierNumber = tierNumber;
    }

    @Column(name="quantity",nullable=false, precision=17, scale=17)
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name="price",nullable=false, precision=17, scale=17)
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name="amount",nullable=false, precision=17, scale=17)
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name="tier_from",nullable=true, precision=17, scale=17)
    public BigDecimal getTierFrom() {
        return tierFrom;
    }

    public void setTierFrom(BigDecimal tierFrom) {
        this.tierFrom = tierFrom;
    }

    @Column(name="tier_to",nullable=true, precision=17, scale=17)
    public BigDecimal getTierTo() {
        return tierTo;
    }

    public void setTierTo(BigDecimal tierTo) {
        this.tierTo = tierTo;
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder("Order Line Tier: id = ").append(this.id).append(", orderLine = ")
                .append(this.orderLine.getId()).append(", tier number = ").append(this.tierNumber)
                .append(", quantity = ").append(this.quantity).append(", price = ").append(this.price)
                .append(", amount= ").append(this.amount)
                .append(",tier from = ").append(tierFrom);
        if (null == tierTo) {
            stringBuilder.append("+");
        }else
            stringBuilder.append(", tier to =").append(tierTo);
        return stringBuilder.toString();
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_ORDER_LINE_TIER;
    }
}
