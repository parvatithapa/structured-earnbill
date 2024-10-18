package com.sapienter.jbilling.server.usagePool.event;

import com.sapienter.jbilling.server.system.event.Event;

public class ReserveCacheEvent implements Event{

    private String key;
    private Integer entityID;

    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return "Reserve Cache cleanup Event";
    }

    @Override
    public Integer getEntityId() {
        return entityID;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setEntityID(Integer entityID) {
        this.entityID = entityID;
    }
}
