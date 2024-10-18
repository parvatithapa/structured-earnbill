package com.sapienter.jbilling.server.integration.common.job.listener;


import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.util.IJobExecutionSessionBean;
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO;


public class MeteredUsageJobListener implements JobExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    IJobExecutionSessionBean jobExecutionService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.debug("MeteredUsageJobListener: started");
        jobExecutionService.startJob(jobExecution.getId(), jobExecution.getJobInstance().getJobName(),
                jobExecution.getStartTime(), Integer.parseInt(jobExecution.getJobParameters().getString(Constants.ENTITY_ID)));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.debug("MeteredUsageJobListener: finished status= {}" ,jobExecution.getStatus().name());
        logger.debug("execution time in ms {}", (jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime()));

        JobExecutionHeaderDTO.Status status = JobExecutionHeaderDTO.Status.ERROR;
        if (jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            status = JobExecutionHeaderDTO.Status.SUCCESS;
        } else if (jobExecution.getExitStatus().equals(ExitStatus.STOPPED)) {
            status = JobExecutionHeaderDTO.Status.STOPPED;
        }
        jobExecutionService.endJob(jobExecution.getId(), jobExecution.getEndTime(), status);
    }
}
