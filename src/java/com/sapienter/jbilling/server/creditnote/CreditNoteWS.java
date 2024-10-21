package com.sapienter.jbilling.server.creditnote;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDTO;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@ApiModel(value = "Credit Note Data", description = "CreditNoteWS model")
public class CreditNoteWS implements WSSecured, Serializable{

	private Integer id;
	@NotNull(message="validation.error.notnull")
	private Integer userId;
	private String type;
	private String balance;
	@DecimalMin(value = "0.01", message = "validation.error.min,0.01")
	private String amount;
	private Integer creationInvoiceId = null;
	private Integer entityId;
	private Integer[] creditNoteLineIds;
	private Integer[] creditNoteInvoiceMapIds;
	private int deleted;
    private Date createDateTime;
	// Ad hoc credit note attributes
	@NotNull(message="validation.error.notnull")
	private Integer itemId;
	@Size(max = 500, message = "validation.error.size,0,500")
	private String description;
	private Date creditNoteDate;
	private String serviceId;
	private Integer subscriptionOrderId;
	@Size(max = 1000, message = "validation.error.size,0,1000")
	private String notes;

	public CreditNoteWS(Integer id, Integer userId, String type, String balance, String amount,
						CreditNoteInvoiceMapDTO[] paidInvoices, Integer creationInvoiceId, CreditNoteLineDTO[] lines,
						Integer entityId, int deleted, Integer itemId, String description, Date creditNoteDate, String serviceId,
						Integer subscriptionOrderId, String notes) {
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
		this.itemId = itemId;
		this.description = description;
		this.creditNoteDate = creditNoteDate;
		this.serviceId = serviceId;
		this.subscriptionOrderId = subscriptionOrderId;
		this.notes = notes;
	}

	public CreditNoteWS(){
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Identifier of the user this credit note belongs to", required = true)
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

	@ApiModelProperty(value = "Credit Note Amount", required = true)
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

	@ApiModelProperty(value = "Identifier of the credit note item", required = true)
	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	@ApiModelProperty(value = "You can pass a specific date while creating a note (Note: the date should not be in the future)")
	public Date getCreditNoteDate() {
		return creditNoteDate;
	}

	public void setCreditNoteDate(Date creditNoteDate) {
		this.creditNoteDate = creditNoteDate;
	}

	@ApiModelProperty(value = "Credit Note against which Service")
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	@ApiModelProperty(value = "Credit Note against which subscription order")
	public Integer getSubscriptionOrderId() {
		return subscriptionOrderId;
	}

	public void setSubscriptionOrderId(Integer subscriptionOrderId) {
		this.subscriptionOrderId = subscriptionOrderId;
	}

	@ApiModelProperty(value = "Credit Note Reason / Note")
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@ApiModelProperty(value = "Credit Note Description (Option) - The default system will populate the product description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	//TODO toString method should include new fiels? If so it is okay to show null ones?
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
