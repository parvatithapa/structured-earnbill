package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job;

import static com.sapienter.jbilling.server.mediation.converter.MediationJobs.addJob;

public class MediationJobsInit {

    static {{
        //DT Mediation Jobs
        addJob(DtOfflineMediationJob.getMediationJobInstance());
        addJob(DtOfflineMediationJob.getRecycleJobInstance());

    }}
}
