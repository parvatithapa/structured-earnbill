package com.sapienter.jbilling.batch.support;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.transaction.annotation.Transactional;

public abstract class PartitionedJobListener implements JobExecutionListener {

    @Resource
    private PartitionService partitionService;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void afterJob (JobExecution jobExecution) {
        logger.trace("Cleanup users Ids for job {}", jobExecution.getJobId());
        partitionService.cleanupJobUsersIds(jobExecution.getJobId());
    }

    @Override
    @Transactional
    public void beforeJob (JobExecution jobExecution) {
        logger.trace("Creating users Ids for job {}", jobExecution.getJobId());
        partitionService.createJobUsersIds(jobExecution.getJobId(), findUsersForJob());
    }

    abstract protected ScrollableResults findUsersForJob ();
}
