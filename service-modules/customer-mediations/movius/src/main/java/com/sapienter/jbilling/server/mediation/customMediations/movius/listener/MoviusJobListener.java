package com.sapienter.jbilling.server.mediation.customMediations.movius.listener;

import org.springframework.batch.core.JobExecution;

import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;
import com.sapienter.jbilling.server.mediation.listener.MediationJobListener;

public class MoviusJobListener extends MediationJobListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        MoviusUtil.clearCache();
        super.beforeJob(jobExecution);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        MoviusUtil.clearCache();
        super.afterJob(jobExecution);
    }

}
