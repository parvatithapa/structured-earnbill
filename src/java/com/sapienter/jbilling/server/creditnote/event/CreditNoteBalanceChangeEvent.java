package com.sapienter.jbilling.server.creditnote.event;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.system.event.Event;

public class CreditNoteBalanceChangeEvent implements Event {

	private final Integer creditNoteId;
	private final Integer entityId;
	private final BigDecimal oldBalance;
	private final BigDecimal newBalance;
    
	public CreditNoteBalanceChangeEvent(Integer entityId, Integer creditNoteId, BigDecimal oldBalance, BigDecimal newBalance){
	   	this.creditNoteId = creditNoteId;
	   	this.entityId = entityId;
	   	this.oldBalance = oldBalance;
	   	this.newBalance = newBalance;
	}
	
	public String getName() {
		return "Credit Note Balance Change Event";
	}
		
	public Integer getCreditNoteId() {
		return this.creditNoteId;
	}

	public Integer getEntityId() {
		return this.entityId;
	}
	
	public BigDecimal getOldBalance() {
		return this.oldBalance;
	}
	
	public BigDecimal getNewBalance() {
		return this.newBalance;
	}

	
}
