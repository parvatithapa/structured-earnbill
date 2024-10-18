package com.sapienter.jbilling.server.mediation.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.util.Constants;

public class MediationPartitioningPluginHelperServiceImpl implements MediationPartitioningPluginHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MediationPartitioningPluginHelperServiceImpl.class);
    private static final IMediationPartitionTask DEFAULT_PARTITIONING_TASK = new DefaultUserPartitioningTask();
    
    @Override
    public IMediationPartitionTask getPartitioningTasksForEntity(Integer entityId) {
        try {
            PluggableTaskManager<IMediationPartitionTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_MEDIATION_USER_PARTITIONING);
            IMediationPartitionTask task = taskManager.getNextClass();
            return  null!= task ? task : DEFAULT_PARTITIONING_TASK;
        } catch (PluggableTaskException e) {
            // eat it
            logger.error("Exception Occurs ", e);
            return DEFAULT_PARTITIONING_TASK;
        }
    }

}
