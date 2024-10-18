package com.sapienter.jbilling.server.mediation.custommediation.spc.mur;

import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

@SuppressWarnings("serial")
class OptusMurMediationJob extends DefaultMediationJob {

    protected OptusMurMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver, writer, recycleJob);
    }


    @Override
    public MediationJobParameterValidator getParameterValidator() {
        return Context.getBean("defaultValidator");
    }

}
