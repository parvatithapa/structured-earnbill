package com.sapienter.jbilling.server.creditnote;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDTO;
import com.sapienter.jbilling.server.security.WSSecured;

public class CreditNoteWS implements WSSecured, Serializable{

	private Integer id;
	private Integer userId;
	private String type;
	private String balance;
	private String amount;
	private Integer creationInvoiceId = null;
	private Integer entityId;
	private Integer[] creditNoteLineIds;
	private Integer[] creditNoteInvoiceMapIds;
	private int deleted;
    private Date createDateTime;

	public CreditNoteWS(Integer id, Integer userId, String type, String balance, String amount,
			CreditNoteInvoiceMapDTO[] paidInvoices, Integer creationInvoiceId,
			CreditNoteLineDTO[] lines, Integer entityId, int deleted) {
		super();
		this.id = id;
		this.userId = userId;
		this.type = type;
		this.balance = balance;
		this.amount = amount;
		this.creditNoteLineIds = Arrays.asList(lines).stream().map(CreditNoteLineDTO::getId).toArray(Integer[]::new);
		this.creditNoteInvoiceMapIds = Arrays.asList(paidInvoices).stream().map(CreditNoteInvoiceMapDTO::getId).toArray(Integer[]::new);
		this.creationInvoiceId = creationInvoiceId;
		this.entityId = entityId;
		this.deleted = deleted;
	}

	public CreditNoteWS(){
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBalance() {
		return balance;
	}

	public void setBalance(String balance) {
		this.balance = balance;
	}

	public BigDecimal getBalanceAsDecimal() {
        return balance != null ? new BigDecimal(balance) : null;
	}

	public void setBalanceAsDecimal(BigDecimal balance) {
        setBalance(balance);
	}
	
	private void setBalance(BigDecimal balance) {
		this.balance = (balance != null ? balance.toString() : null);
	}

	public String getAmount() {
		return amount;
	}

	public BigDecimal getAmountAsDecimal() {
	    return amount != null ? new BigDecimal(amount) : null;
	}

	public void setAmountAsDecimal(BigDecimal amount) {
	    setAmount(amount);
	}
	
	public void setAmount(String amount) {
		this.amount = amount;
	}
	
	public void setAmount(BigDecimal amount) {
	    this.amount = (amount != null ? amount.toString() : null);
    }

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Integer getCreationInvoiceId() {
		return creationInvoiceId;
	}

	public void setCreationInvoiceId(Integer creationInvoiceId) {
		this.creationInvoiceId = creationInvoiceId;
	}

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public Integer getOwningEntityId() {
		return null;
	}

	public Integer getOwningUserId() {
		return userId;
	}
	
	public Integer[] getCreditNoteLineIds() {
		return creditNoteLineIds;
	}

	public void setCreditNoteLineIds(Integer[] creditNoteLineIds) {
		this.creditNoteLineIds = creditNoteLineIds;
	}

	public Integer[] getCreditNoteInvoiceMapIds() {
		return creditNoteInvoiceMapIds;
	}

	public void setCreditNoteInvoiceMapIds(Integer[] creditNoteInvoiceMapIds) {
		this.creditNoteInvoiceMapIds = creditNoteInvoiceMapIds;
	}

    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    @Override
	public String toString() {
		return "CreditNoteWS [id="+ id 
				+ ", userId=" + userId
				+ ", type=" + type
				+ ", balance=" + balance
				+ ", amount="+ amount
				+ ", creationInvoiceId="+ creationInvoiceId
				+ ", entityId=" + entityId
				+ ", creditNoteLineIds=" + creditNoteLineIds
				+ ", creditNoteInvoiceMapIds=" + creditNoteInvoiceMapIds
				+ ", deleted=" + deleted + "]";
	}
}
