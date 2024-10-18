/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.task;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.ICustomerUsagePoolEvaluationSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomerUsagePoolEvaluationTask
 * This is the Customer Usage Pool evaluation task, which is a scheduled task 
 * extending AbstractCronTask. It has been setup to run
 * every midnight by default.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolEvaluationTask extends AbstractCronTask {
	
    private static final Logger logger = LoggerFactory.getLogger(CustomerUsagePoolEvaluationTask.class);

	public String getTaskName() {
		return "Customer Usage Pool Evalution Process: , entity id " + getEntityId() + ", taskId " + getTaskId();
	}

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        ICustomerUsagePoolEvaluationSessionBean usageEvaluationBean =
                Context.getBean(Context.Name.CUSTOMER_USAGE_POOL_EVALUATION_SESSION);

        IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");

        Integer entityId = getEntityId();

        boolean alwaysEnableProrating = actionTxWrapper.execute(() -> ConfigurationBL.doesBillingProcessHaveAlwaysEnableProrating(entityId));

        if (!alwaysEnableProrating) {
            logger.debug("Executing CustomerUsagePoolEvaluationTask for entity {} ", entityId);
            usageEvaluationBean.trigger(entityId, TimezoneHelper.serverCurrentDate());
		} else {
			throw new SessionInternalError("Customer Usage Pool Evalution Task", new String[] {
                    "PluggableTaskTypeWS,className,customer.usage.pool.evaluation.task.validation.error"
            });
		}
    }
 }
