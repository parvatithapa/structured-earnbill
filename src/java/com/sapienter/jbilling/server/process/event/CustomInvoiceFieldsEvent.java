package com.sapienter.jbilling.server.process.event;

import java.util.Map;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.ContactDTOEx;
/**
 * This event is triggered while invoice generation
 * to handle the Full Creative custom invoice specific parameters.
 *   
 * @author Mahesh Shivarkar
 * @since  24-06-2016
 */
public class CustomInvoiceFieldsEvent implements Event {
	
	private final Integer entityId;
	private final Integer userId;
	private final Map<String, Object> messageParameters;
	private final String design;
	private final ContactDTOEx to;
	private final InvoiceDTO invoice;
	
	public CustomInvoiceFieldsEvent(Integer entityId, Integer userId,
			Map<String, Object> messageParameters, String design,
			ContactDTOEx to, InvoiceDTO invoice) {
		super();
		this.entityId = entityId;
		this.userId = userId;
		this.messageParameters = messageParameters;
		this.design = design;
		this.to = to;
		this.invoice = invoice;
	}

	@Override
	public String getName() {
		return "Custom Invoice Fields Event";
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	public Integer getUserId() {
		return userId;
	}

	public Map<String, Object> getParameters() {
		return messageParameters;
	}
	
	public String getDesign() {
		return design;
	}
	
	public ContactDTOEx getTo() {
		return to;
	}

	public InvoiceDTO getInvoice() {
		return invoice;
	}
}
