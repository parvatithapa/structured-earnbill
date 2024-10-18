package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDTO;

public class BillingSkipListener extends SkipListenerSupport<Integer, Integer> implements SkipListener<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;
    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;

    @Value("#{stepExecution.stepName.split(':')[0]}")
    private String stepName;

    private int status;

    public void setStatus (int status) {
        this.status = status;
    }

    /**
     * Called for every object that was skipped. Increments total users failed value in context and adds user id to
     * failed user list in context.
     */
    @Override
    public void onSkipInProcess (Integer userId, Throwable error) {
        logger.info("Billing process ID: {}, UserId # {}  +++ Skipped at step: {} due to # {}", billingProcessId, userId, stepName, error);
        if(BatchConstants.EMAIL_AND_PAYMENT_STEP_NAME.equals(stepName)) {
            return;
        }
        jdbcService.markUserAsFailedWithStatus(billingProcessId, userId, status);
        local.addProcessRunUser(billingProcessId, userId, ProcessRunUserDTO.STATUS_FAILED);
    }
}
