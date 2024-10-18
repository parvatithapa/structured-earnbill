package com.sapienter.jbilling.server.validator.mediation;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.util.Assert;

public class DefaultMediationJobParameterValidatorImpl implements MediationJobParameterValidator {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String VALIDATION_MESSAGE = "Please Provide Parameters!";

    @Override
    public void validate(JobParameters parameters) {
        try {
            logger.debug("Received Parameters {}", parameters);
            Assert.notNull(parameters, VALIDATION_MESSAGE);
            Assert.isTrue(!parameters.isEmpty(), VALIDATION_MESSAGE);
        } catch(Exception ex) {
            logger.error("parameters validation failed!", ex);
            throw new InvalidJobParameterException(ex.getMessage(), ex);
        }
    }

}
