package com.sapienter.jbilling.server.batch.mediation;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class DefaultSkipPolicy implements SkipPolicy {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean shouldSkip(Throwable error, int count) {
        logger.error("Skipping processing of Record count {}, exception:{}", count, error);
        return true;
    }

}
