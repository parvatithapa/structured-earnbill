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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.client.process.SchedulerCloudHelper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * AgeingProcessTask
 *
 * @author Brian Cowdery
 * @since 29/04/11
 */
public class AgeingProcessTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AgeingProcessTask.class));

    public String getTaskName() {
        return "ageing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
            Date startCurrentDate = TimezoneHelper.serverCurrentDate();
            Date companyCurrentDate = companyCurrentDate();

            IBillingProcessSessionBean billing = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
            LOG.info("Starting ageing for entity " + getEntityId() + " at " + startCurrentDate + " server timezone, " + companyCurrentDate + " company timezone.");
            billing.reviewUsersStatus(getEntityId(), companyCurrentDate);
            LOG.info("Ended ageing at " + TimezoneHelper.serverCurrentDate() + " server timezone.");
    }
}
