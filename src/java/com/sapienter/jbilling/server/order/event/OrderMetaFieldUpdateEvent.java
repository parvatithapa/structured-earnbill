package com.sapienter.jbilling.server.order.event;

import java.util.Map;

import org.springframework.util.Assert;

import com.google.common.collect.MapDifference.ValueDifference;
import com.sapienter.jbilling.server.system.event.Event;

public class OrderMetaFieldUpdateEvent implements Event {

    private Integer orderId;
    private Map<String, ValueDifference<Object>> diffMap;
    private Integer entityId;


    public OrderMetaFieldUpdateEvent(Integer orderId, Integer entityId, Map<String, ValueDifference<Object>> diffMap) {
        Assert.notNull(orderId, "enter orderId parameter");
        Assert.notNull(entityId, "enter entityId parameter");
        Assert.notNull(diffMap, "enter diffMap parameter");
        this.orderId = orderId;
        this.entityId = entityId;
        this.diffMap = diffMap;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Map<String, ValueDifference<Object>> getDiffMap() {
        return diffMap;
    }

    @Override
    public String getName() {
        return "OrderMetaFieldUpdateEvent";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OrderMetaFieldUpdateEvent [orderId=");
        builder.append(orderId);
        builder.append(", diffMap=");
        builder.append(diffMap);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append("]");
        return builder.toString();
    }
}
