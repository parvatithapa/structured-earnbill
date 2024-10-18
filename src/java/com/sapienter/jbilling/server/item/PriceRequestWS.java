package com.sapienter.jbilling.server.item;

import java.math.BigDecimal;
import java.util.Date;

import javax.validation.constraints.NotNull;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonFormat;

@ToString
public class PriceRequestWS {
	@NotNull(message = "validation.error.null.user.id")
	private Integer userId;
	@NotNull(message = "validation.error.null.entity.id")
	private Integer entityId;
	private Integer orderId;
	private Integer planId;
	@NotNull(message = "validation.error.null.itemId")
	private Integer itemId;
	@NotNull(message = "validation.error.null.quantity")
	private BigDecimal quantity;
	@NotNull
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date eventDate;
	private String pricingFields;

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

	public String getPricingFields() {
		return pricingFields;
	}

	public void setPricingFields(String pricingFields) {
		this.pricingFields = pricingFields;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public Date getEventDate() {
		return eventDate;
	}

	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
}