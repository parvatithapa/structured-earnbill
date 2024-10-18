package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCSkipPolicy implements SkipPolicy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
        logger.error("Skipping processing of Record count {}, exception:{}", skipCount, t);
        return true;
    }
}
