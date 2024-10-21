package com.sapienter.jbilling.server.order.db;

import java.math.BigDecimal;
import java.math.MathContext;

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

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;

@Entity
@DynamicUpdate(true)
@TableGenerator(
        name            = "order_line_itemized_usage_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "order_line_itemized_usage"
        )
@Table(name="order_line_itemized_usage")
public class OrderLineItemizedUsageDTO {

    private Integer id;
    private OrderLineDTO orderLine;
    private String separator;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal quantity = BigDecimal.ZERO;


    public OrderLineItemizedUsageDTO() { }
    public OrderLineItemizedUsageDTO(OrderLineDTO orderLine, String separator) {
        this.orderLine = orderLine;
        this.separator = separator;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "order_line_itemized_usage_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_line_id", nullable = false)
    public OrderLineDTO getOrderLine() {
        return orderLine;
    }

    @Column(name = "separator", nullable = false)
    public String getSeparator() {
        return separator;
    }

    @Column(name = "amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "quantity", nullable = false, precision = 17, scale = 17)
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setOrderLine(OrderLineDTO orderLine) {
        this.orderLine = orderLine;
    }
    public void setSeparator(String separator) {
        this.separator = separator;
    }
    @Transient
    public void addAmountAndQuantity(BigDecimal amount, BigDecimal quantity) {
        Assert.notNull(amount, "Amount can not be null");
        Assert.notNull(quantity, "quantity can not be null");
        this.amount  = this.amount.add(amount, MathContext.DECIMAL128);
        this.quantity  = this.quantity.add(quantity, MathContext.DECIMAL128);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OrderLineItemizedUsageDTO [id=");
        builder.append(id);
        builder.append(", orderLine=");
        builder.append(orderLine);
        builder.append(", separator=");
        builder.append(separator);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", quantity=");
        builder.append(quantity);
        builder.append("]");
        return builder.toString();
    }
}
