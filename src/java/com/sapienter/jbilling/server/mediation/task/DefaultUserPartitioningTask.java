package com.sapienter.jbilling.server.mediation.task;

import java.util.List;

/**
 * Default Partition Just partition user using given batch Size
 * @author Krunal Bhavsar
 *
 */
public class DefaultUserPartitioningTask extends AbstractUserPartitioningTask {

    @Override
    public List<List<Integer>> doPartition(Integer batchSize, List<Integer> userIds) {
        return choppedList(userIds, batchSize);
    }

}
