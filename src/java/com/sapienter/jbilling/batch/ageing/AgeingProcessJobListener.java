package com.sapienter.jbilling.batch.ageing;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.Resource;

import com.sapienter.jbilling.server.scheduledTask.event.ScheduledJobNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.event.AgeingProcessCompleteEvent;
import com.sapienter.jbilling.server.process.event.AgeingProcessStartEvent;
import com.sapienter.jbilling.server.system.event.EventManager;

public class AgeingProcessJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private CollectionBatchService jdbcService;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobParameters['ageingDate']}")
    private Date ageingDate;

    @Override
    public void afterJob (JobExecution jobExecution) {
        logger.debug("Marking collections process as complete for company id # {}", entityId);
        EventManager.process(new AgeingProcessCompleteEvent(entityId));
        jdbcService.cleanupProcessData(entityId);
        logger.debug("AgeingProcessJobListener : afterJob");
        try {
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "AgeingProcess", jobExecution,
                    ScheduledJobNotificationEvent.TaskEventType.AFTER_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on afterJob for Ageing Process Listener");
        }
    }

    @Override
    public void beforeJob (JobExecution jobExecution) {
        logger.debug("Starting collections process event for company id # {}", entityId);
        EventManager.process(new AgeingProcessStartEvent(entityId));
        logger.debug("Load collections users for company id # {} and collections date # {}", entityId, ageingDate);
        jdbcService.createProcessData(new AgeingBL().getUsersForAgeing(entityId, ageingDate), entityId);
        logger.debug("AgeingProcessJobListener : beforeJob");
        try {
            EventManager.process(new ScheduledJobNotificationEvent(entityId, "AgeingProcess", jobExecution,
                    ScheduledJobNotificationEvent.TaskEventType.BEFORE_JOB));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on beforeJob for Ageing Process Listener");
        }
    }
}
