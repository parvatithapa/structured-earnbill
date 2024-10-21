package com.sapienter.jbilling.batch.email;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.batch.BatchConstants;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * This class has methods to trigger the Invoice email Dispatcher job
 *
 * @author Abhijeet Kore
 *
 */
public class EmailBatchJobService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public boolean triggerAsync (Integer billingProcessId, Integer entityId) {
        new Thread(() -> triggerInvoiceEmailDispatcherJob(billingProcessId, entityId)).start();
        return true;
    }

    /**
     * triggers the Invoice Email Dispatcher Job
     *
     * @param billingProcessId
     *            id of the failed billing process
     * @param entityId
     *            id of the entity to which billing process belongs
     * @return true - if restart was successful
     */
    public boolean triggerInvoiceEmailDispatcherJob (Integer billingProcessId, Integer entityId) {

        logger.debug("Entering triggerInvoiceEmailDispatcherJob() with id # {}", billingProcessId);

        IWebServicesSessionBean webServicesSession  = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        EmailBatchService emailBatchService = Context.getBean(EmailBatchService.class);
        
        if(Boolean.FALSE.equals(webServicesSession.isBillingRunning(entityId)) && 
                Boolean.FALSE.equals(emailBatchService.isEmailJobRunning())) {
            JobLauncher launcher = Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);        
            logger.debug("Loaded job launcher bean # {}", launcher);

            Job job = Context.getBean(Context.Name.BATCH_JOB_DISPATCH_INVOICE_EMAILS);        
            logger.debug("Loaded job bean # {}", job);

            JobParametersBuilder paramBuilder = new JobParametersBuilder()
            .addLong(BatchConstants.PARAM_BILLING_PROCESS_ID, billingProcessId.longValue())
            .addLong(BatchConstants.PARAM_ENTITY_ID, entityId.longValue())
            .addDate(BatchConstants.DATE_TIME_TRIGGERRED, new Date());

            JobParameters jobParameters = paramBuilder.toJobParameters();

            try {
                logger.debug("Triggerring the Email job now");
                launcher.run(job, jobParameters);
            } catch (Exception e) {
                logger.error("Job # {} with parameters # {} could not be launched: {}",
                        job.getName(), jobParameters.toString(), e);
                return false;
            }
            return true;
        } 
        logger.debug("Either Billing Process OR Email Job is running, can not trigger the email job now");
        return false;
    }

}
