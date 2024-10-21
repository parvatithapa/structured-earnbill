/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.billing.task;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.batch.email.EmailBatchJobService;
import com.sapienter.jbilling.batch.email.EmailBatchService;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;

import org.springframework.batch.core.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;

import javax.annotation.Resource;

/**
 * InvoiceEmailDispatcherTask
 * This is the Invoice Email Dispatcher Task, which is a scheduled task
 * extending AbstractCronTask. It has been setup to trigger the InvoiceEmailDispacher Job 
 *
 * @author Abhijeet Kore
 * @since 21-Oct-2021
 */

public class InvoiceEmailDispatcherTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_CUT_OFF = "20:00";
    private static final String CUT_OFF_BILLING_PROCESS_ID = "Cut Off Billing Process Id";
    private static final ParameterDescription PARAM_BILLING_PROCESS_ID =
            new ParameterDescription("Billing Process Id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_DISPATCH_EMAILS_AGAIN =
            new ParameterDescription("Dispatch Emails Again", false, ParameterDescription.Type.BOOLEAN);
    
    public InvoiceEmailDispatcherTask() {
        descriptions.add(PARAM_BILLING_PROCESS_ID);
        descriptions.add(PARAM_DISPATCH_EMAILS_AGAIN);        
    }

    public String getTaskName() {
        return "Invoice Email Dispatcher Scheduled Task called : , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        try {
            _init(context);
            Integer entityId = getEntityId();

            IWebServicesSessionBean webServicesSession  = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            IBillingProcessSessionBean  billing  = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);            
            EmailBatchJobService emailBatchJobService = Context.getBean(EmailBatchJobService.class); 
            EmailBatchService emailBatchService = Context.getBean(EmailBatchService.class);
            
            if(Boolean.FALSE.equals(webServicesSession.isBillingRunning(entityId)) && 
                    Boolean.FALSE.equals(emailBatchService.isEmailJobRunning())) {
                logger.debug("Executing scheduled InvoiceEmailDispatcherTask for Entity Id: {}", entityId);

                Integer billingProcessId = getParameter(PARAM_BILLING_PROCESS_ID.getName(), 0);
                logger.debug("Bill run id passed via parameter is # {}", billingProcessId);

                Boolean forceRun = getParameter(PARAM_DISPATCH_EMAILS_AGAIN.getName(), false);
                logger.debug("Force run value passed via parameter is # {}", forceRun);


                if(billingProcessId == 0) {
                    logger.debug("Bill run id passed via parameter is null # ");                
                    billingProcessId = billing.getLast(getEntityId());
                }

                BillingProcessBL bl = new BillingProcessBL(billingProcessId);                
                BillingProcessDTO billingProcess = bl.getEntity();

                if(billingProcess == null || billingProcess.getIsReview() == 1) {
                    logger.debug("Billing process id is invalid OR billing process is review, can not proceed ahead");
                    return;
                }
                for(ProcessRunDTO processRun : billingProcess.getProcessRuns()) {
                    if(processRun.getFinished() == null) {
                        logger.debug("Billing process is not yet fninished, can not trigger email job");
                        return;   
                    }
                }
                String cutOffBillingProcessIdStr = new MetaFieldDAS().getComapanyLevelMetaFieldValue(CUT_OFF_BILLING_PROCESS_ID, entityId);
                logger.debug("Cut Off Billing Process Id is  # {}", cutOffBillingProcessIdStr);

                Integer cutOffBillingProcessId = Integer.valueOf(cutOffBillingProcessIdStr == null ? "0" : cutOffBillingProcessIdStr);

                if(billingProcessId  > cutOffBillingProcessId) {
                    List<InvoiceEmailProcessInfoDTO> invoiceEmailProcessInfoList = bl.getAllInvoiceEmailProcessInfo(billingProcessId);

                    if(Boolean.FALSE.equals(forceRun)) {
                        if(!invoiceEmailProcessInfoList.isEmpty()) {
                            logger.debug("The email job is already executed for the Billing process Id {} ",billingProcessId);
                            return;
                        }
                    } else {
                        for(InvoiceEmailProcessInfoDTO invoiceEmailProcess : invoiceEmailProcessInfoList) {
                            if(invoiceEmailProcess.getEndDatetime() == null ) {
                                logger.debug("The email job is already in progress for the Billing process Id {} ",billingProcessId);
                                return;
                            }
                        }
                    }
                    logger.debug("Bill run id value is # {}",billingProcessId);

                    String cutOff = new MetaFieldDAS().getComapanyLevelMetaFieldValue(Constants.EMAIL_JOB_DEFAULT_CUT_OFF_TIME, entityId);
                    logger.debug("Default Email cut off time is {}",cutOff);

                    if(cutOff ==null) {
                        cutOff = DEFAULT_CUT_OFF;
                    }

                    String tableName = new MetaFieldDAS().getComapanyLevelMetaFieldValue(BatchConstants.EMAIL_HOLIDAY_TABLE_NAME_META_FIELD, entityId);
                    List<String> holidaysList = emailBatchService.getHolidayList(entityId,tableName);                
                    logger.debug("Holiday list is # {}", holidaysList);

                    if(triggerEmailJob(cutOff, holidaysList)) {
                        logger.debug("Triggerring the email job");
                        emailBatchJobService.triggerInvoiceEmailDispatcherJob(billingProcessId, entityId);
                    } else {
                        logger.debug("Date not meeting the criteria, email job not triggerred");
                    }
                } else {
                    logger.debug("Billing process id is less than the cut off billing process id, can not trigger the Email job");
                }
            } else {
                logger.debug("Either Billing Process OR Email Job is running, can not trigger the email job now");
            }
        } catch (Exception e) {
            logger.error("Job could not be launched, exception occurred : ", e);
        }        
    }

    private boolean triggerEmailJob(String cutOff, List<String> holidaysList) {

        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(getEntityId());        
        LocalDate ld = LocalDate.now(ZoneId.of(companyTimeZone));  
        LocalDate nextDate = ld.plusDays(1);
        DayOfWeek day = DayOfWeek.of(ld.get(ChronoField.DAY_OF_WEEK));
        /* return false in case of below conditions met */
        /* If day of week is Saturday */
        /* If day of week is Sunday and next day is Holiday */
        /* If day of week is Sunday and trigger time is before cut off */
        /* If day of week is Holiday and trigger time is before cut off */
        /* If day of week is holiday and next day is Holiday as well */
        /* If day of week is holiday and next day is Saturday OR Sunday */
        return !((day == DayOfWeek.SATURDAY) ||
                (day == DayOfWeek.SUNDAY && CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(nextDate), holidaysList)) ||
                (day == DayOfWeek.SUNDAY && isBeforeCutOff(LocalDateTime.now(ZoneId.of(companyTimeZone)), cutOff))||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && isBeforeCutOff(LocalDateTime.now(ZoneId.of(companyTimeZone)), cutOff) )||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(nextDate), holidaysList))||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && CalendarUtils.isWeekend(DateConvertUtils.asUtilDate(nextDate))));        
    }

    private boolean isBeforeCutOff(LocalDateTime currentDateTime, String cutOff) {

        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(getEntityId());
        LocalDate currentDate = LocalDate.now(ZoneId.of(companyTimeZone));     
        DateTimeFormatter timeParser = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime cutOffTime = timeParser.parse(cutOff, LocalTime::from);
        LocalDateTime cutOffDateTime = LocalDateTime.of(currentDate, cutOffTime);
        return currentDateTime.isBefore(cutOffDateTime);
    }
}
