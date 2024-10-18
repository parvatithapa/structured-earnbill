package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

/**
 * Created by tarun.rathor on 2/15/18.
 */
public class SubscriptionNotFoundException extends IOException {
	/**
	 *
	 */
	public SubscriptionNotFoundException() {
		super();
	}

	/**
	 * @param message
	 */
	public SubscriptionNotFoundException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SubscriptionNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SubscriptionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
