package com.sapienter.jbilling.server.mediation.converter.customMediations.performanceMediation;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;

/**
 * Created by marcolin on 08/10/15.
 */
public class PerformanceMediationJob extends DefaultMediationJob{

    protected PerformanceMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver,writer, recycleJob);
    }

    public static MediationJob getJobInstance() {
        return new PerformanceMediationJob("performanceMediationJob", "sampleMediationConverter",
                "sampleRecordMediationCdrResolver", "jmrDefaultWriter", "performanceRecycleMediationJob");
    }

//    public static MediationJob getPartitionedJobInstance() {
//        return new PerformanceMediationJob("partitionedPerformanceMediationJob", "sampleMediationConverter",
//                "sampleRecordMediationCdrResolver", "jmrDefaultWriter", null);
//    }

}
