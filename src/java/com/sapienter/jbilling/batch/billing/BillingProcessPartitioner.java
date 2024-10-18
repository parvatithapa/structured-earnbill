package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
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
public class BillingProcessPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobExecutionContext['restart'] == 1L}")
    private boolean restart;

    @Override
    public Map<String, ExecutionContext> partition (int gridSize) {

        int totalUsers = jdbcService.countInputUsers(billingProcessId);
        int size = totalUsers - 1;
        int targetSize = size / gridSize + 1;
        logger.debug("Billing process ID: {}, Target size for each partition: {}", billingProcessId, targetSize);

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

            if (! restart) {
                int minValue = jdbcService.getUserAtOffset(billingProcessId, start);
                int maxValue = jdbcService.getUserAtOffset(billingProcessId, end);
                int totalUsersInPartition = jdbcService.assignPartitionNumber(number, billingProcessId, minValue, maxValue);
                logger.debug(
                        "Billing process ID: {}, part#: {}, min user ID: {}, max user ID: {}, total users in partition: {}",
                        billingProcessId, number, minValue, maxValue, totalUsersInPartition);
            }

            value.putInt("partition", number);
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }
}
