package com.sapienter.jbilling.server.creditnote.db;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

@Entity
@TableGenerator(
        name            = "credit_note_invoice_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "credit_note_invoice",
        allocationSize  = 100)
@Table(name = "credit_note_invoice_map")
public class CreditNoteInvoiceMapDTO {
    private int id;
    private CreditNoteDTO creditNote;
    private InvoiceDTO invoiceEntity;
    private BigDecimal amount;
    private Date createDatetime;
    private int versionNum;

	public CreditNoteInvoiceMapDTO(CreditNoteDTO creditNote, InvoiceDTO invoiceEntity, BigDecimal amount, Date createDatetime, int versionNum) {
		super();
		this.creditNote = creditNote;
		this.invoiceEntity = invoiceEntity;
		this.amount = amount;
		this.createDatetime = createDatetime;
		this.versionNum = versionNum;
	}

	public CreditNoteInvoiceMapDTO() {
	}

	@Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "credit_note_invoice_GEN")
    @Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id")
	public CreditNoteDTO getCreditNote() {
		return creditNote;
	}

	public void setCreditNote(CreditNoteDTO creditNote) {
		this.creditNote = creditNote;
	}
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
	public InvoiceDTO getInvoiceEntity() {
		return invoiceEntity;
	}

	public void setInvoiceEntity(InvoiceDTO invoiceEntity) {
		this.invoiceEntity = invoiceEntity;
	}
	@Column(name = "amount", precision = 17, scale = 17)
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
    @Column(name = "create_datetime", nullable = false, length = 29)
	public Date getCreateDatetime() {
		return createDatetime;
	}

	public void setCreateDatetime(Date createDatetime) {
		this.createDatetime = createDatetime;
	}

	@Version
    @Column(name="OPTLOCK")
	public int getVersionNum() {
		return versionNum;
	}

	public void setVersionNum(int versionNum) {
		this.versionNum = versionNum;
	}

	@Override
	public String toString() {
		return "CreditNoteInvoiceMapDTO [id=" + id + ", creditNote="
				+ creditNote + ", invoiceEntity=" + invoiceEntity + ", amount="
				+ amount + ", createDatetime=" + createDatetime
				+ ", versionNum=" + versionNum + "]";
	}


}
