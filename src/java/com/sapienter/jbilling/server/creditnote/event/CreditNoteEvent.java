package com.sapienter.jbilling.server.creditnote.event;

import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class CreditNoteEvent implements Event {
	
	private final CreditNoteDTO creditNote;
    private final Integer entityId;
    
	public CreditNoteEvent(Integer entityId, CreditNoteDTO creditNote){
	   	this.creditNote=creditNote;
	   	this.entityId=entityId;
	}
	public String getName() {
		return "Credit Note Deleted";
	}
		
	public CreditNoteDTO getCreditNoteDTO() {
		return creditNote;
	}

	public Integer getEntityId() {
		return entityId;
	}
	
}
