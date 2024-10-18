package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by faizan on 5/16/17.
 */
public class AssetStatusUpdateEvent implements Event {
    private final Integer executorId;
    private final OrderChangeWS[] newOrderChanges;
    private final OrderChangeWS[] oldOrderChanges;
    private final Integer entityId;

    public AssetStatusUpdateEvent(Integer executorId, OrderChangeWS[] newOrderChanges, OrderChangeWS[] oldOrderChanges, Integer entityId) {
        this.executorId = executorId;
        this.newOrderChanges = newOrderChanges;
        this.oldOrderChanges = oldOrderChanges;
        this.entityId = entityId;
    }

    public Integer getExecutorId() {
        return executorId;
    }

    public OrderChangeWS[] getNewOrderChanges() {
        return newOrderChanges;
    }

    public OrderChangeWS[] getOldOrderChanges() {
        return oldOrderChanges;
    }

    @Override
    public String getName() {
        return "AssetStatusUpdateEvent";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
