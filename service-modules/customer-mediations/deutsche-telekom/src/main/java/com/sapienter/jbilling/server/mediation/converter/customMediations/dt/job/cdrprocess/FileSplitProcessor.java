package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy.CombinedCdrProcessResult;
import org.aspectj.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class FileSplitProcessor implements ItemProcessor<File, CombinedCdrProcessResult> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String outputFolder;
    private File outputFolderFile;
    private String linesPerFile = "2000";

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        outputFolderFile = new File(outputFolder);
        if(!outputFolderFile.exists()) {
            outputFolderFile.mkdir();
        }

    }

    @Override
    public CombinedCdrProcessResult process(File cdrFile) throws Exception {
        logger.debug("Start Processing -> {}", cdrFile);
        CombinedCdrProcessResult result = new CombinedCdrProcessResult(cdrFile);
        splitFile(cdrFile, result);

        logger.debug("End Processing -> {}", cdrFile);
        return result;
    }

    private void splitFile(File cdrFile, CombinedCdrProcessResult result) {
        try {
            if(Util.isUnix()) {
                String[] command = new String[] {
                        "split",
                        "-l",
                        linesPerFile,
                        cdrFile.getAbsolutePath(),
                        cdrFile.getName().replace('.', '_') + '_'
                };
                Util.executeCommand(command, outputFolderFile);
            } else {
                FileUtil.copyFile(cdrFile, new File(outputFolder + File.separator + cdrFile.getName()));
            }
        } catch (Exception e) {
            result.invalid();
            result.addError(CombinedCdrProcessResult.ERROR_NO_HEADER);
            logger.error("Unable to copy/split file {}" , cdrFile.getName(), e);
        }
    }

    public void setLinesPerFile(String linesPerFile) {
        this.linesPerFile = linesPerFile;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

}
