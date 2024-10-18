package com.sapienter.jbilling.server.distributel;

import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_JOB_LAUNCHER_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_PRICE_INCREASE_DATA_TABLE_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_PRICE_INCREASE_JOB_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_PRICE_REVERSAL_DATA_TABLE_NAME;
import static com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants.PARAM_PROCESSING_DATE_NAME;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

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

public class DistributelPriceIncreaseAndReversalTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles
            .lookup().lookupClass());

    private static final ParameterDescription PARAM_NOTIFICATION_EMAIL_ID = new ParameterDescription(
            "notification_email_id", false, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_PRICE_INCREASE_TABLE_NAME = new ParameterDescription(
            "price_increase_data_table", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_PRICE_REVERSAL_TABLE_NAME = new ParameterDescription(
            "price_reversal_data_table", false, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_DATE_FORMAT = new ParameterDescription(
            "date_format", false, ParameterDescription.Type.STR);

    public DistributelPriceIncreaseAndReversalTask() {
        descriptions.add(PARAM_NOTIFICATION_EMAIL_ID);
        descriptions.add(PARAM_PRICE_INCREASE_TABLE_NAME);
        descriptions.add(PARAM_PRICE_REVERSAL_TABLE_NAME);
        descriptions.add(PARAM_DATE_FORMAT);
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
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
            String priceIncreaseTable = getParameter(PARAM_PRICE_INCREASE_TABLE_NAME.getName(), "");
            if (StringUtils.isEmpty(priceIncreaseTable)) {
                logger.warn("Skipping Since table name is not provided in plugin for entity {}", entityId);
                return;
            }
            String priceDecreaseTable = getParameter(PARAM_PRICE_REVERSAL_TABLE_NAME.getName(), "");

            MediationHelperService service = Context.getBean(MediationHelperService.class);
            if (!service.isTablePresent(priceIncreaseTable)) {
                logger.warn("Skipping Since table name not found for entity {}",entityId);
                return;
            }
            if (StringUtils.isNotEmpty(priceDecreaseTable) && !service.isTablePresent(priceDecreaseTable)) {
                logger.warn("Skipping Since table name not found for entity {}", entityId);
                return;
            }
            String notificationEmailId = getParameter(PARAM_NOTIFICATION_EMAIL_ID.getName(), "");
            JobLauncher jobLauncher = Context.getBean(PARAM_JOB_LAUNCHER_NAME);
            Job job = Context.getBean(PARAM_PRICE_INCREASE_JOB_NAME);
            String dateFromat = getParameter(PARAM_DATE_FORMAT.getName(), DistributelPriceJobConstants.DEFAULT_DATE_FORMAT);
            DateFormat dateFormat = new SimpleDateFormat(dateFromat);
            Date currentDate = TimezoneHelper.companyCurrentDate(entityId);
            String processingDate = dateFormat.format(currentDate);
            JobParametersBuilder params = new JobParametersBuilder()
            .addString(PARAM_PRICE_INCREASE_DATA_TABLE_NAME, priceIncreaseTable)
            .addString(PARAM_NOTIFICATION_EMAIL_ID.getName(), notificationEmailId)
            .addString(PARAM_PROCESSING_DATE_NAME, processingDate)
            .addString(PARAM_ENTITY_ID, entityId.toString())
            .addString("id", UUID.randomUUID().toString()); // for unique parameter generation.
            if(StringUtils.isNotEmpty(priceDecreaseTable)) {
                params.addString(PARAM_PRICE_REVERSAL_DATA_TABLE_NAME, priceDecreaseTable);
            }
            JobParameters parameters = params.toJobParameters();
            logger.debug("Job Parameters for entity {} {}", getEntityId(), parameters);
            JobExecution jobExecution = jobLauncher.run(job, parameters);
            logger.debug("Job started for entity {} with job execution id {} at {} ", getEntityId(), jobExecution.getId(),
                    jobExecution.getStartTime());
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            logger.error("Job {} failed because of {}",
                    "distributelPriceIncreaseAndReverseJob", e);
        } catch (Exception e) {
            logger.error("Failed Job!", e);
        }
    }

    private boolean isJobRunning() {
        BillingBatchJobService batchService = Context.getBean(BillingBatchJobService.class);
        return batchService.isJobRunning(DistributelPriceJobConstants.PARAM_PRICE_INCREASE_JOB_NAME);
    }
}
