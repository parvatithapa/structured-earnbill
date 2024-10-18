package com.sapienter.jbilling.einvoice.providers.gstzen.client;

@SuppressWarnings("serial")
public class GstZenClientException extends RuntimeException {
	private int httpStatusCode;
	private String uuid;

	public GstZenClientException(int httpStatusCode, String uuid, String message) {
		super(message);
		this.httpStatusCode = httpStatusCode;
		this.uuid = uuid;
	}

	public GstZenClientException(int httpStatusCode, String message) {
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
