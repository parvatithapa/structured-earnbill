package com.sapienter.jbilling.server.mediation.customMediations.movius;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class MoviusSkipPolicy implements SkipPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(MoviusSkipPolicy.class);

    @Override
    public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
        LOG.error("Skipping processing of Record count {}, exception:{}", skipCount,t);
        return true;
    }
}
