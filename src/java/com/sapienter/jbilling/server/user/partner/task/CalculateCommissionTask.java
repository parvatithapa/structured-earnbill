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
package com.sapienter.jbilling.server.user.partner.task;

import java.lang.invoke.MethodHandles;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.util.Context;

/**
 * Scheduled task to trigger the Partner Commission Process
 */
public class CalculateCommissionTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public String getTaskName () {
        return this.getClass().getName() + "-" + getEntityId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        _init(context);
        IUserSessionBean userSessionBean = Context.getBean(Context.Name.USER_SESSION);

        // get a session for the remote interfaces
        userSessionBean.calculatePartnerCommissions(getEntityId());
    }
}
