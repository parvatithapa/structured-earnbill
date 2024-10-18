package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.*;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 31/10/14.
 */
public class PluginCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PluginCopyTask.class));

    private IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<PluggableTaskDTO> pluggableTaskDTOs = pluggableTaskDAS.findAllByEntity(targetEntityId);
        return pluggableTaskDTOs != null && !pluggableTaskDTOs.isEmpty();
    }

    PluggableTaskDAS pluggableTaskDAS = null;

    PluginCopyTask() {
        init();
    }

    public void init() {
        pluggableTaskDAS = PluggableTaskDAS.getInstance();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create PluginCopyTask");
        pluggableTaskDAS = (PluggableTaskDAS) Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);

        List<PluggableTaskDTO> pluggableTaskDTOs = pluggableTaskDAS.findAllByEntity(entityId);
        List<PluggableTaskDTO> copyPluggableTaskDTOs = pluggableTaskDAS.findAllByEntity(targetEntityId);
        if (copyPluggableTaskDTOs.isEmpty()) {
            for (PluggableTaskDTO pluggableTaskDTO : pluggableTaskDTOs) {
                pluggableTaskDAS.reattach(pluggableTaskDTO);
                if (pluggableTaskDTO.getType() == null)
                    continue;

                PluggableTaskWS pluggableTaskWS = PluggableTaskBL.getWS(pluggableTaskDTO);
                pluggableTaskWS.setId(0);
                int taskId = pluggableTaskDAS.save(new PluggableTaskDTO(targetEntityId, pluggableTaskWS)).getId();
                //webServicesSessionSpringBean.rescheduleScheduledPlugin(taskId);
            }
        }
        LOG.debug("PluginCopyTask has been completed");
    }

}
