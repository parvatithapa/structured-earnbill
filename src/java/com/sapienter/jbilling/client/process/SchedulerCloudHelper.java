package com.sapienter.jbilling.client.process;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.dao.DuplicateKeyException;

import com.sapienter.jbilling.server.util.Context;

/**
 * Created by marcolin on 30/05/16.
 */
public class SchedulerCloudHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static boolean launchMessageDriveBeanTrackingBatch(Class messageDrivenBean, Message message) {
        Map<String, JobParameter> parametersMap = new HashMap<>();

        String jmsMessageID = "";
        try {
            jmsMessageID = message.getJMSMessageID();
            parametersMap.put("SCHEDULED_JOB_FOR_MDB_HANDLER", new JobParameter(messageDrivenBean.getCanonicalName()));
            parametersMap.put("SCHEDULED_JOB_FOR_MESSAGE_ID", new JobParameter(jmsMessageID));
            JobParameters jobParameters = new JobParameters(parametersMap);
            SimpleJob markingJob = new SimpleJob();
            markingJob.setName("MarkingJobForProvisioning");
            markingJob.setJobRepository(Context.getBean("jobRepository"));
            markingJob.setRestartable(false);
            JobLauncher jobLauncher = Context.getBean("jobLauncher");

            jobLauncher.run(markingJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException e) {
            return false;
        } catch (JobRestartException e) {
            return false;
        } catch (DuplicateKeyException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("There has been a problem during the provisioning, " +
                    "the marking job creation for the provisioning failed");
        }
        logger.debug("Provision done for message id: {}", jmsMessageID);
        return true;    }
}
