package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;

public class ResourceCleaner extends StepExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
        if(null!= jobExecutionContext) {
            String partitionedDirectoryPath = jobExecutionContext.getString(SPCFileSplitter.PARTITIONED_DIRECTORY_PARAM_KEY);
            if(StringUtils.isNotEmpty(partitionedDirectoryPath)) {
                try {
                    FileUtils.deleteDirectory(new File(partitionedDirectoryPath));
                    logger.debug("directory {} deleted", partitionedDirectoryPath);
                } catch (IOException ex) {
                    logger.error("file deletion failed!", ex);
                }
            }
        }
        return stepExecution.getExitStatus();
    }
}
