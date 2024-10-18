package com.sapienter.jbilling.server.creditnote.event;

import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class CreditNoteDeletedEvent implements Event {
	
	private final CreditNoteDTO creditNote;
    private final Integer entityId;
    
	public CreditNoteDeletedEvent(Integer entityId, CreditNoteDTO creditNote){
	   	this.creditNote=creditNote;
	   	this.entityId=entityId;
	}
	public String getName() {
		return "Credit Note Deleted Event";
	}
		
	public CreditNoteDTO getCreditNoteDTO() {
		return this.creditNote;
	}

	public Integer getEntityId() {
		return this.entityId;
	}

}
