package com.sapienter.jbilling.server.integration.common.appdirect.client.exception;

import java.io.IOException;

/**
 * Created by tarun.rathor on 3/15/18.
 */
public class SubscriptionUsageNotAllowed extends IOException {
	/**
	 *
	 */
	public SubscriptionUsageNotAllowed() {
		super();
	}

	/**
	 * @param message
	 */
	public SubscriptionUsageNotAllowed(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SubscriptionUsageNotAllowed(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SubscriptionUsageNotAllowed(String message, Throwable cause) {
		super(message, cause);
	}
}
