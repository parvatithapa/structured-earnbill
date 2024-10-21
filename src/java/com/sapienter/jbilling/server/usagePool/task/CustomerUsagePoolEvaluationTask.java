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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.ICustomerUsagePoolEvaluationSessionBean;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * CustomerUsagePoolEvaluationTask
 * This is the Customer Usage Pool evaluation task, which is a scheduled task
 * extending AbstractCronTask. It has been setup to run
 * every midnight by default.
 *
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolEvaluationTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ParameterDescription PARAM_SPC_BILLING_RUN_DATE =
            new ParameterDescription("Billing Run Date (dd/MM/yyyy)", false, ParameterDescription.Type.STR);

    public CustomerUsagePoolEvaluationTask() {
        descriptions.add(PARAM_SPC_BILLING_RUN_DATE);
    }

    public String getTaskName() {
        return "Customer Usage Pool Evaluation Process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        ICustomerUsagePoolEvaluationSessionBean usagePoolEvaluationBean = Context.getBean(Context.Name.CUSTOMER_USAGE_POOL_EVALUATION_SESSION);

        _init(context);
        Integer entityId = getEntityId();
        logger.debug("Executing scheduled CustomerUsagePoolEvaluationTask for Entity Id: {}", entityId);
        Date billingRunDate = getRunDate(entityId);
        // check bill run date exist,
        Date lastBillingProcessDate = new BillingProcessDAS().getLastBillingProcessDate(entityId);
        logger.debug("LastBillingProcessDate: {}", lastBillingProcessDate);

        if (null != lastBillingProcessDate && lastBillingProcessDate.compareTo(billingRunDate) >= 0) {
            logger.debug("Trigger CustomerUsagePoolEvaluation for entity {} on {}", entityId, billingRunDate);
            usagePoolEvaluationBean.trigger(entityId, billingRunDate);
        } else {
            logger.debug("Billing Run Date {} can not be prior to Last Billing Process Date {}",
                dateFormatter.format(DateConvertUtils.asLocalDate(billingRunDate)),
                null != lastBillingProcessDate ? dateFormatter.format(DateConvertUtils.asLocalDate(lastBillingProcessDate)) : null);
        }
        logger.debug("Completed CustomerUsagePoolEvaluationScheduledTask for Entity Id: {}", entityId);

    }

    private Date getRunDate(Integer entityId) {
        Date billingRunDate;
        String strBillRunDate = getParameter(PARAM_SPC_BILLING_RUN_DATE.getName(), StringUtils.EMPTY);
        logger.debug("Parameter name: {}, value: {}", PARAM_SPC_BILLING_RUN_DATE.getName(), strBillRunDate);

        if (StringUtils.isNotEmpty(strBillRunDate)) {
            try {
                LocalDate ldtBillingRunDate = LocalDate.parse(strBillRunDate, dateFormatter);
                logger.debug("Parse Bill run date from string to date: {}", ldtBillingRunDate);
                billingRunDate = TimezoneHelper.convertToTimezoneAsUtilDateWithTx(ldtBillingRunDate.atStartOfDay(), entityId);
                logger.debug("billingRunDate after conversion to java.util.Date : {}", billingRunDate);

            } catch (DateTimeParseException e) {
                logger.error("Billing run date parse exception: {}", e.getMessage());
                throw new SessionInternalError("Exception from CustomerUsagePoolEvaluationScheduledTask : Not able to parse string parameter bill run date to Date", e);
            }
        } else {
            billingRunDate = TimezoneHelper.companyCurrentDate(entityId);
            logger.debug("Billing Run Date parameter not provided, using company current date {}", billingRunDate);
        }

        return DateUtils.truncate(billingRunDate, java.util.Calendar.DAY_OF_MONTH);
    }

}
