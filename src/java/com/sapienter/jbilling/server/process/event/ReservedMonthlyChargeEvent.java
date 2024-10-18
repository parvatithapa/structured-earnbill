package com.sapienter.jbilling.server.process.event;

import com.sapienter.jbilling.server.system.event.Event;

public class ReservedMonthlyChargeEvent implements Event {

    private Integer entityId;

    public ReservedMonthlyChargeEvent(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {

        return "Reserved  Monthly Charged Event - entity " + entityId;

    }


}
