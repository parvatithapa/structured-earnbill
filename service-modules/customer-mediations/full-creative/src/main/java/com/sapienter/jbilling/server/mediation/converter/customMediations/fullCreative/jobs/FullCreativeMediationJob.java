package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs;

import java.util.Arrays;
import java.util.List;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;

@SuppressWarnings("serial")
public class FullCreativeMediationJob extends DefaultMediationJob {

    protected FullCreativeMediationJob(String job, String lineConverter,
            String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver, writer, recycleJob);
    }

    public static MediationJob getMediationJobInstance() {
        return new FullCreativeMediationJob(FullCreativeConstants.FC_MEDIATION_CONFIGURATION,
                                           FullCreativeConstants.FC_MEDIATION_CONVERTER_BEAN,
                                           FullCreativeConstants.FC_MEDIATION_CDR_RESOLVER_BEAN,
                                           FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN,
                                           FullCreativeConstants.FC_RECYCLE_CONFIGURATION);
    }

    public static MediationJob getRecycleJobInstance() {
        return new FullCreativeMediationJob(FullCreativeConstants.FC_RECYCLE_CONFIGURATION,
                                           FullCreativeConstants.FC_MEDIATION_CONVERTER_BEAN,
                                           FullCreativeConstants.FC_MEDIATION_CDR_RESOLVER_BEAN,
                                           FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN,
                                           null);
    }

    @Override
    public String getOrderServiceBeanName() {
        return "fcOrderService";
    }

    @Override
    public List<String> getCdrTypes() {
        return Arrays.asList("AR",
                "IVR (Inbound)",
                "Spanish (Inbound)",
                "Call Relay (Inbound)",
                "Webform to Call (IVR)",
                "Chat",
                "Supervisor (Inbound)",
                "Voicemail",
                "Mobile App (Outbound)",
                "Inbound",
                "Live Reception");
    }
}
