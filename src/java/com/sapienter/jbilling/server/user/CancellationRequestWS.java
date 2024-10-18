package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.util.Date;

import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

public class CancellationRequestWS  implements  Serializable{

	private Integer id;
	private Integer customerId;
	private Date cancellationDate;
	private String reasonText;
	private CancellationRequestStatus status;
	private Date createTimestamp;

	public CancellationRequestWS() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public Date getCancellationDate() {
		return cancellationDate;
	}

	public void setCancellationDate(Date cancellationDate) {
		this.cancellationDate = cancellationDate;
	}

	public String getReasonText() {
		return reasonText;
	}

	public void setReasonText(String reasonText) {
		this.reasonText = reasonText;
	}

	public CancellationRequestStatus getStatus() {
		return status;
	}

	public void setStatus(CancellationRequestStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return String
				.format("CancellationRequestWS [id=%s, customerId=%s, cancellationDate=%s, reasonText=%s, status=%s, createTimestamp=%s]",
						id, customerId, cancellationDate, reasonText, status,
						createTimestamp);
	}
}
