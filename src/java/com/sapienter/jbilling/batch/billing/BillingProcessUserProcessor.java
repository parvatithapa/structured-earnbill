package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDTO;

/**
 * 
 * @author Khobab
 *
 */
public class BillingProcessUserProcessor implements InitializingBean, ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;
    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobParameters['billingDate']}")
    private Date billingDate;
    @Value("#{jobParameters['review'] == 1L}")
    private boolean review;
    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;

    private boolean onlyRecurring;

    /**
     * receives user id, processes it and then returns a user id.
     */
    @Override
    public Integer process (Integer userId) throws Exception {

        long enteringTime = System.currentTimeMillis();
        logger.trace("Billing process ID: {}, User ID: {} +++ Entering process(userId)", billingProcessId, userId);

        int totalInvoices = 0;

//        if (userId == 75) {
//            throw new RuntimeException("Debug exception for user 75, to verify that skip listener works");
//        }
        Integer[] result = local.processUser(billingProcessId, billingDate, userId, review, onlyRecurring);
        if (result != null) {
            jdbcService.markUserAsSuccessful(billingProcessId, userId, jobExecutionId);
            local.addProcessRunUser(billingProcessId, userId, ProcessRunUserDTO.STATUS_SUCCEEDED);
            logger.debug("Billing process ID: {}, User ID: {}  +++ STATUS_SUCCEEDED.", billingProcessId, userId);
            totalInvoices = result.length;
        }
        logger.debug("Billing process ID: {}, User ID: {}  +++ invoices generated: {}.", billingProcessId, userId,
                totalInvoices);

        logger.trace("Billing process ID: {}, User ID: {} processed in {} secs. +++ Leaving process(userId)",
                billingProcessId, userId, (System.currentTimeMillis() - enteringTime) / 1000);

        return userId;
    }

    @Override
    public void afterPropertiesSet () {
        onlyRecurring = new ConfigurationBL(entityId).getEntity().getOnlyRecurring() == 1;
    }
}
