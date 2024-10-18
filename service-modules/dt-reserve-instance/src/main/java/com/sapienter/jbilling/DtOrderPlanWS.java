package com.sapienter.jbilling;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sapienter.jbilling.catalogue.DtPlanWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

@ApiModel(value = "DT Order PlanWS Data", description = "DtOrderPlanWS model")
@JsonPropertyOrder({"orderId", "orderStatus","planId","description", "currencyCode", "planPrice", "activeSince", "activeUntil",
                    "enProductName", "deProductName", "productCategory", "duration", "paymentMode", "productCurrencyCode", "productElasticPrice"})
public class DtOrderPlanWS extends DtPlanWS implements Serializable{
    private Integer orderId;
    private String orderStatus;

    @ApiModelProperty(value = "Current status of the order")
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    @ApiModelProperty(value = "Order Id of the reserve instance purchase")
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
}
