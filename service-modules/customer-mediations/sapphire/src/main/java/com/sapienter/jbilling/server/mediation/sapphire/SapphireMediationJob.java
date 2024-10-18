package com.sapienter.jbilling.server.mediation.sapphire;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CDR_CONVERTOR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CDR_RESOLVER;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.FORWARDED_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.INCOMING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.JMR_DEFAULT_WRITER_BEAN;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.JOB_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ON_NET_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OUT_GOING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.RECYCLE_JOB_NAME;

import java.util.Arrays;
import java.util.List;

import com.sapienter.jbilling.server.mediation.converter.MediationJobs;
import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

@SuppressWarnings("serial")
public class SapphireMediationJob extends DefaultMediationJob {

    private SapphireMediationJob(String job, String lineConverter,
            String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver, writer, recycleJob);
    }

    public static void init() {
        MediationJobs.addJob(getMediationJobInstance());
        MediationJobs.addJob(getRecycleJobInstance());
    }

    public static MediationJob getMediationJobInstance() {
        return new SapphireMediationJob(JOB_NAME, CDR_CONVERTOR, CDR_RESOLVER, JMR_DEFAULT_WRITER_BEAN, RECYCLE_JOB_NAME);
    }

    public static MediationJob getRecycleJobInstance() {
        return new SapphireMediationJob(RECYCLE_JOB_NAME, CDR_CONVERTOR, CDR_RESOLVER, JMR_DEFAULT_WRITER_BEAN, null);
    }

    @Override
    public boolean needsInputDirectory() {
        return true;
    }

    @Override
    public MediationJobParameterValidator getParameterValidator() {
        return Context.getBean("jobParameterValidator");
    }

    @Override
    public List<String> getCdrTypes() {
        return Arrays.asList(INCOMING_CALL_CDR_TYPE, ON_NET_CALL_CDR_TYPE, OUT_GOING_CALL_CDR_TYPE, FORWARDED_CALL_CDR_TYPE);
    }
}
