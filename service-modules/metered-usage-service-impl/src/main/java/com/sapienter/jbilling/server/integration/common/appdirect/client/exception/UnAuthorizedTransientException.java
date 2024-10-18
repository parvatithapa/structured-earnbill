package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

public class UnAuthorizedTransientException extends IOException {
	/**
	 *
	 */
	public UnAuthorizedTransientException() {
		super();
	}

	/**
	 * @param message
	 */
	public UnAuthorizedTransientException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public UnAuthorizedTransientException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnAuthorizedTransientException(String message, Throwable cause) {
		super(message, cause);
	}
}
