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
import java.util.Comparator;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.CascadeType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;

/**
 * OrderLineUsagePoolDTO
 * The domain object representing the association of OrderLineDTO with 
 * CustomerUsagePoolDTO. This association keeps how much of free quantity 
 * was drawn from each available customer usage pool by a particular order line.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

@Entity
@TableGenerator(
        name="order_line_usage_pool_map_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="order_line_usage_pool_map",
        allocationSize = 10
        )
@Table(name="order_line_usage_pool_map")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderLineUsagePoolDTO implements Serializable{

	private int id;
	private OrderLineDTO orderLine;
	private BigDecimal quantity;
	private Date effectiveDate;
	private CustomerUsagePoolDTO customerUsagePool;
	
	private boolean isTouched = false;
	
	public OrderLineUsagePoolDTO() {

	}
	
	public OrderLineUsagePoolDTO(int id, OrderLineDTO orderLine, BigDecimal quantity) {
		this.setId(id);
		this.setOrderLine(orderLine);
		this.setQuantity(quantity);
	}
	
	public OrderLineUsagePoolDTO(int id, OrderLineDTO orderLine, BigDecimal quantity, CustomerUsagePoolDTO customerUsagePool, Date effectiveDate) {
		this.setId(id);
		this.setOrderLine(orderLine);
		this.setQuantity(quantity);
		this.setCustomerUsagePool(customerUsagePool);
		this.setEffectiveDate(effectiveDate);
	}
	
	public OrderLineUsagePoolDTO(OrderLineUsagePoolDTO other) {
		this.setId(other.getId());
		this.setOrderLine(other.getOrderLine());
		this.setQuantity(other.getQuantity());
		this.setCustomerUsagePool(other.getCustomerUsagePool());
	}
	
	@Id @GeneratedValue(strategy=GenerationType.TABLE, generator="order_line_usage_pool_map_GEN")
	@Column(name="id",unique=true, nullable=false)
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false)
	public OrderLineDTO getOrderLine() {
		return orderLine;
	}

	public void setOrderLine(OrderLineDTO orderLine) {
		this.orderLine = orderLine;
	}

	@Column(name="quantity", nullable=false)
	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_usage_pool_id", nullable=true)
	public CustomerUsagePoolDTO getCustomerUsagePool() {
		return this.customerUsagePool;
	}
	
	public void setCustomerUsagePool(CustomerUsagePoolDTO customerUsagePool) {
		this.customerUsagePool = customerUsagePool; 
	}
	
	@Column(name="effective_date", nullable=false, length=29)
	public Date getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	@Transient
	public boolean hasCustomerUsagePool() {
		return null != getCustomerUsagePool();
	}

	/**
     * Load all lazy dependencies of entity if needed
     */
    public void touch() {
        // touch entity only once
        if (isTouched) return;
        isTouched = true;

        getCustomerUsagePool();
        
        if (getOrderLine() != null) {
            getOrderLine().touch();
        }
    }
	
	@Override
	public String toString(){
		return "Order Line Usage Pool: id = "+id+", orderLine = "+orderLine.getId()+", quantity = "+quantity+", customerUsagePool= "+customerUsagePool;
	}
	
}

