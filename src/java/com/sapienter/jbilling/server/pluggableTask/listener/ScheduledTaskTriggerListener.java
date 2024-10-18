package com.sapienter.jbilling.server.pluggableTask.listener;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.scheduledTask.event.ScheduledJobNotificationEvent;
import com.sapienter.jbilling.server.system.event.EventManager;

/**
 * Listener to intercept events for quartz jobs and trigger Notification event
 *
 * @author Aadil Nazir
 * @since 04/26/18.
 */
@Transactional(propagation = Propagation.REQUIRED)
public class ScheduledTaskTriggerListener implements TriggerListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getName() {
        return "ScheduledTaskTriggerListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
        try {
            logger.debug("ScheduledTaskTriggerListener : triggerFired");
            Integer entityId = (Integer) jobExecutionContext.getMergedJobDataMap().get("entityId");
            String jobName = jobExecutionContext.getTrigger().getKey().getName();

            if(excludeTaskNotification(jobName)) {
                logger.debug("job name : {} matches name to exclude from properties file, so exiting", jobName);
                return;
            }

            logger.debug("entityId {}", entityId);
            EventManager.process(new ScheduledJobNotificationEvent(entityId,
                    jobName,
                    jobExecutionContext, ScheduledJobNotificationEvent.TaskEventType.TRIGGER_FIRED));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on trigger");
        }
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext jobExecutionContext) {
        logger.debug("ScheduledTaskTriggerListener : vetoJobExecution");
        // for testing purpose, it's set to true, so that plugin does not execute.
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        logger.debug("ScheduledTaskTriggerListener : triggerMisfired");
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
        try {
            logger.debug("ScheduledTaskTriggerListener : triggerComplete");
            Integer entityId = (Integer) jobExecutionContext.getMergedJobDataMap().get("entityId");
            String jobName = jobExecutionContext.getTrigger().getKey().getName();

            if(excludeTaskNotification(jobName)) {
                logger.debug("job name : {} matches name to exclude from properties file, so exiting", jobName);
                return;
            }

            EventManager.process(new ScheduledJobNotificationEvent(entityId, jobName, jobExecutionContext,
                    ScheduledJobNotificationEvent.TaskEventType.TRIGGER_COMPLETED));

        } catch (Exception exception) {
            logger.warn("Cannot send notification on trigger");
        }
    }

    private boolean excludeTaskNotification (String jobName) {
        String excludeTasksFromNotification = Util.getSysProp("exclude.tasks.from.notification");
        if (null != excludeTasksFromNotification && !excludeTasksFromNotification.trim().isEmpty()) {
            for (String taskName : excludeTasksFromNotification.split(",")){
                logger.debug("checking job name : {} match against name : {} properties file", jobName, taskName);
                if(StringUtils.containsIgnoreCase(jobName, taskName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
