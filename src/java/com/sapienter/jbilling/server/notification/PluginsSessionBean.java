package com.sapienter.jbilling.server.notification;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.client.process.JobScheduler;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.process.task.ScheduledTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * Created by marcolin on 10/06/16.
 */
@Transactional( propagation = Propagation.REQUIRED )
public class PluginsSessionBean implements IPluginsSessionBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Integer createPlugin(Integer executorUserId, Integer companyId, PluggableTaskWS plugin) {
        Integer pluginId = new PluggableTaskBL<>().create(executorUserId,
                new PluggableTaskDTO(companyId, plugin));
        rescheduleScheduledPlugin(executorUserId, companyId, pluginId);
        return pluginId;
    }

    @Override
    public void updatePlugin(Integer executorUserId, Integer companyId, PluggableTaskWS plugin) {
        new PluggableTaskBL<>().update(executorUserId, new PluggableTaskDTO(
                companyId, plugin));
        rescheduleScheduledPlugin(executorUserId, companyId, plugin.getId());
    }

    @Override
    public void deletePlugin(Integer executorUserId, Integer companyId, Integer pluginId) {
        unscheduleScheduledPlugin(executorUserId, companyId, pluginId);
        new PluggableTaskBL<>(pluginId).delete(executorUserId);
        // invalidate the plug-in cache to clear the deleted plug-in reference
        PluggableTaskDAS pluggableTaskDas = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        pluggableTaskDas.invalidateCache();
    }

    /**
     * This method reschedules an existing scheduled task that got changed. If
     * not existing, the new plugin may need to be scheduled only if it is an
     * instance of {@link com.sapienter.jbilling.server.process.task.IScheduledTask}
     */
    @Override
    public void rescheduleScheduledPlugin(Integer executorUserId, Integer companyId, Integer pluginId) {
        logger.debug("Rescheduling... {} ", pluginId);
        try {
            IScheduledTask scheduledTask = getScheduledTask(pluginId, companyId);

            if (scheduledTask != null) {
                JobScheduler jobScheduler = Context.getBean(Context.Name.JOB_SCHEDULER );
                jobScheduler.rescheduleJob(scheduledTask);
            }
        } catch (SessionInternalError internalError) {
            throw  internalError;
        } catch (Exception e) {
            logger.error("Failed to reschedule",e);
        }

        logger.debug("done.");
    }

   /*
    * Quartz jobs
    */
    /**
     * This method unschedules an existing scheduled task before it is deleted
     */
    @Override
    public void unscheduleScheduledPlugin(Integer executorUserId, Integer companyId, Integer pluginId) {
        logger.debug("Unscheduling... {}" , pluginId);
        try {
            IScheduledTask scheduledTask = getScheduledTask(pluginId, companyId);

            if (scheduledTask != null) {
                String taskName = scheduledTask.getTaskName();
                JobScheduler jobScheduler = Context.getBean(Context.Name.JOB_SCHEDULER );
                jobScheduler.unScheduleExisting(taskName);
            }
        } catch (SessionInternalError internalError) {
            throw  internalError;
        } catch (Exception e) {
            logger.error("failed to unschedule",e);
        }

        logger.debug("done.");
    }

    @Override
    public void triggerScheduledTask(Integer callerId, Integer callerCompanyId, Integer pluginId, Date executionDate) {
        try {
            ScheduledTask task = (ScheduledTask) getScheduledTask(pluginId, callerCompanyId);

            if(task == null){
                logger.error("Scheduled task not found.");
                return;
            }

            task.executeOn(executionDate);

        } catch (SessionInternalError internalError) {
            throw  internalError;
        } catch (Exception e){
            logger.error("failed to trigger",e);
        }
    }

    @Override
    public IScheduledTask getScheduledTask(Integer pluginId, Integer companyId) {
        try {
            PluggableTaskDTO task = getTask(pluginId);
            if (task != null && task.getType() != null && task.getType().getCategory() != null &&
                    Constants.PLUGGABLE_TASK_SCHEDULED.equals(task.getType().getCategory().getId())) {
                PluggableTaskManager<IScheduledTask> manager = new PluggableTaskManager<>(
                        companyId,
                        com.sapienter.jbilling.server.util.Constants.PLUGGABLE_TASK_SCHEDULED);
                IScheduledTask scheduledTask = manager.getInstance(task
                        .getType().getClassName(), task.getType().getCategory()
                        .getInterfaceName(), task);
                logger.debug("{}",task.getParameters());
                return scheduledTask;
            }
        } catch (PluggableTaskException e) {
            logger.error("Failed to get scheduled task", e);
        }
        return null;
    }

    private PluggableTaskDTO getTask(Integer pluginId) {
        try {
            return new PluggableTaskBL<>(pluginId, true).getDTO();
        } catch (ObjectNotFoundException o) {
            logger.error("Plugin not found!", o);
            return null;
        }
    }
}
