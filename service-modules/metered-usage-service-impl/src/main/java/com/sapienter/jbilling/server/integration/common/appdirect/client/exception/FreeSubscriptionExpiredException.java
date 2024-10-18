package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

/**
 * Created by tarun.rathor on 2/15/18.
 */
public class FreeSubscriptionExpiredException extends IOException {
	/**
	 *
	 */
	public FreeSubscriptionExpiredException() {
		super();
	}

	/**
	 * @param message
	 */
	public FreeSubscriptionExpiredException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public FreeSubscriptionExpiredException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FreeSubscriptionExpiredException(String message, Throwable cause) {
		super(message, cause);
	}
}
