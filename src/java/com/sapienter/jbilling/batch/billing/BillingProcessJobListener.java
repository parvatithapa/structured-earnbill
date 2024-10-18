package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.Resource;

import com.sapienter.jbilling.server.scheduledTask.event.ScheduledJobNotificationEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;

import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.BillingProcessInfoBL;
import com.sapienter.jbilling.server.process.BillingProcessFailedUserBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask;
import com.sapienter.jbilling.server.process.task.IBillingProcessFilterTask;
import com.sapienter.jbilling.server.util.Constants;

/**
 *
 * @author Khobab
 */
public class BillingProcessJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String JOBCONTEXT_BILLING_PROCESS_ID_KEY = "billingProcessId";

    @Resource
    private JobRepository jobRepository;
    @Resource
    private IBillingProcessSessionBean local;
    @Resource
    private BillingProcessDAS billingProcessDAS;
    @Resource
    private BillingProcessInfoBL batchProcessInfoBL;
    @Resource
    private BillingBatchService jdbcService;
    @Resource
    private BasicBillingProcessFilterTask basicBillingProcessFilterTask;

    /**
     * Moves job execution context data to database tables at the end of billing process
     */
    @Override
    public void afterJob (JobExecution jobExecution) {

        Integer billingProcessId = jobExecution.getExecutionContext().getInt(JOBCONTEXT_BILLING_PROCESS_ID_KEY);

        int totalUsersSuccessful = jdbcService.countSuccessfulUsers(billingProcessId, jobExecution.getId());
        int totalUsersFailed = jdbcService.countFailedUsers(billingProcessId);

        logger.debug("Billing process ID: {}, jobExecution ID: {}. Totals: successful users: {} ,failed users: {}",
                billingProcessId, jobExecution.getId(), totalUsersSuccessful, totalUsersFailed);

        Integer batchProcessInfoId = batchProcessInfoBL
                .create(billingProcessId, jobExecution.getId().intValue(), totalUsersFailed, totalUsersSuccessful)
                .getId();

        // if there are failed users mark the job as FAILED so it could be restarted
        if (totalUsersFailed > 0) {
            BillingProcessFailedUserBL failedUserBL = new BillingProcessFailedUserBL();
            for (Integer failed : jdbcService.listFailedUsers(billingProcessId)) {
                failedUserBL.create(batchProcessInfoId, failed);
            }
            logger.debug("There are # {} failed users in job # {}, marking job as failed.", totalUsersFailed,
                    jobExecution.getJobId());
            jobExecution.setStatus(BatchStatus.FAILED);
        } else {
            jdbcService.cleanupBillingProcessData(billingProcessId);
        }

        logger.debug("BillingProcessJobListener : afterJob");
        try {
            Integer entityId = jobExecution.getJobParameters().getLong(Constants.BATCH_JOB_PARAM_ENTITY_ID).intValue();
            logger.debug("entityId : {}", entityId);
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "BillingProcess",
                    jobExecution, ScheduledJobNotificationEvent.TaskEventType.AFTER_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on afterJob for Billing Process Listener");
        }
    }

    /**
     * if first execution 1. Creates a BillingProcessDTO record if first execution 2. findUsersToProcess(entityId,
     * billingDate) 3. stores user IDs to batch billing table
     */
    @Override
    public void beforeJob (JobExecution jobExecution) {
        JobParameters jobParams = jobExecution.getJobParameters();

        Integer entityId = jobParams.getLong(Constants.BATCH_JOB_PARAM_ENTITY_ID).intValue();
        Date billingDate = jobParams.getDate(BatchConstants.PARAM_BILLING_DATE);
        boolean review = jobParams.getLong(BatchConstants.PARAM_REVIEW) == 1L;

        BillingProcessDTO billingProcess = billingProcessDAS.isPresent(entityId, review ? 1 : 0, billingDate);

        boolean createBillingProcessData = ( null == billingProcess || review );

        Integer billingProcessId = createBillingProcessData
                ? createBillingProcessDTO(entityId, billingDate, review, jobParams).getId()
                : billingProcess.getId();

        logger.debug("Job will use billing process with id: {}", billingProcessId);

        if (createBillingProcessData) {
            jdbcService.createBillingProcessData(
                    billingProcessFilter(entityId).findUsersToProcess(entityId, billingDate), billingProcessId);
        } else {
            jobExecution.getExecutionContext().putInt("restart", 1);
        }

        jobExecution.getExecutionContext().putInt(JOBCONTEXT_BILLING_PROCESS_ID_KEY, billingProcessId);

        jobRepository.updateExecutionContext(jobExecution);
        logger.debug("BillingProcessJobListener : beforeJob");
        try {
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "BillingProcess",
                    jobExecution, ScheduledJobNotificationEvent.TaskEventType.BEFORE_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on beforeJob for Billing Process Listener");
        }
    }

    private BillingProcessDTO createBillingProcessDTO (Integer entityId, Date billingDate, boolean review,
            JobParameters jobParams) {

        Integer periodType = jobParams.getLong(BatchConstants.PARAM_PERIOD_TYPE).intValue();
        Integer periodValue = jobParams.getLong(BatchConstants.PARAM_PERIOD_VALUE).intValue();

        ConfigurationBL conf = new ConfigurationBL(entityId);
        BillingProcessDTO result = local.createProcessRecord(entityId, billingDate, periodType, periodValue, review,
                conf.getEntity().getRetries());

        billingProcessDAS.reattachUnmodified(result);

        return result;
    }

    private IBillingProcessFilterTask billingProcessFilter (Integer entityId) {
        PluggableTaskManager<IBillingProcessFilterTask> taskManager = null;
        try {
            taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_BILL_PROCESS_FILTER);
        } catch (PluggableTaskException e) {
            // eat it
        }
        IBillingProcessFilterTask task = null;
        try {
            if (taskManager != null) {
                task = taskManager.getNextClass();
            }
        } catch (PluggableTaskException e) {
            // eat it
        }
        // if one was not configured just use the basic task by default
        if (task == null) {
            logger.debug("No filter was found, initializing basic filter");
            task = basicBillingProcessFilterTask;
        }
        return task;
    }
}
