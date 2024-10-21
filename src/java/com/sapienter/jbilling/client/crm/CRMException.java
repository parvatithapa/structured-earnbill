/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.client.crm;

@SuppressWarnings("serial")
public class CRMException extends RuntimeException {
	private int httpStatusCode;
	private String uuid;

	public CRMException(int httpStatusCode, String uuid, String message) {
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
