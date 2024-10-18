package com.sapienter.jbilling.server.sapphire;

import java.io.Serializable;
import java.util.Date;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ToString
public final class NewSaleRequestWS implements Serializable {

    private final Integer userId;
    private final Integer planOrderId;
    private final ProductDetailWS[] productDetails;
    private final Date startDate;
    private final OrderPeriod orderPeriod;

    @JsonCreator
    public NewSaleRequestWS(@JsonProperty(value = "userId", required = true) Integer userId,
            @JsonProperty(value = "planOrderId", required = true) Integer planOrderId,
            @JsonProperty("productDetails") ProductDetailWS[] productDetails,
            @JsonProperty("startDate") Date startDate,
            @JsonProperty(value = "orderPeriod", required = true) OrderPeriod orderPeriod) {
        this.userId = userId;
        this.planOrderId = planOrderId;
        this.productDetails = productDetails;
        this.startDate = startDate;
        this.orderPeriod = orderPeriod;
    }

    @ApiModelProperty(value = "user id.", required = true)
    public Integer getUserId() {
        return userId;
    }

    @ApiModelProperty(value = "ProductDetails", required = true)
    public ProductDetailWS[] getProductDetails() {
        return productDetails;
    }

    public Date getStartDate() {
        return startDate;
    }

    @ApiModelProperty(value = "orderPeriod", required = true)
    public OrderPeriod getOrderPeriod() {
        return orderPeriod;
    }

    @ApiModelProperty(value = "plan order id.")
    public Integer getPlanOrderId() {
        return planOrderId;
    }
}
