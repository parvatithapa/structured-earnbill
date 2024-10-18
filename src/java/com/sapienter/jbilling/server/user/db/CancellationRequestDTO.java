package com.sapienter.jbilling.server.user.db;

import java.util.Date;

import javax.persistence.*;

@Entity
@TableGenerator(
        name = "cancellation_request_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "cancellation_request",
        allocationSize = 100)
@Table(name = "cancellation_request")
public class CancellationRequestDTO {
	private Integer id;
	private CustomerDTO customer;
	private Date cancellationDate;
	private String reasonText;
	private CancellationRequestStatus status;
	private Date createTimestamp;
	
	public CancellationRequestDTO() {

	}
	
	public CancellationRequestDTO(CustomerDTO customer,	Date cancellationDate, String reasonText,
			CancellationRequestStatus status,Date createTimestamp) {
		this.customer = customer;
		this.cancellationDate = cancellationDate;
		this.reasonText = reasonText;
		this.status = status;
		this.createTimestamp = createTimestamp;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
	public CustomerDTO getCustomer() {
		return customer;
	}

	public void setCustomer(CustomerDTO customer) {
		this.customer = customer;
	}

	@Column(name="cancellation_date")
	public Date getCancellationDate() {
		return cancellationDate;
	}

	public void setCancellationDate(Date cancellationDate) {
		this.cancellationDate = cancellationDate;
	}

	@Column(name="reason_of_cancellation")
	public String getReasonText() {
		return reasonText;
	}

	public void setReasonText(String reasonText) {
		this.reasonText = reasonText;
	}

	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public CancellationRequestStatus getStatus() {
		return status;
	}

	public void setStatus(CancellationRequestStatus status) {
		this.status = status;
	}
	
	@Column(name = "create_timestamp", nullable = false, length = 29)
	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}

	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "cancellation_request_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return String
				.format("CancellationRequestDTO [id=%s, customerId=%s, cancellationDate=%s, reasonText=%s, status=%s, createTimestamp=%s]",
						id, customer.getId(), cancellationDate, reasonText, status,
						createTimestamp);
	}
	
}
