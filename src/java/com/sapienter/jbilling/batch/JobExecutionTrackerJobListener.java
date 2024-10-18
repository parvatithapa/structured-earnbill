package com.sapienter.jbilling.batch;

import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.IJobExecutionSessionBean;
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;


public class JobExecutionTrackerJobListener implements JobExecutionListener {

    @Autowired
    IJobExecutionSessionBean jobExecutionService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecutionService.startJob(jobExecution.getId(), jobExecution.getJobInstance().getJobName(),
            jobExecution.getStartTime(), Integer.parseInt( jobExecution.getJobParameters().getString("entityId") ));

        jobExecutionService
                .addLine(jobExecution.getId(), IJobExecutionSessionBean.LINE_TYPE_HEADER, DtConstants.EXEC_STAT_AGG_ZIP_PKG_FILES, "");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        JobExecutionHeaderDTO.Status status = JobExecutionHeaderDTO.Status.ERROR;
        if(jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            status = JobExecutionHeaderDTO.Status.SUCCESS;
        } else if(jobExecution.getExitStatus().equals(ExitStatus.STOPPED)) {
            status = JobExecutionHeaderDTO.Status.STOPPED;
        }
        jobExecutionService.endJob(jobExecution.getId(), jobExecution.getEndTime(), status);

        long mediationJobId = jobExecution.getJobParameters().getLong("mediation_job_id", 0);
        if(mediationJobId > 0) {
            int entityId = Integer.parseInt( jobExecution.getJobParameters().getString("entityId") );

            triggerMediationByConfiguration((int)mediationJobId, entityId);
        }
    }

    public void triggerMediationByConfiguration(Integer cfgId, int entityId) {
        MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
        MediationProcessService mediationProcessService = Context.getBean("mediationProcessService");
        IMediationSessionBean mediationBean = Context
                .getBean(Context.Name.MEDIATION_SESSION);
        MediationConfiguration configuration = mediationBean.getMediationConfiguration(cfgId);

        mediationService.launchMediation(entityId, cfgId,
                configuration.getMediationJobLauncher());
    }
}
