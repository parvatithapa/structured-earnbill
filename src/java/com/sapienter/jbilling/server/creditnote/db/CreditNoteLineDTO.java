package com.sapienter.jbilling.server.creditnote.db;

import java.math.BigDecimal;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;

@Entity
@TableGenerator(
        name="credit_note_line_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="credit_note_line",
        allocationSize = 100
        )
@Table(name="credit_note_line")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditNoteLineDTO {

	private Integer id;
	private String description; //(description of the invoice line from which this credit note line was created)
	private BigDecimal amount;
	private InvoiceLineDTO creationInvoiceLine;
	private CreditNoteDTO creditNoteDTO;
	private int deleted;

	@Id @GeneratedValue(strategy=GenerationType.TABLE, generator="credit_note_line_GEN")
	@Column(name="id", unique=true, nullable=false)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	@Column(name="description", length=1000)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "amount", nullable = false, precision = 17, scale = 17)
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice_line_id")
	public InvoiceLineDTO getCreationInvoiceLine() {
		return creationInvoiceLine;
	}

	public void setCreationInvoiceLine(InvoiceLineDTO creationInvoiceLine) {
		this.creationInvoiceLine = creationInvoiceLine;
	}

	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name = "credit_note_id")
	public CreditNoteDTO getCreditNoteDTO() {
		return creditNoteDTO;
	}

	public void setCreditNoteDTO(CreditNoteDTO creditNoteDTO) {
		this.creditNoteDTO = creditNoteDTO;
	}

	@Column(name="deleted")
	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

}
