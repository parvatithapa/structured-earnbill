package com.sapienter.jbilling.resources;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class OrderMetaFieldValueWS implements Serializable {

    @NotNull(message = "validation.error.notnull")
    private Integer orderId;
    @NotNull(message = "validation.error.notnull")
    private Map<String, String> metaFieldValues = new HashMap<>();

    @JsonCreator
    public OrderMetaFieldValueWS(@JsonProperty("orderId")Integer orderId,
            @JsonProperty("metaFieldValues") Map<String, String> metaFieldValues) {
        this.orderId = orderId;
        this.metaFieldValues = metaFieldValues;
    }

    @ApiModelProperty(value = "order metaFieldsValues map", required = true)
    public Map<String, String> getMetaFieldValues() {
        return metaFieldValues;
    }

    @ApiModelProperty(value = "order id", required = true)
    public Integer getOrderId() {
        return orderId;
    }
}
