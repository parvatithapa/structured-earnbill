package com.sapienter.jbilling.server.item.event;

import java.util.Map;

import com.sapienter.jbilling.server.system.event.Event;

public class SwapAssetsEvent implements Event {

    private Integer orderId;
    private Map<Integer, Integer> oldNewAssetMap;
    private Integer entityId;

    public SwapAssetsEvent(Integer orderId, Map<Integer, Integer> oldNewAssetMap, Integer entityId) {
        this.orderId = orderId;
        this.entityId = entityId;
        this.oldNewAssetMap = oldNewAssetMap;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "SwapAssetsEvent";
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Map<Integer, Integer> getOldNewAssetMap() {
        return oldNewAssetMap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SwapAssetsEvent [orderId=");
        builder.append(orderId);
        builder.append(", oldNewAssetMap=");
        builder.append(oldNewAssetMap);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append("]");
        return builder.toString();
    }

}
