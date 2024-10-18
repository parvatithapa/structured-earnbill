package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

/**
 * Created by tarun.rathor on 3/15/18.
 */
public class NetworkTimeoutException extends IOException {
	/**
	 *
	 */
	public NetworkTimeoutException() {
		super();
	}

	/**
	 * @param message
	 */
	public NetworkTimeoutException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NetworkTimeoutException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NetworkTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
