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

package com.sapienter.jbilling.server.billing.task;

import com.sapienter.jbilling.client.process.SchedulerCloudHelper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Scheduled billing process plug-in, executing the billing process on a simple schedule.
 *
 * This plug-in accepts the standard {@link com.sapienter.jbilling.server.process.task.AbstractCronTask}
 *
 * @author
 * @since
 */
public class BillingProcessTask extends AbstractCronTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BillingProcessTask.class));

    public String getTaskName() {
        return "billing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
            IBillingProcessSessionBean billing = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
            LOG.info("Starting billing at " + TimezoneHelper.serverCurrentDate() + " for " + getEntityId());
            billing.trigger(companyCurrentDate(), getEntityId());
            LOG.info("Ended billing at " + TimezoneHelper.serverCurrentDate());
            handleChainedTasks(context); // calling chained tasks
    }
}
