package com.sapienter.jbilling.server.sapphire;

import lombok.ToString;

import com.sapienter.jbilling.server.system.event.Event;

@ToString
public final class SapphireSwapAssetEvent implements Event {

    private final Integer oldOrderId;
    private final Integer newOrderId;
    private final Integer oldAssetId;
    private final Integer newAssetId;
    private final Integer entityId;

    public SapphireSwapAssetEvent(Integer oldOrderId, Integer newOrderId,
            Integer oldAssetId, Integer newAssetId, Integer entityId) {
        this.oldOrderId = oldOrderId;
        this.newOrderId = newOrderId;
        this.oldAssetId = oldAssetId;
        this.newAssetId = newAssetId;
        this.entityId = entityId;
    }

    public Integer getOldOrderId() {
        return oldOrderId;
    }

    public Integer getNewOrderId() {
        return newOrderId;
    }

    public Integer getOldAssetId() {
        return oldAssetId;
    }

    public Integer getNewAssetId() {
        return newAssetId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "SapphireSwapAssetEvent-"+ getEntityId();
    }

}
