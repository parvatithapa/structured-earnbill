package com.sapienter.jbilling.server.payment.tasks.ep2;

/**
 * Ep2Result Class will used  as Container of Ep2 gate Way response
 * @author krunal bhavar
 *
 */
public class Ep2Result {

	private boolean succeeded;
	private String description;
	private String transactionId;
	private String responseCode;
	private String tokenId;
	private String approvalCode;
	
	public boolean isSucceeded() {
		return succeeded;
	}
	
	public String getTokenId() {
		return tokenId;
	}

	public Ep2Result addTokenId(String tokenId) {
		this.tokenId = tokenId;
		return this;
	}

	public String getDescription() {
		return description;
	}
	
	public String getTransactionId() {
		return transactionId;
	}
	
	public String getResponseCode() {
		return responseCode;
	}
	
	public Ep2Result addSucceeded(boolean succeeded) {
		this.succeeded = succeeded;
		return this;
	}
	
	public Ep2Result addDescription(String description) {
		this.description = description;
		return this;
	}
	
	public Ep2Result addTransactionId(String transactionId) {
		this.transactionId = transactionId!=null ? transactionId : ""; 
		return this;
	}
	
	public Ep2Result addResponseCode(String responseCode) {
		this.responseCode = responseCode;
		return this;
	}
	
	public String getApprovalCode() {
		return approvalCode;
	}

	public Ep2Result addApprovalCode(String approvalCode) {
		this.approvalCode = approvalCode;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Ep2Result [succeseeded=");
		builder.append(succeeded);
		builder.append(", description=");
		builder.append(description);
		builder.append(", transactionId=");
		builder.append(transactionId);
		builder.append(", responseCode=");
		builder.append(responseCode);
		builder.append(", tokenId=");
		builder.append(tokenId);
		builder.append("]");
		return builder.toString();
	}
	
}