package com.sapienter.jbilling.server.sapphire;

import java.io.Serializable;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ToString
public class ChangeOfPlanRequestWS implements Serializable {

    private final Integer orderId;
    private final String existingPlanCode;
    private final String newPlanCode;

    @JsonCreator
    public ChangeOfPlanRequestWS(@JsonProperty("orderId") Integer orderId,
            @JsonProperty("existingPlanCode") String existingPlanCode,
            @JsonProperty("newPlanCode") String newPlanCode) {
        this.orderId = orderId;
        this.existingPlanCode = existingPlanCode;
        this.newPlanCode = newPlanCode;
    }

    @ApiModelProperty(value = "Order Id", required = true)
    public Integer getOrderId() {
        return orderId;
    }

    @ApiModelProperty(value = "existingPlanCode", required = true)
    public String getExistingPlanCode() {
        return existingPlanCode;
    }

    @ApiModelProperty(value = "newPlanCode", required = true)
    public String getNewPlanCode() {
        return newPlanCode;
    }

}
