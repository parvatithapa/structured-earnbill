package com.sapienter.jbilling.batch.ageing;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 * @author Khobab
 *
 */
public class AgeingProcessPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private CollectionBatchService jdbcService;

    @Value("#{jobParameters['ageingDate']}")
    private Date ageingDate;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public Map<String, ExecutionContext> partition (int gridSize) {
        int totalUsers = jdbcService.countInputUsers(entityId);
        int size = totalUsers - 1;
        int targetSize = size / gridSize + 1;
        logger.debug("Collections process for company ID: {}, Target size for each partition: {}", entityId, targetSize);

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
            int minValue = jdbcService.getUserAtOffset(entityId, start);
            int maxValue = jdbcService.getUserAtOffset(entityId, end);
            int totalUsersInPartition = jdbcService.assignPartitionNumber(number, entityId, minValue, maxValue);
            logger.debug(
                    "Collections process for company ID: {}, part#: {}, min user ID: {}, max user ID: {}, total users in partition: {}",
                    entityId, number, minValue, maxValue, totalUsersInPartition);

            value.putInt("partition", number);
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }
}
