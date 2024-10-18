package com.sapienter.jbilling.batch.ignition;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

public class IgnitionCustomerPaymentUpdateStepListener extends StepExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IgnitionBatchService jdbcService;

    @Override
    public ExitStatus afterStep (StepExecution stepExecution) {
        logger.trace("Cleanup payments Ids for job {}", stepExecution.getJobExecution().getJobId());
        jdbcService.cleanupJobPaymentsIds(stepExecution.getJobExecution().getJobId());
        return null;
    }
}
