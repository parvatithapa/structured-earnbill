package com.sapienter.jbilling.batch.email;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.Resource;

import com.sapienter.jbilling.batch.BatchConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;

import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoBL;
import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoDTO;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.scheduledTask.event.ScheduledJobNotificationEvent;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

public class InvoiceEmailDispatcherJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String JOBCONTEXT_BILLING_PROCESS_ID_KEY = "billingProcessId";

    @Resource
    private JobRepository jobRepository;
    @Resource
    private IBillingProcessSessionBean local;    
    @Resource
    private EmailBatchService jdbcService;
    @Resource
    private InvoiceDAS invoiceDAS;

    /**
     * Moves job execution context data to database tables at the end of billing process
     */
    @Override
    public void afterJob (JobExecution jobExecution)  {
        logger.debug("InvoiceEmailDispatcherJobListener : afterJob : started");
        JobParameters jobParams = jobExecution.getJobParameters();
        Integer billingProcessId = jobParams.getLong(BatchConstants.PARAM_BILLING_PROCESS_ID).intValue();

        int totalEmailsSent = jdbcService.countSuccessfulUsers(billingProcessId, jobExecution.getId());
        int totalEmailsFailed = jdbcService.countFailedUsers(billingProcessId);

        logger.debug("Billing process ID: {}, jobExecution ID: {}. Totals: successful users: {} ,failed users: {}",
                billingProcessId, jobExecution.getId(), totalEmailsSent, totalEmailsFailed);

        try {
            BillingProcessBL billingProcessBL = new BillingProcessBL(billingProcessId);                

            Integer emailProcessInfoId = null;
            if(jobExecution.getExecutionContext().containsKey(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY)){
                logger.debug("execution context contains the invoice email process info id..{}",jobExecution.getExecutionContext().getInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY));
                emailProcessInfoId = Integer.valueOf(jobExecution.getExecutionContext().getInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY));
            }
            if(emailProcessInfoId !=null) {
                InvoiceEmailProcessInfoBL invoiceEmailProcessInfoBL = new InvoiceEmailProcessInfoBL(emailProcessInfoId);
                InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO = invoiceEmailProcessInfoBL.getEntity();                 
                invoiceEmailProcessInfoDTO.setJobExecutionId(jobExecution.getId().intValue());
                invoiceEmailProcessInfoDTO.setBillingProcess(billingProcessBL.getEntity());
                invoiceEmailProcessInfoDTO.setEmailsFailed(billingProcessBL.getEmailsFailedCount(billingProcessId, jobExecution.getId().intValue()));
                invoiceEmailProcessInfoDTO.setEmailsSent(billingProcessBL.getEmailsSentCount(billingProcessId, jobExecution.getId().intValue()));
                invoiceEmailProcessInfoDTO.setEndDatetime(new Date());
                SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
                spcHelperService.createOrUpdateInvoiceEmailProcessInfo(invoiceEmailProcessInfoDTO);
            }
        } catch(Exception e) {
            logger.error("Error while updating the email counts in invoice email process info after email job, {}",e);
        }

       jdbcService.cleanupEmailJobData(billingProcessId);

        logger.debug("BillingProcessJobListener : afterJob");
        try {
            Integer entityId = jobExecution.getJobParameters().getLong(Constants.BATCH_JOB_PARAM_ENTITY_ID).intValue();
            logger.debug("entityId : {}", entityId);
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "Email Job",
                    jobExecution, ScheduledJobNotificationEvent.TaskEventType.AFTER_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on afterJob for Billing Process Listener");
        }
        logger.debug("InvoiceEmailDispatcherJobListener : afterJob : ended");
    }

    /**
     * 1. findinvoicesToProcess(entityId, billingProcessId) 
     * 2. Stores Invoice Ids to batch invoice table
     */
    @Override
    public void beforeJob (JobExecution jobExecution) {

        logger.debug("InvoiceEmailDispatcherJobListener : beforeJob : started");
        JobParameters jobParams = jobExecution.getJobParameters();
        Integer entityId = jobParams.getLong(Constants.BATCH_JOB_PARAM_ENTITY_ID).intValue();
        Integer billingProcessId = jobParams.getLong(BatchConstants.PARAM_BILLING_PROCESS_ID).intValue();
        BillingProcessBL billingProcessBL = new BillingProcessBL(billingProcessId);

        logger.debug("Job will use billing process id: {}", billingProcessId);

        jdbcService.createEmailProcessData(invoiceDAS.getInvoiceIdsByBillingProcessIdEmailUsers(billingProcessId), billingProcessId);

        jobExecution.getExecutionContext().putLong(JOBCONTEXT_BILLING_PROCESS_ID_KEY, billingProcessId);
        try {
            logger.debug("Billing process not review, creating record in the table invoice_email_process_info");
            InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO = new InvoiceEmailProcessInfoDTO(billingProcessBL.getEntity(), jobExecution.getId().intValue(), jdbcService.getEmailJobEstimatedEmails(billingProcessId), 0, 0, new Date(), null,BatchConstants.EMAIL_JOB);
            SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
            invoiceEmailProcessInfoDTO = spcHelperService.createOrUpdateInvoiceEmailProcessInfo(invoiceEmailProcessInfoDTO);
            
            jobExecution.getExecutionContext().putInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY, invoiceEmailProcessInfoDTO.getId());
        } catch(Exception e) {
            logger.warn("Error while updating the email counts in invoice email process info before email job, {}",e);
        }
        jobRepository.updateExecutionContext(jobExecution);

        try {
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "Email Job",
                    jobExecution, ScheduledJobNotificationEvent.TaskEventType.BEFORE_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on beforeJob for Billing Process Listener");
        }
        logger.debug("InvoiceEmailDispatcherJobListener : beforeJob: ended");
    }
}
