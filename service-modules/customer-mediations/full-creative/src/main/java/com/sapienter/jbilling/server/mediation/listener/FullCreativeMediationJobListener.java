package com.sapienter.jbilling.server.mediation.listener;

import org.springframework.batch.core.JobExecution;

import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class FullCreativeMediationJobListener extends MediationJobListener {

	@Override
	public void beforeJob(JobExecution jobExecution) {
		MediationCacheManager.clearCache();
		super.beforeJob(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		MediationCacheManager.clearCache();
		super.afterJob(jobExecution);
	}

}
