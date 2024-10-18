package com.sapienter.jbilling.server.mediation.task;

public interface MediationPartitioningPluginHelperService {

    IMediationPartitionTask getPartitioningTasksForEntity(Integer entityId);
}
