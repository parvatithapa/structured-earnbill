/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

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
 * @author Igor Poteryaev
 */
public class EmailAndPaymentPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private BillingBatchService jdbcService;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;

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
            result.put("email-partition" + number, value);

            if (end >= size) {
                end = size;
            }
            value.putInt("partition", number);
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }
}
