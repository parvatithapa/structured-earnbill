package com.sapienter.jbilling.server.item;

import java.math.BigDecimal;
import java.util.Date;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonFormat;

@ToString
public class PriceResponseWS {

	private BigDecimal resolvedPrice;
	private BigDecimal freeQuantity;
	private BigDecimal quantity;
	private Integer userId;
	private Integer entityId;
	private Integer orderId;
	private Integer planId;
	private Integer itemId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date eventDate;

	public BigDecimal getResolvedPrice() {
		return resolvedPrice;
	}

	public void setResolvedPrice(BigDecimal resolvedPrice) {
		this.resolvedPrice = resolvedPrice;
	}

	public BigDecimal getFreeQuantity() {
		return freeQuantity;
	}

	public void setFreeQuantity(BigDecimal freeQuantity) {
		this.freeQuantity = freeQuantity;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public Integer getPlanId() {
		return planId;
	}

	public void setPlanId(Integer planId) {
		this.planId = planId;
	}

	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	public Date getEventDate() {
		return eventDate;
	}

	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
}