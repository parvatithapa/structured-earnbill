package com.sapienter.jbilling.server.sapphire;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("serial")
public class SwapAssetResponse implements Serializable {

    private Integer updatedOrder;
    private Integer[] createdOrders;

    @JsonCreator
    public SwapAssetResponse(@JsonProperty(value = "updatedOrder") Integer updatedOrder,
            @JsonProperty(value = "createdOrders") Integer[] createdOrders) {
        this.updatedOrder = updatedOrder;
        this.createdOrders = createdOrders;
    }

    public Integer getUpdatedOrder() {
        return updatedOrder;
    }

    public Integer[] getCreatedOrders() {
        return createdOrders;
    }

}
