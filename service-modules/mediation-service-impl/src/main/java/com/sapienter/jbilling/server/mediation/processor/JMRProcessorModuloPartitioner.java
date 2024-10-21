package com.sapienter.jbilling.server.mediation.processor;

import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.task.IMediationPartitionTask;
import com.sapienter.jbilling.server.mediation.task.MediationPartitioningPluginHelperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Creates execution context for remote processors.
 *
 * @author Gerhard Maree
 * @since 29-07-2015
 */
public class JMRProcessorModuloPartitioner implements Partitioner, JmrProcessorConstants {

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private MediationPartitioningPluginHelperService mediationPartitioningPluginHelperService;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobExecutionContext['mediationProcessId']}")
    private UUID mediationProcessId;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> contextMap = new HashMap<>(gridSize*2);

        List<Integer> userIds = jmrRepository.findUsersByStatus("UNPROCESSED", mediationProcessId)
                .stream()
                .distinct()
                .collect(Collectors.toList());

        //TODO Should retrieve jmr records by process id count...
        long jmrQuantity = userIds.size();
        int numberOfUsersForProcessor = userIds.size() / gridSize + 1;

        IMediationPartitionTask partitionTask = mediationPartitioningPluginHelperService.getPartitioningTasksForEntity(entityId);
        List<List<Integer>> partitionedIds = partitionTask.doPartition(numberOfUsersForProcessor, userIds);
        int processorIdx = 0;
        for(String usersCommaSeparatedString: partitionedIds.stream()
                .map( users -> users.stream().map(String::valueOf).collect(Collectors.joining(","))).collect(Collectors.toList())) {
            createContextForUsers(gridSize, contextMap, processorIdx, usersCommaSeparatedString);
            processorIdx ++;
        }
        logger.debug("jmrQuantity={}, gridSize={}", jmrQuantity, gridSize);
        return contextMap;
    }

    private void createContextForUsers(int gridSize, Map<String, ExecutionContext> contextMap, int processorIdx, String usersCommaSeparatedString) {
        Map<String, Object> parameters = new HashMap<>(4);
        parameters.put(PARM_PROCESSOR_IDX, processorIdx);
        parameters.put(PARM_GRID_SIZE, gridSize);
        parameters.put(PARM_USER_LIST, usersCommaSeparatedString);

        logger.debug("Created partition {} with parameters {}", processorIdx, parameters);

        ExecutionContext ctx = new ExecutionContext(parameters);
        contextMap.put("DefaultPartitionedJMRProcessorStep:partition"+processorIdx, ctx);
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

}
