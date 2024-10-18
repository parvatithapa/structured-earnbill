package com.sapienter.jbilling.batch.billing;

import javax.annotation.Resource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.annotation.Value;

public class BillingProcessJobFlowDecider implements JobExecutionDecider {

    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;

    @Override
    public FlowExecutionStatus decide (JobExecution jobExecution, StepExecution stepExecution) {
                
        return (jdbcService.countFailedUsers(billingProcessId) == 0) ? FlowExecutionStatus.COMPLETED
                : FlowExecutionStatus.FAILED;
    }
}
