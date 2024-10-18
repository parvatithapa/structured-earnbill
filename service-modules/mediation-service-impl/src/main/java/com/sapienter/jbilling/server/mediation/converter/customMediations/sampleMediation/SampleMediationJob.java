package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;

/**
 * Created by marcolin on 08/10/15.
 */
public class SampleMediationJob extends DefaultMediationJob{

    protected SampleMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver,writer, recycleJob);
    }

    public static MediationJob getJobInstance() {
        return new SampleMediationJob("sampleMediationJob", "sampleMediationConverter",
                "sampleRecordMediationCdrResolver", "jmrDefaultWriter", "sampleRecycleJob");
    }

    public static MediationJob getPartitionedJobInstance() {
        return new SampleMediationJob("partitionedSampleMediationJob", "sampleMediationConverter",
                "sampleRecordMediationCdrResolver", "jmrDefaultWriter", "sampleRecycleJob");
    }

}
