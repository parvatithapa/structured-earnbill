package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by neeraj on 12/02/16.
 */
public class NewEDIFileEvent implements Event {

    private Integer entityId;
    private EDIFileDTO ediFileDTO;

    public EDIFileDTO getEdiFileDTO() {
        return ediFileDTO;
    }

    public NewEDIFileEvent(Integer entityId, EDIFileDTO ediFileDTO) {
        this.entityId = entityId;
        this.ediFileDTO = ediFileDTO;
    }

    public String getName() {
        return "EDI Status Updation event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
