package com.sapienter.jbilling.server.payment.tasks.paysafe;

public class PaySafeResult {

	private String merchantRefNumber;
	private String transactionId;
	private String authCode;
	private String errorMessage;
	private String errorCode;
	private boolean succeeded;
	private String avs;
	
	public String getAvs() {
		return avs;
	}

	public void setAvs(String avs) {
		this.avs = avs;
	}

	public String getMerchantRefNumber() {
		return merchantRefNumber;
	}
	
	public String getTransactionId() {
		return transactionId;
	}
	
	public String getAuthCode() {
		return authCode;
	}
	
	public void setMerchantRefNumber(String merchantRefNumber) {
		this.merchantRefNumber = merchantRefNumber;
	}
	
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public boolean isSucceeded() {
		return succeeded;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public void setSucceeded(boolean succeeded) {
		this.succeeded = succeeded;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
}
