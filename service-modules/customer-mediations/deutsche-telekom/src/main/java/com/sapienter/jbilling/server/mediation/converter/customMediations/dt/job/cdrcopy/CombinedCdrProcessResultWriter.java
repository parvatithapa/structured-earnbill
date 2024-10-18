package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtConstants;
import com.sapienter.jbilling.server.util.IJobExecutionSessionBean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 */
public class CombinedCdrProcessResultWriter implements ItemWriter<CombinedCdrProcessResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String backupFolder;
    private boolean backup;
    private boolean deleteError;
    private boolean deleteSuccess = false;
    private long jobExecutionId;

    @Autowired
    IJobExecutionSessionBean jobExecutionService;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        jobExecutionId = stepExecution.getJobExecution().getId();

        if(!StringUtils.isBlank(backupFolder)) {
            File folder = new File(backupFolder);
            if(!folder.exists()) {
                folder.mkdir();
            }
            backup = true;
        }
        logger.info("Job Execution backup for Job ID {}", jobExecutionId);
    }

    @Override
    public void write(List<? extends CombinedCdrProcessResult> items) throws Exception {
        for(CombinedCdrProcessResult result : items) {
            if(!result.isValid()) {
                for(String error : result.getErrors()) {
                    jobExecutionService.addLine(jobExecutionId, DtConstants.EXEC_STAT_LINE, result.getFile().getName(), error);
                }
            }
            boolean fileDeleted = false;

            if(deleteError && !result.isValid()) {
                result.getFile().delete();
                fileDeleted = true;
            } else if(deleteSuccess && result.isValid()) {
                fileDeleted = true;
                result.getFile().delete();
            }

            if(!fileDeleted && backup) {
                File backupFile = new File(backupFolder, result.getFile().getName());
                if(backupFile.exists()) {
                    backupFile.delete();
                }
                result.getFile().renameTo(backupFile);
            }
        }
    }

    public String getBackupFolder() {
        return backupFolder;
    }

    public void setBackupFolder(String backupFolder) {
        this.backupFolder = backupFolder;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public boolean isDeleteError() {
        return deleteError;
    }

    public void setDeleteError(boolean deleteError) {
        this.deleteError = deleteError;
    }

    public void setDeleteSuccess(boolean deleteSuccess) {
        this.deleteSuccess = deleteSuccess;
    }
}
