package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

public class RemoteServerException extends IOException {

	public RemoteServerException() {
		super();
	}

	/**
	 * @param message
	 */
	public RemoteServerException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteServerException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteServerException(String message, Throwable cause) {
		super(message, cause);
	}


}
