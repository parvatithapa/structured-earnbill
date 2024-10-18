package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates execution context for remote processors.
 *
 * @author Gerhard Maree
 * @since 29-07-2015
 */
public class CdrProcessorPartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String folder;
    int processorIdx = 0;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        if(gridSize < 1) {
            gridSize = 100;
        }
        final int nrPartitions = gridSize;

        Map<String, ExecutionContext> contextMap = new HashMap<>(nrPartitions*2);

        File srcFolder = new File(folder);
        processorIdx=0;

        processFolder(srcFolder, (File f) ->
                createContext(f, processorIdx++, contextMap, nrPartitions)
        );

        logger.debug("processorIdx={}, gridSize={}", processorIdx, nrPartitions);
        return contextMap;
    }

    private void createContext(File file, int processorIdx, Map<String, ExecutionContext> contextMap, int nrPartitions) {
        String key = "I:"+(processorIdx % nrPartitions);
        ExecutionContext ctx = contextMap.get(key);

        if(ctx == null) {
            Map<String, Object> parameters = new HashMap<>(2);
            ctx = new ExecutionContext(parameters);
            contextMap.put(key, ctx);
        }

        ctx.putString(DtConstants.PARM_FILE + '.' + processorIdx, file.getName());

        logger.debug("Added to partition {} with file {}", processorIdx, file);
    }

    private void processFolder(File file, Consumer<File> consumer) {
        if(file.exists()) {
            if(file.isFile()) {
                consumer.accept(file);
            } else if(file.isDirectory()) {
                for(File f : file.listFiles()) {
                    processFolder(f, consumer);
                }
            }
        }
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
