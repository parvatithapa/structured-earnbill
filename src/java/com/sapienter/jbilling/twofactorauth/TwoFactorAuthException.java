package com.sapienter.jbilling.twofactorauth;

@SuppressWarnings("serial")
public class TwoFactorAuthException extends RuntimeException {
	private int httpStatusCode;
	private String uuid;

	public TwoFactorAuthException(int httpStatusCode, String uuid, String message) {
		super(message);
		this.httpStatusCode = httpStatusCode;
		this.uuid = uuid;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public String getUuid() {
		return uuid;
	}
}