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
package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.IPluggableTaskSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IFileExchangeTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.quartz.JobExecutionException;

/**
 * Created by marcomanzi on 3/3/14.
 */
public class FileExchangeBL {

    public void filesDownload(int entityId) {
        launchTasksInPlugin(entityId, true);
    }

    public void filesUpload(int entityId) {
        launchTasksInPlugin(entityId, false);
    }

    public void filesExchange(int entityId) {
        launchTasksInPlugin(entityId, true);
        launchTasksInPlugin(entityId, false);
    }

    private void launchTasksInPlugin(int entityId, boolean downloadOnly) {
        try {
            PluggableTaskManager<IFileExchangeTask> taskManager
                    = new PluggableTaskManager<IFileExchangeTask>(entityId, Constants.PLUGGABLE_TASK_FILE_EXCHANGE);
            IFileExchangeTask task = taskManager.getNextClass();
            while (task != null) {
                if (downloadOnly && task.isDownloadTask() ||
                        !downloadOnly && !task.isDownloadTask()) {
                    task.execute();
                }
                task = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("File Exchange: exception while running filesDownload.", e);
        }
    }

    public void executeFileExchangeServer(Integer pluginId) throws JobExecutionException{
        try {
            IFileExchangeTask fileExchangeTask = instantiateTask(pluginId);
            if (null != fileExchangeTask) {
                fileExchangeTask.execute();
            } else {
                throw new JobExecutionException("Can Not Create a Plugin with Given ID:" + pluginId);
            }
        } catch (NumberFormatException nfe) {
            throw new JobExecutionException(nfe);
        } catch (PluggableTaskException pte) {
            throw new JobExecutionException(pte);
        }
    }

    private IFileExchangeTask instantiateTask(Integer pluginId)
            throws PluggableTaskException {
        PluggableTaskBL<IFileExchangeTask> taskLoader =
                new PluggableTaskBL<IFileExchangeTask>(pluginId);
        IPluggableTaskSessionBean pluginSessionBean = getPluginSessionBean();
        PluggableTaskDTO dto = pluginSessionBean.getDTO(pluginId);
        if (null == dto) return null;
        taskLoader.set(dto);
        return taskLoader.instantiateTask();
    }

    private IPluggableTaskSessionBean getPluginSessionBean() {
        return (IPluggableTaskSessionBean) Context.
                getBean(Context.Name.PLUGGABLE_TASK_SESSION);
    }

}
