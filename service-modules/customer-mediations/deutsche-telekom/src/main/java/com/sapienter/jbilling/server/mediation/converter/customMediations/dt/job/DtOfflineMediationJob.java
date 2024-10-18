package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants;

public class DtOfflineMediationJob extends DefaultMediationJob {

    protected DtOfflineMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
    	super(job, lineConverter, resolver,writer, recycleJob);
    }

    @Override
    public boolean handleRootRateTables() {
        return false;
    }

    @Override
    public boolean needsInputDirectory() {
        return true;
    }

    public static MediationJob getMediationJobInstance() {
        return new DtOfflineMediationJob(DtConstants.OFFLINE_CDR_JOB,
                DtConstants.OFFLINE_CDR_CONVERTER,
                DtConstants.OFFLINE_CDR_RESOLVER,
                DtConstants.JMR_DEFAULT_WRITER_BEAN,
        		DtConstants.OFFLINE_CDR_RECYCLE_JOB);
    }
    
    public static MediationJob getRecycleJobInstance() {
        return new DtOfflineMediationJob(DtConstants.OFFLINE_CDR_RECYCLE_JOB,
                DtConstants.OFFLINE_CDR_CONVERTER,
                DtConstants.OFFLINE_CDR_RESOLVER,
                DtConstants.JMR_DEFAULT_WRITER_BEAN,
                null);
    }
}