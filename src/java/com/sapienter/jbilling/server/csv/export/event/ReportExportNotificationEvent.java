/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.csv.export.event;

import com.sapienter.jbilling.server.system.event.Event;


/**
 *
 * @author Harshad P
 */
public class ReportExportNotificationEvent implements Event {

	public enum NotificationStatus {
		PASSED, FAILED
	}
	
	 private final Integer entityId;
	 private final Integer userId;
	 private final String fileName;
	 private final NotificationStatus status;
	
	 public ReportExportNotificationEvent(Integer entityId, Integer userId, String fileName, NotificationStatus status) {
		 this.entityId = entityId;
		 this.userId   = userId;
		 this.fileName = fileName;
		 this.status   = status;
	 }
	 
	public Integer getUserId() {
		return userId;
	}

	@Override
	public Integer getEntityId() {
		return this.entityId;
	}

	@Override
	public String getName() {
		return "ReportExportNotificationEvent";
	}
	
	public NotificationStatus getStatus() {
		return status;
	}

	public String getFileName() {
		return this.fileName;
	}
	
}