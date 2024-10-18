package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleRootRateMediation;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation.SampleMediationJob;

/**
 * Created by marcomanzicore on 25/11/15.
 */
public class SampleRootRateMediationJob extends SampleMediationJob {

    private SampleRootRateMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver,writer, recycleJob);
    }

    public static MediationJob getJobInstance() {
        return new SampleRootRateMediationJob("sampleRootRouteMediationJob", "sampleMediationConverter",
                "sampleRootRateCdrResolver", "jmrDefaultWriter", "sampleRootRouteRecycleJob");
    }

    @Override
    public boolean handleRootRateTables() {
        return true;
    }
}
