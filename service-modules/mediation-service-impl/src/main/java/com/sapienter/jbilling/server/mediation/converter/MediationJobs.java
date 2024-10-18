package com.sapienter.jbilling.server.mediation.converter;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DistributelMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DistributelRecycleJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.performanceMediation.PerformanceMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation.SampleMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation.SampleRecycleJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleRootRateMediation.SampleRecycleRootRateMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleRootRateMediation.SampleRootRateMediationJob;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by marcolin on 08/10/15.
 */
public class MediationJobs {
    private static List<MediationJob> jobs = new ArrayList<>();

    static {{
        addJob(PerformanceMediationJob.getJobInstance());

        addJob(SampleMediationJob.getJobInstance());
        addJob(SampleRecycleJob.getJobInstance());
        addJob(SampleRootRateMediationJob.getJobInstance());
        addJob(SampleRecycleRootRateMediationJob.getJobInstance());
        addJob(SampleMediationJob.getPartitionedJobInstance());
        addJob(DistributelMediationJob.getJobInstance());
        addJob(DistributelRecycleJob.getJobInstance());
    }}

    public static void addJob(MediationJob mediationJob) {
        jobs.add(mediationJob);
    }

    public static List<MediationJob> getJobs() {
        return jobs;
    }

    public static MediationJob getJobForName(String jobName) {
        Optional<MediationJob> job = jobs.stream().filter(j -> j.getJob().equals(jobName)).findFirst();
        if (job.isPresent())
            return job.get();
        return null;
    }

    public static MediationJob getRecycleJobForMediationJob(String jobName) {
        return getJobForName(getJobForName(jobName).getRecycleJob());
    }

}
