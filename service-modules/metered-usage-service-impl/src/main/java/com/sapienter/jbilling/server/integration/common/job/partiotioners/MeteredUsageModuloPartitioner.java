package com.sapienter.jbilling.server.integration.common.job.partiotioners;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import com.sapienter.jbilling.server.integration.Constants;

public class MeteredUsageModuloPartitioner implements Partitioner {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> contextMap = new HashMap<>(gridSize*2);

        for(int idx = 1; idx <= gridSize; idx++) {
            createContextForUsers(gridSize, contextMap, idx);
        }
        logger.debug("gridSize={}", gridSize);
        return contextMap;
    }

    private void createContextForUsers(int gridSize, Map<String, ExecutionContext> contextMap, int processorIdx) {
        Map<String, Object> parameters = new HashMap<>(4);
        parameters.put(Constants.PARM_CURRENT_PARTITION, processorIdx);
        parameters.put(Constants.PARM_NUMBER_OF_PARTITIONS, gridSize);

        logger.debug("Created partition {} with parameters {}", processorIdx, parameters);

        ExecutionContext ctx = new ExecutionContext(parameters);
        contextMap.put("JMR:p"+processorIdx, ctx);
    }
}
