package com.sapienter.jbilling.server.mediation.task;

import java.util.List;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public interface IMediationPartitionTask {
    /**
     * Actual partitioning logic place in this method
     * @param batchSize
     * @param userIds
     * @return
     */
    List<List<Integer>> doPartition(Integer batchSize, List<Integer> userIds);
}
