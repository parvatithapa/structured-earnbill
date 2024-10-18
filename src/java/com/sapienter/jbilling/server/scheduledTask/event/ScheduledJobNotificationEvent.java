package com.sapienter.jbilling.server.scheduledTask.event;

import com.sapienter.jbilling.server.system.event.Event;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.JobExecution;

/**
 * Notification event triggered for scheduled and batch jobs
 *
 * @author Aadil Nazir
 * @since 04/26/18
 */
public class ScheduledJobNotificationEvent implements Event {

    public enum TaskEventType {
        TRIGGER_FIRED, TRIGGER_MISFIRED, TRIGGER_COMPLETED, BEFORE_JOB, AFTER_JOB
    }

    private final Integer entityId;
    private final String jobName;
    private final JobExecutionContext jobExecutionContext;
    private final JobExecution jobExecution;
    private final TaskEventType taskEventType;


    public ScheduledJobNotificationEvent(Integer entityId, String jobName, JobExecutionContext jobExecutionContext,
                                         TaskEventType eventType) {

        this.entityId = entityId;
        this.jobName = jobName;
        this.jobExecutionContext = jobExecutionContext;
        this.jobExecution = null;
        this.taskEventType = eventType;
    }

    public ScheduledJobNotificationEvent(Integer entityId, String jobName, JobExecution jobExecution,
                                         TaskEventType eventType) {

        this.entityId = entityId;
        this.jobName = jobName;
        this.jobExecution = jobExecution;
        this.jobExecutionContext = null;
        this.taskEventType = eventType;
    }

    @Override
    public String getName() {
        return "Schedule Job Notification Event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public JobExecutionContext getJobExecutionContext() {
        return jobExecutionContext;
    }

    public JobExecution getJobExecution() {
        return jobExecution;
    }

    public TaskEventType getTaskEventType() {
        return taskEventType;
    }

    public String getJobName() {
        return jobName;
    }
}
