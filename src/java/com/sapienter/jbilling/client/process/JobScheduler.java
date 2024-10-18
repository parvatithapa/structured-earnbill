/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.client.process;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.pluggableTask.listener.ScheduledTaskTriggerListener;
import com.sapienter.jbilling.server.pluggableTask.listener.SchedulerHistoryLogger;

import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Context;

/**
 * Spring bean to provide easy access to the Quartz Scheduler. Used to schedule
 * all of jBilling's batch processes and {@link com.sapienter.jbilling.server.process.task.IScheduledTask}
 * plug-ins.
 *
 * @author Brian Cowdery
 * @since 02-02-2010
 */
public class JobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Scheduler scheduler;
    private Map<String, String> dbParameters;

    @Resource(name = "scheduledTaskTriggerListener")
    private TriggerListener scheduledTaskTriggerListener;

    public void setDbParameters (Map<String, String> dbParameters) {
        this.dbParameters = dbParameters;
    }

    /*
     * Setup scheduler using quartz configuration in the next priority order:
     * 1. system property
     * 2. failback: use generated properties
     *
     * Hint: To set configuration location via environment variable use JAVA_OPTS.
     * Example: JAVA_OPTS="${JAVA_OPTS} -Dorg.quartz.properties=/path/to/quartz.properties"
     *
     * N.B. quartz.properties from current working directory or classpath IS NOT USED, use system property instead.
     */
    @PostConstruct
    public void initialize () throws SchedulerException {

        if (System.getProperty(StdSchedulerFactory.PROPERTIES_FILE) != null) {
            scheduler = new StdSchedulerFactory().getScheduler();
        } else {
            scheduler = new StdSchedulerFactory(generateProperties()).getScheduler();
        }
        Assert.notNull(scheduledTaskTriggerListener, "scheduledTaskTriggerListener is null!");
        scheduler.getListenerManager().addTriggerListener(scheduledTaskTriggerListener);
    }

    private Properties generateProperties () {

        Properties props = new Properties();

        // Set custom properties
        props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "jbilling-jobs");
        props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
        props.setProperty("org.quartz.threadPool.threadCount", "2");

        props.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.setProperty("org.quartz.jobStore.dataSource", "quartzDS");
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");

        // Set db connection properties
        // N.B. Quartz datasource name is fixed, no use cases for multiple names

        for (Entry<String, String> paramEntry: dbParameters.entrySet()) {
            props.setProperty(StdSchedulerFactory.PROP_DATASOURCE_PREFIX + ".quartzDS." + paramEntry.getKey(), paramEntry.getValue());
        }

        return props;
    }

    /**
     * Start the quartz scheduler and scheduled all the scheduled tasks
     *
     * @author Tarun Rathor
     *
     * Fails fast by throwing exceptions
     * @throws SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        schedulePluggableTasks();
        scheduler.start();
    }

    /**
     * Execute the job with specified jobKey on the executionDateParamValue
     *
     * @param jobKey Identifies the job
     * @param executionDateParamName execution date parameter name to set in the context
     * @param executionDateParamValue execution date value to be set in the context
     */
    public void executeJobOn (JobKey jobKey, String executionDateParamName, Date executionDateParamValue) throws SchedulerException {
        scheduler.getContext().put(executionDateParamName, executionDateParamValue);
        scheduler.triggerJob(jobKey);
    }

    /**
     * Shutdown the quartz scheduler
     */
    @PreDestroy
    public void shutdown() {

        try {
            if(scheduler.isStarted()) {
                logger.debug("Shutdown the quartz scheduler");
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            // swallow
            logger.error("Exception occurred shutting down the scheduler.", e);
        }
    }

    /**
     * Reschedule a jBilling IScheduledTask after it has been saved.
     *
     * @param task name of the task to reschedule
     */
    public void rescheduleJob (IScheduledTask task) throws SchedulerException, PluggableTaskException {
        if (null == task) {
            return;
        }
        logger.debug("Rescheduling instance of: {}", task.getClass().getName());
        logger.debug("Task Name: {}", task.getTaskName());

        boolean found = unScheduleExisting(task.getTaskName());
        // schedule new plugin if not found, no need to restart jbilling then
        if (!found) {
            logger.debug("This is a new scheduled task. {}", task.getTaskName());
        }

        logger.debug("Scheduling {}", task.getTaskName());
        scheduler.scheduleJob(task.getJobDetail(), task.getTrigger());
    }

    /**
     * Unschedule a jBilling task after it has been saved.
     *
     * @param taskName name of the task to unschedule
     *
     * @author Leandro Zoi
     */

    public boolean unScheduleExisting (String taskName) throws SchedulerException {
        logger.debug("Unscheduling instance of: {}", taskName);
        if (null == taskName) {
            return false;
        }
        boolean found = false;
        for (String stTriggerGrp : scheduler.getTriggerGroupNames()) {
            logger.debug("Trigger Group Name: {}", stTriggerGrp);

            for (TriggerKey keyTrigger : scheduler.getTriggerKeys(GroupMatcher.<TriggerKey> groupEquals(stTriggerGrp))) {
                logger.debug("Trigger Name : {}", keyTrigger.getName());

                if (keyTrigger.getName().equals(taskName)) {
                    found = true;
                    logger.debug("unscheduling {}", keyTrigger.getName());
                    scheduler.unscheduleJob(keyTrigger);
                }
            }
        }
        return found;
    }

    private PluggableTaskManager<IScheduledTask> getPluginManagerForEntity(Integer entityId) {
        try {
            return new PluggableTaskManager<>(entityId,
                    com.sapienter.jbilling.server.util.Constants.PLUGGABLE_TASK_SCHEDULED);
        } catch (PluggableTaskException e) {
            logger.warn("Skipping ScheduledTask for entity {} because of error {}", entityId, e.getLocalizedMessage());
            return null;
        }
    }

    private void schedulePluggableTasks () {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        txAction.execute(()->
        new CompanyDAS().findEntities()
        .stream()
        .map(CompanyDTO::getId)
        .forEach(entityId -> {
            PluggableTaskManager<IScheduledTask> manager = getPluginManagerForEntity(entityId);
            if(null!= manager) {
                logger.debug("Processing {} scheduled tasks for entity {}", manager.getAllTasks().size(), entityId);
                boolean done = false;
                while (!done) {
                    try {
                        IScheduledTask task = manager.getNextClass();
                        if (task == null) {
                            done = true;
                            continue;
                        }
                        scheduler.scheduleJob(task.getJobDetail(), task.getTrigger());
                        logger.debug("Scheduled: [{}]", task.getTaskName());
                    } catch (ObjectAlreadyExistsException e) {
                        logger.debug("Pluggable task already exists [{}]", e.getMessage());
                    } catch (PluggableTaskException | SchedulerException e) {
                        logger.error("Failed to schedule pluggable task", e);
                    }
                }
            }
        }));
    }
}
