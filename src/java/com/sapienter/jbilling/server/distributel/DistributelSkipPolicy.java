package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.beans.factory.annotation.Value;

public class DistributelSkipPolicy implements SkipPolicy {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{stepExecution.stepName}")
    private String stepName;

    @Override
    public boolean shouldSkip(Throwable ex, int skipCount) {
        logger.error("Skipped from step {} beacuse of ", stepName, ex);
        return true;
    }

}
