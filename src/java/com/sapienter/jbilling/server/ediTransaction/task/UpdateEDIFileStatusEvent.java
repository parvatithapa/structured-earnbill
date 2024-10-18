package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by neeraj on 8/1/16.
 */
public class UpdateEDIFileStatusEvent implements Event {

    private Integer entityId;
    private EDIFileDTO ediFileDTO;
    private String escapeStatus;

    public EDIFileDTO getEdiFileDTO() {
        return ediFileDTO;
    }

    public UpdateEDIFileStatusEvent(Integer entityId, EDIFileDTO ediFileDTO, String status) {
        this.entityId = entityId;
        this.ediFileDTO = ediFileDTO;
        this.escapeStatus=status;
    }

    public String getName() {
        return "EDI Status Updation event";
    }

    public String getEscapeStatus() {
        return escapeStatus;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
