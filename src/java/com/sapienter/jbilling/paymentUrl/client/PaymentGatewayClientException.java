package com.sapienter.jbilling.paymentUrl.client;

@SuppressWarnings("serial")
public class PaymentGatewayClientException extends RuntimeException {
	private int httpStatusCode;
	private String uuid;

	public PaymentGatewayClientException(int httpStatusCode, String uuid, String message) {
		super(message);
		this.httpStatusCode = httpStatusCode;
		this.uuid = uuid;
	}

	public PaymentGatewayClientException(int httpStatusCode, String message) {
		super(message);
		this.httpStatusCode = httpStatusCode;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public String getUuid() {
		return uuid;
	}
}
