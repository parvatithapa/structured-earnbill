package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by Taimoor Choudhary on 9/20/17.
 */
public class IgnitionOrderStatusEvent implements Event {

    private Integer entityId;
    private Integer userId;
    private Integer statusId;
    private Integer orderId;

    public Integer getUserId() {
        return userId;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public IgnitionOrderStatusEvent(Integer entityId, Integer userId, Integer statusId, Integer orderId) {

        this.entityId = entityId;
        this.userId = userId;
        this.statusId = statusId;
        this.orderId = orderId;
    }

    @Override
    public String getName() {
        return "Ignition Order Status Event";
    }

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }
}
