package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

public class SpcMediationMetaDataCleaningTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer DELETE_BEFORE_NUMBER_OF_DAYS = 90;

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            logger.debug("executing {}", getTaskName());
            _init(context);
            SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
            spcHelperService.deleteMediationJobMetaDataAndMurMediationRecords(getEntityId(), DELETE_BEFORE_NUMBER_OF_DAYS);
        } catch(Exception ex) {
            logger.error("mediation meta data cleaning task failed for entity {}", getEntityId(), ex);
        }
    }

}
