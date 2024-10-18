package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.jmrProcess.partitioner;

import com.sapienter.jbilling.server.mediation.processor.JmrProcessorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;


public class DtJMRModuloPartitioner implements Partitioner, JmrProcessorConstants {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        if(gridSize < 1) {
            gridSize = 100;
        }
        final int nrPartitions = gridSize;

        Map<String, ExecutionContext> contextMap = new HashMap<>(nrPartitions*2);

        for(int idx = 0; idx < nrPartitions; idx++) {
            createContextForUsers(nrPartitions, contextMap, idx);
        }

        logger.debug("gridSize={}", nrPartitions);
        return contextMap;
    }

    private void createContextForUsers(int gridSize, Map<String, ExecutionContext> contextMap, int processorIdx) {
        Map<String, Object> parameters = new HashMap<>(4);
        parameters.put(PARM_CURRENT_PARTITION, processorIdx);
        parameters.put(PARM_NUMBER_OF_PARTITIONS, gridSize);

        logger.debug("Created partition {} with parameters {}", processorIdx, parameters);

        ExecutionContext ctx = new ExecutionContext(parameters);
        contextMap.put("JMR:p"+processorIdx, ctx);
    }
}
