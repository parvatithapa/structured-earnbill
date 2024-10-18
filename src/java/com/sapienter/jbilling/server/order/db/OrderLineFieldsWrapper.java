package com.sapienter.jbilling.server.order.db;

import java.math.BigDecimal;

public class OrderLineFieldsWrapper {
	
	private Integer itemId;
	private BigDecimal quantity;
	private BigDecimal amount;
	private BigDecimal price;
	private String description;
	private Long callCounter;
	
	public Integer getItemId() {
		return itemId;
	}
	
	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Long getCallCounter() {
		return callCounter;
	}
	
	public void setCallCounter(Long callCounter) {
		this.callCounter = callCounter;
	}
	
}
