package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants;
import com.sapienter.jbilling.server.util.IJobExecutionSessionBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileFilter;
import java.lang.invoke.MethodHandles;

public class CdrFileCountStepListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String folder;

    @Autowired
    IJobExecutionSessionBean jobExecutionService;

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        File file = new File(folder);
        int filesDownloaded = file.exists() ? file.listFiles(new FilesFilter()).length : 0;
        logger.info("Number files for CDR are : {}", filesDownloaded);
        jobExecutionService
                .updateLine(stepExecution.getJobExecution().getId(), IJobExecutionSessionBean.LINE_TYPE_HEADER, DtConstants.EXEC_STAT_AGG_ZIP_PKG_FILES, Integer.toString(filesDownloaded));
        return stepExecution.getExitStatus();
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    private class FilesFilter implements java.io.FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile();
        }
    }
}
