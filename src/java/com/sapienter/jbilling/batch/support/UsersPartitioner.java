package com.sapienter.jbilling.batch.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

/**
 * @author igor poteryaev <igor.poteryaev@appdirect.com>
 *
 */
public class UsersPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private PartitionService partitionService;

    @Value("#{jobExecution.jobId}")
    private Long jobId;

    @Override
    public Map<String, ExecutionContext> partition (int gridSize) {

        int totalUsers = partitionService.countJobInputUsers(jobId);
        int size = totalUsers - 1;
        int targetSize = size / gridSize + 1;

        logger.debug("Job ID: {}, Target size for each partition: {}", jobId, targetSize);

        Map<String, ExecutionContext> result = new HashMap<>();
        int number = 0;
        int start = 0;
        int end = start + targetSize - 1;

        while (start <= size) {
            ExecutionContext value = new ExecutionContext();
            result.put("partition" + number, value);

            if (end >= size) {
                end = size;
            }

            int minId = partitionService.getUserAtOffset(jobId, start);
            int maxId = partitionService.getUserAtOffset(jobId, end);

            int totalUsersInPartition = partitionService.assignPartitionNumber(number, jobId, minId, maxId);

            logger.debug("Job ID: {}, part#: {}, min user ID: {}, max user ID: {}, total users in partition: {}", jobId,
                    number, minId, maxId, totalUsersInPartition);

            value.putInt("partition", number);
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }
}
