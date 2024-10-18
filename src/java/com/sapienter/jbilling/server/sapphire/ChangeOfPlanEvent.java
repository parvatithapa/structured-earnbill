package com.sapienter.jbilling.server.sapphire;

import lombok.ToString;

import com.sapienter.jbilling.server.system.event.Event;

@ToString
public class ChangeOfPlanEvent implements Event {

    private final Integer orderId;
    private final String existingPlanCode;
    private final String newPlanCode;
    private final Integer entityId;
    private Integer newOrderId;

    public ChangeOfPlanEvent(Integer orderId, String existingPlanCode,
            String newPlanCode, Integer entityId) {
        this.orderId = orderId;
        this.existingPlanCode = existingPlanCode;
        this.newPlanCode = newPlanCode;
        this.entityId = entityId;
    }

    public Integer getNewOrderId() {
        return newOrderId;
    }

    public void setNewOrderId(Integer newOrderId) {
        this.newOrderId = newOrderId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public String getExistingPlanCode() {
        return existingPlanCode;
    }

    public String getNewPlanCode() {
        return newPlanCode;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "ChangeOfPlanEvent";
    }

}
