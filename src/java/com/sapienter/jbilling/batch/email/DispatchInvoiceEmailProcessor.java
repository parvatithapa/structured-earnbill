package com.sapienter.jbilling.batch.email;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;

/**
 * @author Abhijeet Kore
 */
public class DispatchInvoiceEmailProcessor implements ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Resource
    private EmailBatchService jdbcService;
    @Resource
    private IBillingProcessSessionBean local;    
    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;

    @Override
    public Integer process (Integer invoiceId) {
        logger.debug("Billing process ID: {}, Invoice ID: {} Entering Dispatch Invoice Email Processor ", billingProcessId, invoiceId);

        try {
            local.email(entityId, invoiceId, billingProcessId);
            logger.debug("Billing process ID: {}, Invoice ID: {} sent email now updating status of email ", billingProcessId, invoiceId);
            jdbcService.markInvoiceAsSuccessful(billingProcessId, invoiceId, jobExecutionId);
        } catch(Exception e) {
            logger.error("Billing process ID: {}, Invoice ID: {} , error while sending email ", billingProcessId, invoiceId);
        }

        logger.debug("Billing process ID: {}, Invoice ID: {} Exiting process ", billingProcessId, invoiceId);
        return invoiceId;
    }
}
