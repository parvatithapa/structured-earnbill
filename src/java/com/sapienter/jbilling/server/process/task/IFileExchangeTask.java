package com.sapienter.jbilling.server.process.task;/*
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

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by marcomanzi on 3/3/14.
 */
public interface IFileExchangeTask {

    public void execute();

    boolean isDownloadTask() throws PluggableTaskException;

}
