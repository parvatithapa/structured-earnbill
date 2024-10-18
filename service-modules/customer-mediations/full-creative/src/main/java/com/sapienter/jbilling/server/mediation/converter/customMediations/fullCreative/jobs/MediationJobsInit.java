package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs;

import static com.sapienter.jbilling.server.mediation.converter.MediationJobs.addJob;

/**
 * Created by marcolin on 23/05/16.
 */
public class MediationJobsInit {

    static {{
        //FullCreative Mediation Jobs
        addJob(InboundCallMediationJob.getMediationJobInstance());
        addJob(InboundCallMediationJob.getRecycleJobInstance());

        addJob(ChatMediationJob.getMediationJobInstance());
        addJob(ChatMediationJob.getRecycleJobInstance());

        addJob(ActiveResponseMediationJob.getMediationJobInstance());
        addJob(ActiveResponseMediationJob.getRecycleJobInstance());

        addJob(IvrMediationJob.getMediationJobInstance());
        addJob(IvrMediationJob.getRecycleJobInstance());

        addJob(SpanishMediationJob.getMediationJobInstance());
        addJob(SpanishMediationJob.getRecycleJobInstance());

        addJob(SupervisorMediationJob.getMediationJobInstance());
        addJob(SupervisorMediationJob.getRecycleJobInstance());

        addJob(CallRelayMediationJob.getMediationJobInstance());
        addJob(CallRelayMediationJob.getRecycleJobInstance());

        addJob(LiveReceptionMediationJob.getMediationJobInstance());
        addJob(LiveReceptionMediationJob.getRecycleJobInstance());

        addJob(FullCreativeMediationJob.getMediationJobInstance());
        addJob(FullCreativeMediationJob.getRecycleJobInstance());
    }}
}
