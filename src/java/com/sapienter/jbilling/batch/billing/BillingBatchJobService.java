package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.util.Assert;

import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessInfoDAS;

/**
 *
 * @author Khobab
 *
 */
public class BillingBatchJobService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;
    @Resource
    private JobExplorer jobExplorer;
    @Resource
    private BillingProcessInfoDAS billingProcessInfoDAS;

    public Date getStartDate (Integer jobExecutionId) {
        return findJobExecution(jobExecutionId).getStartTime();
    }

    public Date getEndDate (Integer jobExecutionId) {
        return findJobExecution(jobExecutionId).getEndTime();
    }

    /**
     * restarts a failed job
     *
     * @param billingProcessId
     *            id of the failed billing process
     * @param entityId
     *            id of the entity to which billing process belongs
     * @return true - if restart was successful
     */
    public boolean restartFailedJobByBillingProcessId (Integer billingProcessId, final Integer entityId) {
        logger.debug("Entering restartFailedJobByBillingProcessId() with id # {}", billingProcessId);
        final Date jobRunDate = extractBillingDateFromJobParameters(
                findRecentExecutionIdByBillingProcessId(billingProcessId));
        if (jobRunDate != null) {
            return local.triggerAsync(jobRunDate, entityId);
        }
        return false;
    }

    public boolean isJobRunning(String jobName) {
        Assert.hasLength(jobName, "Job Name can not be null or empty!");
        Set<JobExecution> executions = jobExplorer.findRunningJobExecutions(jobName);
        return !executions.isEmpty();
    }

    /**
     * Get latest job execution id of given billing process
     *
     * @param billingProcessId
     *            : id of the billing process
     * @return : latest execution id
     */
    private Integer findRecentExecutionIdByBillingProcessId (Integer billingProcessId) {
        return billingProcessInfoDAS.findExecutionsInfoByBillingProcessId(billingProcessId).get(0).getJobExecutionId();
    }

    private JobExecution findJobExecution (Integer jobExecutionId) {
        return jobExplorer.getJobExecution(jobExecutionId.longValue());
    }

    private Date extractBillingDateFromJobParameters (Integer executionId) {
        return findJobExecution(executionId).getJobParameters().getDate(BatchConstants.PARAM_BILLING_DATE);
    }
}
