package com.sapienter.jbilling.batch.billing;

import javax.annotation.Resource;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.batch.BatchConstants;

public class GenerateInvoicesStepListener implements StepExecutionListener {

    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['review'] == 1L}")
    private boolean review;

    @Override
    public ExitStatus afterStep (StepExecution stepExecution) {
        int failedUserCount = jdbcService.countFailedUsers(billingProcessId);
        if(review) {
            return failedUserCount > 0 ? BatchConstants.FAILED_REVIEW :
                BatchConstants.COMPLETED_REVIEW;
        }
        return failedUserCount > 0 ? ExitStatus.FAILED :
            ExitStatus.COMPLETED;
    }

    @Override
    public void beforeStep (StepExecution arg0) {
        jdbcService.resetFailedUsers(billingProcessId);
    }
}
