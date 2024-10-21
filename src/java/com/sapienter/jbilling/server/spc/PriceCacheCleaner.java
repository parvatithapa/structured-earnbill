package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

public class PriceCacheCleaner extends StepExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().getClass());

    @Resource
    private SpcHelperService spcHelperService;

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            // invalidate price cache.
            spcHelperService.invalidatePriceCache();
            logger.debug("price cache cleared");
        } catch(Exception ex) {
            logger.error("error during clearing price cache", ex);
        }
        return stepExecution.getExitStatus();
    }

}
