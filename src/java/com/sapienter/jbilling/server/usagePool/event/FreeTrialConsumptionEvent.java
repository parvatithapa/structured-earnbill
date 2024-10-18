package com.sapienter.jbilling.server.usagePool.event;

import com.sapienter.jbilling.server.system.event.Event;

public class FreeTrialConsumptionEvent implements Event {

    private final Integer entityId;
    private final Integer userId;

    public FreeTrialConsumptionEvent(Integer entityId, Integer userId) {
        this.entityId = entityId;
        this.userId = userId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "FreeTrialConsumptionEvent : entity=" + entityId;
    }

    public Integer getUserId() {
        return userId;
    }
    
    @Override
    public String toString() {
        return "FreeTrialConsumptionEvent [entityId=" + entityId + ", userId="
                + userId + "]";
    }

}
