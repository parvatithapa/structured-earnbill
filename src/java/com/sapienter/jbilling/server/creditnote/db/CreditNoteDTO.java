package com.sapienter.jbilling.server.creditnote.db;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.hibernate.annotations.OrderBy;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name = "credit_note_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "credit_note",
        allocationSize = 100)
@Table(name="credit_note")
public class CreditNoteDTO extends AbstractDescription implements Exportable {
	private int id;
	private Integer entityId;
	private CreditType creditType;
	private BigDecimal balance;
	private BigDecimal amount;
	private Set <CreditNoteInvoiceMapDTO> paidInvoices = new HashSet<>();
	private InvoiceDTO creationInvoice;
	private Set<CreditNoteLineDTO> lines;
	private int deleted;
	private Date createDateTime;
	// Ad hoc credit note fields
	private Date creditNoteDate;
	private UserDTO user;
	private String serviceId;
	private OrderDTO subscriptionOrder;
	private String notes;

	public CreditNoteDTO() {
	}

	//Copy constructor Shallow Copy
	public CreditNoteDTO(CreditNoteDTO creditNoteDTO){
		this.id = creditNoteDTO.getId();
		this.entityId=creditNoteDTO.getEntityId();
		this.creditType=creditNoteDTO.getCreditType();
		this.balance = creditNoteDTO.getBalance();
		this.amount = creditNoteDTO.getAmount();
		this.paidInvoices = creditNoteDTO.getPaidInvoices();
		this.creationInvoice = creditNoteDTO.getCreationInvoice();
		this.lines = creditNoteDTO.getLines();
		this.deleted =creditNoteDTO.getDeleted();
	}

	@Enumerated(EnumType.STRING)
    @Column(name = "credit_type", nullable = false, length = 50)
	public CreditType getCreditType() {
		return this.creditType;
	}

	public void setCreditType(CreditType creditType) {
		this.creditType = creditType;
	}

	@Column(name="balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name="amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "creditNote")
	public Set<CreditNoteInvoiceMapDTO> getPaidInvoices() {
		return paidInvoices;
	}

	public void setPaidInvoices(Set<CreditNoteInvoiceMapDTO> paidInvoices) {
		this.paidInvoices = paidInvoices;
	}

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "creation_invoice_id", nullable = false)
	public InvoiceDTO getCreationInvoice() {
		return creationInvoice;
	}

	public void setCreationInvoice(InvoiceDTO creationInvoice) {
		this.creationInvoice = creationInvoice;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "credit_note_GEN")
    @Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}

	@Override
    @Transient
	protected String getTable() {
		return Constants.TABLE_CREDIT_NOTE;
	}

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="creditNoteDTO")
	@OrderBy(clause="id")
	public Set<CreditNoteLineDTO> getLines() {
		return lines;
	}

	public void setLines(Set<CreditNoteLineDTO> lines) {
		this.lines = lines;
	}

	@Column(name = "deleted", nullable = false)
	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	@Column(name="entity_id")
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	@Column(name="create_datetime")
	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	@Column(name="credit_note_date")
	public Date getCreditNoteDate() {
		return creditNoteDate;
	}

	public void setCreditNoteDate(Date creditNoteDate) {
		this.creditNoteDate = creditNoteDate;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	public UserDTO getUser() {
		return user;
	}

	public void setUser(UserDTO user) {
		this.user = user;
	}

	@Column(name="service_id")
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_order_id")
	public OrderDTO getSubscriptionOrder() {
		return subscriptionOrder;
	}

	public void setSubscriptionOrder(OrderDTO subscriptionOrder) {
		this.subscriptionOrder = subscriptionOrder;
	}

	@Column(name = "notes", length = 1000)
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Override
	public String toString() {
		return "CreditNoteDTO [id=" + id + ", balance=" + balance + ", amount="
				+ amount + ", paidInvoices=" + paidInvoices
				+ ", creationInvoice=" + creationInvoice + "]";
	}

    @Override
    @Transient
    public String[] getFieldNames() {
        return new String[] {
                "id",
                "userId",
                "userName",
                "creationInvoice",
                "linkedInvoices",
                "amount",
                "balance",
        };
    }

    @Override
    @Transient
    public Object[][] getFieldValues() {
        StringBuilder creditNoteIds = new StringBuilder();
        for (Iterator<CreditNoteInvoiceMapDTO> it = paidInvoices.iterator(); it.hasNext();) {
            creditNoteIds.append(it.next().getInvoiceEntity().getId());
            if (it.hasNext()) creditNoteIds.append(", ");
        }

        return new Object[][] {
                {
                        id,
                        (creationInvoice.getBaseUser() != null ? creationInvoice.getBaseUser().getId() : null),
                        (creationInvoice.getBaseUser() != null ? creationInvoice.getBaseUser().getUserName() : null),
                        creationInvoice.getId(),
                        creditNoteIds.toString(),
                        amount,
                        balance,
                }
        };
    }

	@Transient
	public String getCurrencySymbol(){
		if(creationInvoice != null && creationInvoice.getCurrency() != null){
			return creationInvoice.getCurrency().getSymbol();
		} else if (user != null && user.getCurrency() != null) {
			user.getCurrency().getSymbol();
		}

		return null;
	}

	@Transient
	public boolean hasBalance() {
	    return this.balance.compareTo(BigDecimal.ZERO) > 0 ;
	}
}