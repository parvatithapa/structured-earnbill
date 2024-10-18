package com.sapienter.jbilling.server.mediation.custommediation.spc.listener;

import org.springframework.batch.core.JobExecution;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;
import com.sapienter.jbilling.server.mediation.listener.RecycleMediationJobListener;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCRecycleJobListener extends RecycleMediationJobListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
    	SPCMediationUtil.clearCache();
        super.beforeJob(jobExecution);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
    	SPCMediationUtil.clearCache();
        super.afterJob(jobExecution);
    }
}
