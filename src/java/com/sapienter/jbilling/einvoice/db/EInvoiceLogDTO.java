package com.sapienter.jbilling.einvoice.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="e_invoice_log")
public class EInvoiceLogDTO {

	private long id;
	private Integer invoiceId;
	private String eInvoiceRequestpayload;
	private String eInvoiceResponse;
	private Status status;
	private String irn;
	private Date createdAt = new Date();

	public EInvoiceLogDTO() {}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_invoice_log_generator")
	@SequenceGenerator(name = "e_invoice_log_generator", sequenceName = "e_invoice_log_seq", allocationSize = 1)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "invoice_id", nullable = false)
	public Integer getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(Integer invoiceId) {
		this.invoiceId = invoiceId;
	}

	@Column(name = "invoice_payload", nullable = false)
	public String geteInvoiceRequestpayload() {
		return eInvoiceRequestpayload;
	}

	public void seteInvoiceRequestpayload(String eInvoiceRequestpayload) {
		this.eInvoiceRequestpayload = eInvoiceRequestpayload;
	}

	@Column(name = "invoice_response", nullable = false)
	public String geteInvoiceResponse() {
		return eInvoiceResponse;
	}

	public void seteInvoiceResponse(String eInvoiceResponse) {
		this.eInvoiceResponse = eInvoiceResponse;
	}

	@Column(name = "create_datetime", nullable = false)
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Enumerated(value = EnumType.STRING)
	@Column(name = "status", nullable = false)
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Column(name = "irn", nullable = false)
	public String getIrn() {
		return irn;
	}

	public void setIrn(String irn) {
		this.irn = irn;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EInvoiceLogDTO [id=");
		builder.append(id);
		builder.append(", invoiceId=");
		builder.append(invoiceId);
		builder.append(", eInvoiceRequestpayload=");
		builder.append(eInvoiceRequestpayload);
		builder.append(", eInvoiceResponse=");
		builder.append(eInvoiceResponse);
		builder.append(", createdAt=");
		builder.append(createdAt);
		builder.append(", status=");
		builder.append(status);
		builder.append(", irn=");
		builder.append(irn);
		builder.append("]");
		return builder.toString();
	}

}
