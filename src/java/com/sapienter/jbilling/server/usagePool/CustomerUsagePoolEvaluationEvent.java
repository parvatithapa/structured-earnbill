package com.sapienter.jbilling.server.usagePool;

import java.util.Date;

import com.sapienter.jbilling.server.system.event.Event;

public class CustomerUsagePoolEvaluationEvent implements Event {

    private final Integer customerUsagePoolId;
    private final Date runDate;
    private final Integer entityId;

    public CustomerUsagePoolEvaluationEvent(Integer customerUsagePoolId, Integer entityId, Date runDate) {
        this.customerUsagePoolId = customerUsagePoolId;
        this.entityId = entityId;
        this.runDate = runDate;
    }

    public Integer getCustomerUsagePoolId() {
        return customerUsagePoolId;
    }

    public Date getRunDate() {
        return new Date(runDate.getTime());
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "CustomerUsagePoolEvaluationEvent-"+ getEntityId();
    }
}
