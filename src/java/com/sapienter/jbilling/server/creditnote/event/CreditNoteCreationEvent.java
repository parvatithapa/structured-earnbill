package com.sapienter.jbilling.server.creditnote.event;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class CreditNoteCreationEvent implements Event {

	private final InvoiceDTO invoice;
	private final Integer entityId;
    
    public CreditNoteCreationEvent(InvoiceDTO invoice, Integer entityId) {
    	this.invoice = invoice;
    	this.entityId=entityId;
    }
    
	public String getName() {
		return "Credit Note Created Event";
	}

	public Integer getEntityId() {
		return this.entityId;
	}
	
	public InvoiceDTO getInvoice() {
		return this.invoice;
	}

}
