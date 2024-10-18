package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by hitesh on 24/2/16.
 */
public class UpdateEDIFileEvent implements Event {

    private Integer entityId;
    private EDIFileDTO ediFileDTO;

    public EDIFileDTO getEdiFileDTO() {
        return ediFileDTO;
    }

    public UpdateEDIFileEvent(Integer entityId, EDIFileDTO ediFileDTO) {
        this.entityId = entityId;
        this.ediFileDTO = ediFileDTO;
    }

    public String getName() {
        return "EDI File Updation event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
