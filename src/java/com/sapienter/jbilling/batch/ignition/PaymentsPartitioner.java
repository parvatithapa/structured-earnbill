package com.sapienter.jbilling.batch.ignition;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

public class PaymentsPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IgnitionBatchService jdbcService;

    @Value("#{jobExecution.jobId}")
    private Long jobId;

    @Override
    public Map<String, ExecutionContext> partition (int gridSize) {
        int totalPayments = jdbcService.countJobInputPayments(jobId);
        int size = totalPayments - 1;
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

            int minId = jdbcService.getPaymentAtOffset(jobId, start);
            int maxId = jdbcService.getPaymentAtOffset(jobId, end);

            int totalPaymentsInPartition = jdbcService.assignPartitionNumber(number, jobId, minId, maxId);

            logger.debug(
                    "Job ID: {}, part#: {}, min payment ID: {}, max payment ID: {}, total payments in partition: {}",
                    jobId, number, minId, maxId, totalPaymentsInPartition);

            value.putInt("partition", number);
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }
}
