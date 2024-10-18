package com.sapienter.jbilling.server.sapphire;

import lombok.ToString;

import com.sapienter.jbilling.server.system.event.Event;

@ToString
public final class NewSaleEvent implements Event {

    private final Integer orderId;
    private final Integer entityId;
    private final NewSaleRequestWS newSaleRequest;

    public NewSaleEvent(Integer orderId, Integer entityId, NewSaleRequestWS newSaleRequest) {
        this.orderId = orderId;
        this.entityId = entityId;
        this.newSaleRequest = newSaleRequest;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "NewSaleEvent";
    }

    public Integer getOrderId() {
        return orderId;
    }

    public NewSaleRequestWS getNewSaleRequest() {
        return newSaleRequest;
    }

}
