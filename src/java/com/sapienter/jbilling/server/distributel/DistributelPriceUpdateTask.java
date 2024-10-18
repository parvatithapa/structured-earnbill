package com.sapienter.jbilling.server.distributel;

import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_DATA_TABLE_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_FUTURE_PROCESSING_DATE;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_JOB_LAUNCHER_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_JOB_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_ORDER_LEVEL_MF_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_PROCESSING_DATE_NAME;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import com.sapienter.jbilling.batch.billing.BillingBatchJobService;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class DistributelPriceUpdateTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_TABLE_NAME = new ParameterDescription("data_table_name", true,
            ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_NOTIFICATION_EMAIL_ID = new ParameterDescription(
            "notification_email_id", false, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_DATE_FORMAT = new ParameterDescription("date_format", false,
            ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_NOTE_TITLE = new ParameterDescription("note_title", false,
            ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_ORDER_LEVEL_META_FIELD_NAME = new ParameterDescription(
            "order_level_mf_name", false, ParameterDescription.Type.STR);

    public DistributelPriceUpdateTask() {
        descriptions.add(PARAM_TABLE_NAME);
        descriptions.add(PARAM_NOTIFICATION_EMAIL_ID);
        descriptions.add(PARAM_DATE_FORMAT);
        descriptions.add(PARAM_NOTE_TITLE);
        descriptions.add(PARAM_ORDER_LEVEL_META_FIELD_NAME);
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    /**
     * Calculates ends days based on processingDate
     *
     * @param processingDate
     * @return
     */
    private List<String> calculateFutureProcessingDate(Date processingDate) {
        String datePattern = getParameter(PARAM_DATE_FORMAT.getName(), DistributelPriceJobConstants.DEFAULT_DATE_FORMAT);
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        Calendar processingCal = Calendar.getInstance();
        processingCal.setTime(processingDate);
        int procesingMonthMaxDays = processingCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        boolean isProcessingEndOfMonth = processingCal.get(Calendar.DAY_OF_MONTH) == procesingMonthMaxDays;
        Calendar futureProcessingDate = Calendar.getInstance();
        futureProcessingDate.setTime(processingDate);
        futureProcessingDate.add(Calendar.MONTH, DistributelPriceJobConstants.MONTH_TO_ADD);
        if (!isProcessingEndOfMonth) {
            return Arrays.asList(dateFormat.format(futureProcessingDate.getTime()));
        }
        List<String> futureDays = new ArrayList<>();
        int futureMonthMaxDays = futureProcessingDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        futureDays.add(dateFormat.format(futureProcessingDate.getTime())); // add
                                                                           // initial
                                                                           // future
                                                                           // day
        while (futureProcessingDate.get(Calendar.DAY_OF_MONTH) != futureMonthMaxDays) {
            // Adding rest of remaining days
            futureProcessingDate.add(Calendar.DAY_OF_MONTH, DistributelPriceJobConstants.DAY_TO_ADD);
            futureDays.add(dateFormat.format(futureProcessingDate.getTime()));
        }
        return futureDays;
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            _init(context);
            Integer entityId = getEntityId();
            if (isJobRunning()) {
                logger.warn("Skipping task for entity {} because one price update job is already running ", entityId);
                return;
            }
            logger.debug("Executing {} for entity {}", getTaskName(), entityId);
            String tableName = getParameter(PARAM_TABLE_NAME.getName(), "");
            if (StringUtils.isEmpty(tableName)) {
                logger.warn("Skipping Since table name is empty for entity {}", entityId);
                return;
            }

            MediationHelperService service = Context.getBean(MediationHelperService.class);
            if (!service.isTablePresent(tableName)) {
                logger.warn("Skipping Since table name not found for entity {}", entityId);
                return;
            }
            String notificationEmailId = getParameter(PARAM_NOTIFICATION_EMAIL_ID.getName(), "");
            JobLauncher jobLauncher = Context.getBean(PARAM_JOB_LAUNCHER_NAME);
            Job job = Context.getBean(PARAM_JOB_NAME);
            String dateFromat = getParameter(PARAM_DATE_FORMAT.getName(),
                    DistributelPriceJobConstants.DEFAULT_DATE_FORMAT);
            DateFormat dateFormat = new SimpleDateFormat(dateFromat);
            Date currentDate = TimezoneHelper.companyCurrentDate(entityId);
            String processingDate = dateFormat.format(currentDate);
            List<String> futureDates = calculateFutureProcessingDate(currentDate);

            String noteTitle = getParameter(PARAM_NOTE_TITLE.getName(), DistributelPriceJobConstants.DEFAULT_NOTE_TITLE);
            String orderLevelMfname = getParameter(PARAM_ORDER_LEVEL_META_FIELD_NAME.getName(), StringUtils.EMPTY);
            JobParameters param = new JobParametersBuilder().addString(PARAM_DATA_TABLE_NAME, tableName)
                    .addString(PARAM_NOTIFICATION_EMAIL_ID.getName(), notificationEmailId)
                    .addString(PARAM_PROCESSING_DATE_NAME, processingDate)
                    .addString(PARAM_ENTITY_ID, entityId.toString())
                    .addString(PARAM_FUTURE_PROCESSING_DATE, futureDates.stream().collect(Collectors.joining(",")))
                    .addString(PARAM_NOTE_TITLE.getName(), noteTitle)
                    .addString(PARAM_ORDER_LEVEL_MF_NAME, orderLevelMfname)
                    .addString("id", UUID.randomUUID().toString()) // for unique
                                                                   // parameter
                                                                   // generation.
                    .toJobParameters();
            logger.debug("Job Parameters for entity {} {}", getEntityId(), param);
            JobExecution jobExecution = jobLauncher.run(job, param);
            logger.debug("Job started for entity {} with job execution id {} at {} ", getEntityId(),
                    jobExecution.getId(), jobExecution.getStartTime());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            logger.error("Job {} failed because of {}", "distributelPriceUpdateJob", e);
        } catch (Exception e) {
            logger.error("Failed Job!", e);
        }

    }

    private boolean isJobRunning() {
        BillingBatchJobService batchService = Context.getBean(BillingBatchJobService.class);
        return batchService.isJobRunning(DistributelPriceJobConstants.PARAM_JOB_NAME);
    }

}
