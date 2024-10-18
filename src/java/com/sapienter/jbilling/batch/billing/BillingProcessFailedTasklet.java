package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.BillingProcessRunBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.util.Constants;

/**
 * 
 * @author Khobab
 *
 */
public class BillingProcessFailedTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;
    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    /**
     * Marks process as failed, Send notification of failure along with total number of users failed and updates total
     * number of invoices.
     */
    @Override
    public RepeatStatus execute (StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        int totalUsersFailed = jdbcService.countFailedUsers(billingProcessId);

        logger.debug("Billing process ID: {}, Entity ID {}. Setting as failed", billingProcessId, entityId);
        local.updateProcessRunFinished(billingProcessId, Constants.PROCESS_RUN_STATUS_FAILED);

        BillingProcessRunBL billingProcessRunBL = new BillingProcessRunBL();
        billingProcessRunBL.setProcess(billingProcessId);
        billingProcessRunBL.notifyProcessRunFailure(entityId, totalUsersFailed);

        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        runBL.updateTotals(billingProcessId);

        logger.debug("Billing process ID: {}, Entity ID {} is done. Failed users # {}", billingProcessId, entityId,
                totalUsersFailed);

        return RepeatStatus.FINISHED;
    }
}
