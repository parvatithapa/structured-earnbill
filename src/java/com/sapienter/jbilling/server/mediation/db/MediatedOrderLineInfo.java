package com.sapienter.jbilling.server.mediation.db;

import java.math.BigDecimal;

public class MediatedOrderLineInfo {
	
	private Integer processId;
	private Integer orderLineId;
	private BigDecimal quantity;
	private BigDecimal amount;
	
	public MediatedOrderLineInfo() {
		
	}
	
	public MediatedOrderLineInfo(Integer processId, Integer orderLineId) {
		this.processId = processId;
		this.orderLineId = orderLineId;
	}
	
	public Integer getProcessId() {
		return processId;
	}
	
	public void setProcessId(Integer processId) {
		this.processId = processId;
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
	
	public Integer getOrderLineId() {
		return orderLineId;
	}
	
	public void setOrderLineId(Integer orderLineId) {
		this.orderLineId = orderLineId;
	}

	@Override
	public String toString() {
		return "MediatedOrderLineInfo [processId=" + processId
				+ ", orderLineId=" + orderLineId + ", quantity=" + quantity
				+ ", amount=" + amount + "]";
	}
	
}
