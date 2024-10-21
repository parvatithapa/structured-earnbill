package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;


/**
 * 
 * @author Abhijeet Kore
 *
 */
public class BillingProcessEmailsSkipTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    
    /**
     * Marks process as failed, Send notification of failure along with total number of users failed and updates total
     * number of invoices.
     */
    @Override
    public RepeatStatus execute (StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        logger.debug("Skipping emails for this Billing Process id {}", billingProcessId);        
        return RepeatStatus.FINISHED;
    }
}
