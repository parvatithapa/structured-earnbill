package com.sapienter.jbilling.server.notification;

import java.util.Date;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.task.IScheduledTask;

/**
 * Created by marcolin on 10/06/16.
 */
public interface IPluginsSessionBean {

    Integer createPlugin(Integer executorUserId, Integer companyId, PluggableTaskWS plugin);
    void updatePlugin(Integer executorUserId, Integer companyId, PluggableTaskWS plugin);
    void deletePlugin(Integer executorUserId, Integer companyId, Integer pluginId);
    IScheduledTask getScheduledTask(Integer pluginId, Integer companyId);
    void rescheduleScheduledPlugin(Integer executorId, Integer companyId, Integer pluginId );
    void unscheduleScheduledPlugin(Integer executorId, Integer companyId, Integer pluginId);
    void triggerScheduledTask(Integer callerId, Integer callerCompanyId, Integer pluginId, Date executionDate);

}
