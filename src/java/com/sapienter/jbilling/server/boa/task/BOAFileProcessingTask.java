package com.sapienter.jbilling.server.boa.task;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Javier Rivero
 * @since 05/01/16.
 */
public class BOAFileProcessingTask extends AbstractCronTask {
    // BOAFileProcessingTask task parameters names
    public static final ParameterDescription    PARAMETER_READ_FROM_DAILY_FILES_DIRECTORY = new ParameterDescription(Constants.BOA_JOB_PARAM_READ_FROM_DAILY_FILES_DIRECTORY, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_READ_FROM_INTRADAY_FILES_DIRECTORY = new ParameterDescription(Constants.BOA_JOB_PARAM_READ_FROM_INTRADAY_FILES_DIRECTORY, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_MOVE_TO_DAILY_FILES_DIRECTORY = new ParameterDescription(Constants.BOA_JOB_PARAM_MOVE_TO_DAILY_FILES_DIRECTORY, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_MOVE_TO_INTRADAY_FILES_DIRECTORY = new ParameterDescription(Constants.BOA_JOB_PARAM_MOVE_TO_INTRADAY_FILES_DIRECTORY, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_DEFAULT_USER_ID = new ParameterDescription(Constants.BOA_JOB_PARAM_DEFAULT_USER_ID, true, ParameterDescription.Type.INT);
    public static final String BANK_FILES_DIR = "bank_files";
    public static final String RESOURCES_PATH = "/resources";
    //initializer for BOAFileProcessingTask params
    {
        descriptions.add(PARAMETER_READ_FROM_DAILY_FILES_DIRECTORY);
        descriptions.add(PARAMETER_READ_FROM_INTRADAY_FILES_DIRECTORY);
        descriptions.add(PARAMETER_MOVE_TO_DAILY_FILES_DIRECTORY);
        descriptions.add(PARAMETER_MOVE_TO_INTRADAY_FILES_DIRECTORY);
        descriptions.add(PARAMETER_DEFAULT_USER_ID);
    }

    private static final Logger LOG = Logger.getLogger(BOAFileProcessingTask.class);
    private String readFromDailyFilesDirectory;
    private String readFromIntradayFilesDirectory;
    private String moveToDailyFilesDirectory;
    private String moveToIntradayFilesDirectory;
    private String defaultUserId;
    private String jbillingHome =  Util.getSysProp("base_dir").replace(RESOURCES_PATH, "");
    private String bankFilesDir = jbillingHome + BANK_FILES_DIR;

    public String getTaskName() {
        return "BOA File Processing Task : " + getScheduleString();
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        _init(jobExecutionContext);
        String companyId = String.valueOf(this.getEntityId());
        // get report_path parameters
        readFromDailyFilesDirectory = bankFilesDir + File.separator + companyId + File.separator + getParameter(PARAMETER_READ_FROM_DAILY_FILES_DIRECTORY.getName());
        readFromIntradayFilesDirectory = bankFilesDir + File.separator + companyId + File.separator + getParameter(PARAMETER_READ_FROM_INTRADAY_FILES_DIRECTORY.getName());
        moveToDailyFilesDirectory = bankFilesDir + File.separator + companyId + File.separator + getParameter(PARAMETER_MOVE_TO_DAILY_FILES_DIRECTORY.getName());
        moveToIntradayFilesDirectory = bankFilesDir + File.separator + companyId + File.separator + getParameter(PARAMETER_MOVE_TO_INTRADAY_FILES_DIRECTORY.getName());
        defaultUserId = getParameter(PARAMETER_DEFAULT_USER_ID.getName());

        if (StringUtils.trimToNull(readFromDailyFilesDirectory) != null) {
            File dailyDirectory = new File(readFromDailyFilesDirectory);
            if (!dailyDirectory.exists()) {
                dailyDirectory.mkdir();
            }

        } else {
            throw new JobExecutionException("Remote path does not exist. Please provide correct remote path");
        }

        if (StringUtils.trimToNull(readFromIntradayFilesDirectory) != null) {
            File intraDailyDirectory = new File(readFromIntradayFilesDirectory);
            if (!intraDailyDirectory.exists()) {
                intraDailyDirectory.mkdir();
            }

        } else {
            throw new JobExecutionException("Remote path does not exist. Please provide correct remote path");
        }

        if (StringUtils.trimToNull(moveToDailyFilesDirectory) != null) {
            File moveToDailyDirectory = new File(moveToDailyFilesDirectory);
            if (!moveToDailyDirectory.exists()) {
                moveToDailyDirectory.mkdir();
            }

        } else {
            throw new JobExecutionException("Remote path does not exist. Please provide correct remote path");
        }

        if (StringUtils.trimToNull(moveToIntradayFilesDirectory) != null) {
            File moveToIntraDailyDirectory = new File(moveToIntradayFilesDirectory);
            if (!moveToIntraDailyDirectory.exists()) {
                moveToIntraDailyDirectory.mkdir();
            }

        } else {
            throw new JobExecutionException("Remote path does not exist. Please provide correct remote path");
        }

        JobLauncher launcher = Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
        Job job = Context.getBean(Context.Name.BATCH_JOB_BAI_FILE_PROCESSING);
        JobParametersBuilder paramBuilder = new JobParametersBuilder().addString(Constants.BOA_JOB_PARAM_READ_FROM_DAILY_FILES_DIRECTORY, readFromDailyFilesDirectory);
        paramBuilder.addString(Constants.BOA_JOB_PARAM_READ_FROM_INTRADAY_FILES_DIRECTORY, readFromIntradayFilesDirectory);
        paramBuilder.addString(Constants.BOA_JOB_PARAM_MOVE_TO_DAILY_FILES_DIRECTORY, moveToDailyFilesDirectory);
        paramBuilder.addString(Constants.BOA_JOB_PARAM_MOVE_TO_INTRADAY_FILES_DIRECTORY, moveToIntradayFilesDirectory);
        paramBuilder.addString(Constants.BOA_JOB_PARAM_DEFAULT_USER_ID, defaultUserId);
        //Following 'time' parameter added to avoid JobInstanceAlreadyCompleteException thrown if job is ran with same parameters
        paramBuilder.addDate("time", TimezoneHelper.serverCurrentDate());
        JobParameters jobParameters = paramBuilder.toJobParameters();
        long startTime = System.currentTimeMillis();
        try {
            launcher.run(job, jobParameters);
        } catch (JobExecutionAlreadyRunningException e) {
            throw new SessionInternalError(e);
        } catch (JobRestartException e) {
            throw new SessionInternalError(e);
        } catch (JobInstanceAlreadyCompleteException e) {
            throw new SessionInternalError(e);
        } catch (JobParametersInvalidException e) {
            throw new SessionInternalError(e);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("[BOAFileProcessingTask] - bankJob completed in: " + (endTime - startTime) / 1000 + " seconds");
    }

    public String getParameter(String key) throws JobExecutionException {
        String value = (String) parameters.get(key);
        LOG.info("In getParameter with key=" + key + " and value=" + value);
        if (value == null || value.trim().equals(""))
            throw new JobExecutionException("parameter '" + key + "' cannot be blank!");
        return value;
    }
}
