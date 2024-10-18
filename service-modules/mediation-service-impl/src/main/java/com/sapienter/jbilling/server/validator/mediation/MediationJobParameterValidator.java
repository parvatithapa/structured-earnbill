package com.sapienter.jbilling.server.validator.mediation;

import org.springframework.batch.core.JobParameters;

public interface MediationJobParameterValidator {
    void validate(JobParameters parameters);
}
