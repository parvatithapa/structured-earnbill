package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;

/**
 * Created by andres on 27/10/15.
 */
public class SampleRecycleJob extends SampleMediationJob {

    private SampleRecycleJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver,writer, recycleJob);
    }

    public static MediationJob getJobInstance() {
        return new SampleRecycleJob("sampleRecycleJob", "sampleMediationConverter",
                "sampleRecordMediationCdrResolver", "jmrDefaultWriter", null);
    }

}
