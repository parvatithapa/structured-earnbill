package com.sapienter.jbilling.server.process.event;

import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by neeraj on 28/10/15.
 */
public class SureAddressEvent implements Event {
    private final Integer entityId;
    private CustomizedEntity entity;

    public SureAddressEvent(Integer entityId, CustomizedEntity entity){
        this.entityId=entityId;
        this.entity=entity;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Fine Sure address";
    }

    public CustomizedEntity getEntity() {
        return entity;
    }
}
