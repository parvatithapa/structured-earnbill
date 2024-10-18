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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO;

import java.util.Date;

public interface IJobExecutionSessionBean {

    public static String LINE_TYPE_HEADER = "HEADER";

    public JobExecutionHeaderDTO startJob(long jobExecutionId, String type, Date startDate, int entityId);

    public void endJob(long jobExecutionId, Date endDate) ;

    public void endJob(long jobExecutionId, Date endDate, JobExecutionHeaderDTO.Status status);

    public void addLine(long jobExecutionId, String type, String name, String value) ;

    public void updateLine(long jobExecutionId, String type, String name, String value) ;

    public void incrementLine(long jobExecutionId, String type, String name) ;
}
