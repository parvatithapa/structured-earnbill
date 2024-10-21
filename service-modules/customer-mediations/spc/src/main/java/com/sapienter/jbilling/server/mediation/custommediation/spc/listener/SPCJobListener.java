package com.sapienter.jbilling.server.mediation.custommediation.spc.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationCacheInterceptor;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;
import com.sapienter.jbilling.server.mediation.listener.MediationJobListener;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCJobListener extends MediationJobListener {

    @Autowired
    private SPCMediationCacheInterceptor spcMediationCacheInterceptor;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        spcMediationCacheInterceptor.clearCache();
        SPCMediationUtil.clearCache();
        super.beforeJob(jobExecution);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        spcMediationCacheInterceptor.clearCache();
        SPCMediationUtil.clearCache();
        super.afterJob(jobExecution);
    }

}
