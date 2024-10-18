package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by vivek on 18/9/15.
 */
public class EDIStatusUpdatedEvent implements Event {

    private final Integer entityId;
    private final EDIFileDTO fileDTO;

    public EDIStatusUpdatedEvent(Integer entityId, EDIFileDTO fileDTO) {
        this.entityId = entityId;
        this.fileDTO = fileDTO;
    }

    public String getName() {
        return "EDI status update event.";
    }

    public final Integer getEntityId() {
        return entityId;
    }
}
