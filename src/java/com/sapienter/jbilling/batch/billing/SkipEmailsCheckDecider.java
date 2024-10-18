package com.sapienter.jbilling.batch.billing;



import java.lang.invoke.MethodHandles;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoBL;
import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoDTO;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.batch.email.EmailBatchService;
import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;

public class SkipEmailsCheckDecider implements JobExecutionDecider {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
      
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Resource
    private BillingProcessDAS billingProcessDAS;
    @Resource
    private EmailBatchService emailBatchService;

    @Override
    public FlowExecutionStatus decide (JobExecution jobExecution, StepExecution stepExecution) {

        ConfigurationBL conf = new ConfigurationBL(entityId);
        BillingProcessConfigurationDTO billingProcesssConfig = conf.getDTO();
        BillingProcessBL billingProcessBL = new BillingProcessBL(billingProcessId);
        BillingProcessDTO billingprocess  = billingProcessBL.getEntity();

        boolean skipEmails = billingProcesssConfig.shouldSkipEmails();
        logger.debug("skip emails flag value is # {}", skipEmails);
        List<String> skipEmailDaysList = Collections.emptyList();
        String skipEmailDays = billingProcesssConfig.getSkipEmailsDays();
        if(skipEmails) {
            skipEmailDaysList=Arrays.asList(skipEmailDays == null ? new String[0] : skipEmailDays.split(","));
        }
        logger.debug("skip emails day list is # {}", skipEmailDaysList);
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        String tableName = new MetaFieldDAS().getComapanyLevelMetaFieldValue(BatchConstants.EMAIL_HOLIDAY_TABLE_NAME_META_FIELD, entityId);
        List<String> holidaysList = emailBatchService.getHolidayList(entityId, tableName);
        logger.debug("Holiday list is # {}", holidaysList);
        logger.debug("Billing Process id is # {}", billingProcessId);
        BillingProcessDTO billRunProcess = new BillingProcessDAS().find(billingProcessId);
        Date billRunDate = billRunProcess.getBillingDate();
        logger.debug("Billing Run date is # {}", billRunDate);

        if(CalendarUtils.skipEmails(skipEmails, billRunDate, skipEmailDaysList, holidaysList)) {
            logger.debug("Skipping the email step for the Bill Run {}",billingProcessId);            
            return new FlowExecutionStatus("SKIP");
        }
        
        if(billingprocess.getIsReview() == 0) {
            try {
                logger.debug("Billing process not review, creating record in the table invoice_email_process_info");
                Integer emailProcessInfoId = null;
                if(jobExecution.getExecutionContext().containsKey(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY)){
                    logger.debug("execution context contains the invoice email process info id..{}",jobExecution.getExecutionContext().getInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY));
                    emailProcessInfoId = Integer.valueOf(jobExecution.getExecutionContext().getInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY));
                }
                InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO = null;
                InvoiceEmailProcessInfoBL invoiceEmailProcessInfoBL = new InvoiceEmailProcessInfoBL();
                invoiceEmailProcessInfoDTO = spcHelperService.createOrUpdateInvoiceEmailProcessInfo(new InvoiceEmailProcessInfoDTO(billingprocess, jobExecution.getId().intValue(), billingProcessDAS.getEmailsEstimatedCount(billingProcessId), 0, 0, new Date(), null, BatchConstants.BILLING_PROCESS));
                                              
                jobExecution.getExecutionContext().putInt(BatchConstants.JOBCONTEXT_INVOICE_EMAIL_PROCESS_INFO_ID_KEY, invoiceEmailProcessInfoDTO.getId());
            } catch(Exception e) {
                logger.error("Error creating entry in the table invoice_email_process_info, {}",e.getMessage(),e);
            }
        }
        logger.debug("No need to skip the email step for the Bill Run {}",billingProcessId);       
        return new FlowExecutionStatus("RUN");
    }
}
