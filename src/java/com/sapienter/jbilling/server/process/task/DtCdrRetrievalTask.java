package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.MediationJobs;
import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.sourcereader.AbstractRemoteFileRetriever;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.core.task.AsyncTaskExecutor;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DtCdrRetrievalTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final String PARAM_SERVER = "server";
    protected static final String PARAM_SERVER_PORT = "server_port";
    protected static final String PARAM_SERVER_USER = "user";
    protected static final String PARAM_SERVER_PASSWORD = "password";
    protected static final String PARAM_REMOTE_PATH = "remote_path";
    protected static final String PARAM_SHARED_WORK_FOLDER = "shared_work_folder";
    protected static final String PARAM_LOCAL_WORK_FOLDER = "local_work_folder";
    protected static final String PARAM_BACKUP_FOLDER = "backup_folder";
    protected static final String PARAM_FILE_NAME_REGEX = "file_name_regex";
    protected static final String PARAM_RECURSIVE = "recursive";
    protected static final String PARAM_MEDIATION_JOB = "mediation_job_id";
    protected static final String PARAM_FTP_METHOD = "ftp_method";
    protected static final String PARAM_SSH_KEY_FILE = "ssh_key_file";
    protected static final String PARAM_SSH_KEY_FILE_PASSPHRASE = "ssh_key_file_passphrase";
    protected static final String PARAM_DECRYPT = "decrypt";
    protected static final String PARAM_GPG_PASSWORD = "gpg_password";

    protected static final ParameterDescription PARAM_SERVER_DESC =
            new ParameterDescription(PARAM_SERVER, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SERVER_PORT_DESC =
            new ParameterDescription(PARAM_SERVER_PORT, true, ParameterDescription.Type.INT, "21");
    protected static final ParameterDescription PARAM_SERVER_USER_DESC=
            new ParameterDescription(PARAM_SERVER_USER, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SERVER_PASSWORD_DESC =
            new ParameterDescription(PARAM_SERVER_PASSWORD, false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_REMOTE_DESC =
            new ParameterDescription(PARAM_REMOTE_PATH, true, ParameterDescription.Type.STR , "/");
    protected static final ParameterDescription PARAM_LOCAL_BACKUP_FOLDER_DESC =
            new ParameterDescription(PARAM_BACKUP_FOLDER, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SHARED_WORK_FOLDER_DESC =
            new ParameterDescription(PARAM_SHARED_WORK_FOLDER , true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_LOCAL_WORK_FOLDER_DESC =
            new ParameterDescription(PARAM_LOCAL_WORK_FOLDER , true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_FILE_NAME_REGEX_DESC =
            new ParameterDescription(PARAM_FILE_NAME_REGEX, true, ParameterDescription.Type.STR, ".*pgp");
    protected static final ParameterDescription PARAM_RECURSIVE_DESC=
            new ParameterDescription(PARAM_RECURSIVE, false, ParameterDescription.Type.BOOLEAN, "false");
    protected static final ParameterDescription PARAM_MEDIATION_JOB_DESC=
            new ParameterDescription(PARAM_MEDIATION_JOB, false, ParameterDescription.Type.INT);
    protected static final ParameterDescription PARAM_FTP_METHOD_DESC =
            new ParameterDescription(PARAM_FTP_METHOD, false, ParameterDescription.Type.STR, "ftp");
    protected static final ParameterDescription PARAM_SSH_KEY_FILE_DESC =
            new ParameterDescription(PARAM_SSH_KEY_FILE, false, ParameterDescription.Type.STR, "~/.ssh/id_rsa");
    protected static final ParameterDescription PARAM_SSH_KEY_FILE_PASSPHRASE_DESC =
            new ParameterDescription(PARAM_SSH_KEY_FILE_PASSPHRASE, false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_DECRYPT_DESC=
            new ParameterDescription(PARAM_DECRYPT, true, ParameterDescription.Type.BOOLEAN, "true");
    protected static final ParameterDescription PARAM_GPG_PASSWORD_DESC=
            new ParameterDescription(PARAM_GPG_PASSWORD, false, ParameterDescription.Type.STR);

    {
        descriptions.add(PARAM_REMOTE_DESC);
        descriptions.add(PARAM_LOCAL_BACKUP_FOLDER_DESC);
        descriptions.add(PARAM_SHARED_WORK_FOLDER_DESC);
        descriptions.add(PARAM_LOCAL_WORK_FOLDER_DESC);
        descriptions.add(PARAM_SERVER_DESC);
        descriptions.add(PARAM_SERVER_PORT_DESC);
        descriptions.add(PARAM_SERVER_USER_DESC);
        descriptions.add(PARAM_SERVER_PASSWORD_DESC);
        descriptions.add(PARAM_FILE_NAME_REGEX_DESC);
        descriptions.add(PARAM_RECURSIVE_DESC);
        descriptions.add(PARAM_MEDIATION_JOB_DESC);
        descriptions.add(PARAM_FTP_METHOD_DESC);
        descriptions.add(PARAM_SSH_KEY_FILE_DESC);
        descriptions.add(PARAM_SSH_KEY_FILE_PASSPHRASE_DESC);
        descriptions.add(PARAM_DECRYPT_DESC);
        descriptions.add(PARAM_GPG_PASSWORD_DESC);
    }

    @Override
    public String getTaskName() {
        return "DtCdrRetrievalTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void doExecute(JobExecutionContext context)
            throws JobExecutionException {
        try {
            JobDataMap parms = context.getMergedJobDataMap();

            JobLauncher jobLauncher = (JobLauncher) Context.getBean("jobLauncher");
            Map<String, JobParameter> parametersMap = new HashMap<>();
            parametersMap.put(PARAM_SERVER, new JobParameter(parms.getString(PARAM_SERVER)));
            parametersMap.put("date", new JobParameter(new Date()));
            parametersMap.put(PARAM_SERVER_PORT, new JobParameter(parms.getString(PARAM_SERVER_PORT)));
            parametersMap.put(PARAM_SERVER_USER, new JobParameter(parms.getString(PARAM_SERVER_USER)));
            parametersMap.put(PARAM_SERVER_PASSWORD, new JobParameter(parms.getString(PARAM_SERVER_PASSWORD)));
            parametersMap.put(PARAM_REMOTE_PATH, new JobParameter(parms.getString(PARAM_REMOTE_PATH)));
            parametersMap.put(PARAM_FILE_NAME_REGEX, new JobParameter(parms.getString(PARAM_FILE_NAME_REGEX)));
            parametersMap.put(PARAM_RECURSIVE, new JobParameter(parms.getString(PARAM_RECURSIVE)));
            parametersMap.put(PARAM_BACKUP_FOLDER, new JobParameter(parms.getString(PARAM_BACKUP_FOLDER)));
            parametersMap.put(PARAM_SHARED_WORK_FOLDER, new JobParameter(parms.getString(PARAM_SHARED_WORK_FOLDER)));
            parametersMap.put(PARAM_LOCAL_WORK_FOLDER, new JobParameter(parms.getString(PARAM_LOCAL_WORK_FOLDER )));
            parametersMap.put(PARAM_FTP_METHOD, new JobParameter(parms.getString(PARAM_FTP_METHOD)));
            parametersMap.put(PARAM_SSH_KEY_FILE, new JobParameter(parms.getString(PARAM_SSH_KEY_FILE)));
            parametersMap.put(PARAM_SSH_KEY_FILE_PASSPHRASE, new JobParameter(parms.getString(PARAM_SSH_KEY_FILE_PASSPHRASE)));
            parametersMap.put(PARAM_DECRYPT, new JobParameter(parms.getString(PARAM_DECRYPT)));
            parametersMap.put(PARAM_GPG_PASSWORD, new JobParameter(parms.getString(PARAM_GPG_PASSWORD)));

            if(parms.containsKey(PARAM_MEDIATION_JOB) && parms.getLongFromString(PARAM_MEDIATION_JOB) > 0) {
                parametersMap.put(PARAM_MEDIATION_JOB, new JobParameter(parms.getLongFromString(PARAM_MEDIATION_JOB)));
            }
            parametersMap.put("entityId", new JobParameter(getEntityId().toString()));

            JobParameters jobParameters = new JobParameters(parametersMap);

            jobLauncher.run((Job) Context.getBean("dtCdrCollection"), jobParameters);

        } catch (Exception e) {
            logger.error("Problem executing DtCdrRetrievalTask", e);
        }
    }


}
