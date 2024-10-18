/**
 * TestRail API binding for Java (API v2, available since TestRail 3.0)
 *
 * Learn more:
 *
 * http://docs.gurock.com/testrail-api2/start
 * http://docs.gurock.com/testrail-api2/accessing
 *
 * Code borrowed from: https://github.com/gurock/testrail-api/tree/master/java/com/gurock/testrail
 *
 * Copyright Gurock Software GmbH. See license.md for details.
 */

package com.gurock.testrails;

public class APIException extends Exception {
	/** 
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public APIException(String message) {
		super(message);
	}
}
