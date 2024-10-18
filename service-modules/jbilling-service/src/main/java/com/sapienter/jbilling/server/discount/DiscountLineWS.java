package com.sapienter.jbilling.server.discount;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@ApiModel(value = "Discount line data", description = "DiscountLineWS model")
public class DiscountLineWS implements Serializable {

	private Integer id;
	@NotNull(message = "validation.error.null.discount")
	private Integer discountId;
	private Integer orderId;
	private Integer planItemId;
	private Integer itemId;
	private Integer discountOrderLineId;
	
	private String orderLineAmount;		// this line amount will be used for product level discounts
	private String description;			// discount line description to be used in invoice
	
	// Starts from 1. Identifies the sorting order on UI and 
	// used for removing discountline from conversation.order
	private Integer discountLineIndex;
	private String lineLevelDetails;
	private String discountAmount;
	
	public DiscountLineWS() {
		
	}
	
	public DiscountLineWS(Integer id, Integer discountId, Integer orderId, Integer planItemId, Integer itemId) {
		setId(id);
		setDiscountId(discountId);
		setOrderId(orderId);
		setPlanItemId(planItemId);
		setItemId(itemId);
	}

	@ApiModelProperty(value = "Unique identifier of the discount line", required = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Unique identifier of the discount applied to the line")
	public Integer getDiscountId() {
		return discountId;
	}

	public void setDiscountId(Integer discountId) {
		this.discountId = discountId;
	}

	@ApiModelProperty(value = "Unique identifier of the order of which the discount line is part")
	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	@ApiModelProperty(value = "Unique identifier of the plan item part of the discount line")
	public Integer getPlanItemId() {
		return planItemId;
	}

	public void setPlanItemId(Integer planItemId) {
		this.planItemId = planItemId;
	}

	@ApiModelProperty(value = "Unique identifier of the item part of the discount line")
	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	@ApiModelProperty(value = "Unique identifier of the order line of which the discount line is part of")
	public Integer getDiscountOrderLineId() {
		return discountOrderLineId;
	}

	public void setDiscountOrderLineId(Integer discountOrderLineId) {
		this.discountOrderLineId = discountOrderLineId;
	}

	@JsonIgnore
	public String getOrderLineAmount() {
		return orderLineAmount;
	}

	@ApiModelProperty(value = "The total amount of the order line of which the discount line is part of")
	@JsonProperty("orderLineAmount")
	public BigDecimal getOrderLineAmountAsDecimal() {
		return Util.string2decimal(orderLineAmount);
	}

	@JsonIgnore
	public void setOrderLineAmount(String orderLineAmount) {
		this.orderLineAmount = orderLineAmount;
	}

	@JsonProperty("orderLineAmount")
	public void setOrderLineAmount(BigDecimal orderLineAmount) {
		this.orderLineAmount = (null != orderLineAmount ? orderLineAmount.toString() : null);
	}

	@ApiModelProperty(value = "Description of the discount line used in the invoice")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonIgnore
	public Integer getDiscountLineIndex() {
		return discountLineIndex;
	}

	public void setDiscountLineIndex(Integer discountLineIndex) {
		this.discountLineIndex = discountLineIndex;
	}

	@JsonIgnore
	public String getLineLevelDetails() {
		return lineLevelDetails;
	}

	public void setLineLevelDetails(String lineLevelDetails) {
		this.lineLevelDetails = lineLevelDetails;
	}

	@JsonIgnore
	public boolean hasItem() {
		return getItemId() != null && getItemId() > 0;
	}

	@JsonIgnore
	public boolean hasPlanItem() {
		return getPlanItemId() != null && getPlanItemId() > 0;
	}

	@JsonIgnore
	public boolean isPlanItemLevelDiscount() {
		return (hasPlanItem() && !hasItem());
	}

	@JsonIgnore
	public boolean isProductLevelDiscount() {
		return (!hasPlanItem() && hasItem());
	}

	@JsonIgnore
	public boolean isOrderLevelDiscount() {
		return (!hasPlanItem() && !hasItem());
	}

    /**
     * @return the discountAmount
     */
    @JsonIgnore
    public String getDiscountAmount() {
        return discountAmount;
    }

	@ApiModelProperty(value = "The total discount amount")
	@JsonProperty("discountAmount")
    public BigDecimal getDiscountAmountAsDecimal() {
        return Util.string2decimal(discountAmount);
    }
    
    /**
     * @param discountAmount the discountAmount to set
     */
    @JsonIgnore
    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    @JsonProperty("discountAmount")
    public void setDiscountAmount(BigDecimal discountAmount) {
		this.discountAmount = (null != discountAmount ? discountAmount.toString() : null);
	}

	@Override
	public String toString() {
		return String
				.format("DiscountLineWS [id=%s, discountId=%s, orderId=%s, planItemId=%s, itemId=%s, discountOrderLineId=%s, orderLineAmount=%s, description=%s, discountLineIndex=%s, lineLevelDetails=%s, discountAmount=%s]",
						id, discountId, orderId, planItemId, itemId,
						discountOrderLineId, orderLineAmount, description,
						discountLineIndex, lineLevelDetails, discountAmount);
	}    
	
}
