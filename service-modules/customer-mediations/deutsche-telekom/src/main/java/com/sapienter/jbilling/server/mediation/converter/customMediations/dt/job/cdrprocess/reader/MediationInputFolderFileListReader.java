package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.reader;

import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper.MediationHelperService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.NonTransientResourceException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MediationInputFolderFileListReader implements ItemStreamReader<File> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean recursive = false;

    private List<File> files = new ArrayList<>();
    private Iterator<File> fileIterator;

    private MediationHelperService mediationHelperService;

    private String fileLocation;
    private String workFolder;

    //If true we will check if this mediation was triggered by a file upload and if so only process the 1 file.
    private boolean checkForFileUpload;

    @BeforeStep
    public void initializeFolders(StepExecution stepExecution) {
        try {

            fileLocation = checkForFileUpload ? stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY) : null;
            if(fileLocation != null) {
                logger.debug("Single file: {}", fileLocation);
            } else {
                fileLocation = workFolder;
                logger.debug("Path: {}", fileLocation);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setMediationHelperService(MediationHelperService mediationHelperService) {
        this.mediationHelperService = mediationHelperService;
    }

    public void setCheckForFileUpload(boolean checkForFileUpload) {
        this.checkForFileUpload = checkForFileUpload;
    }

    public void setWorkFolder(String workingFolder) {
        this.workFolder = workingFolder;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        File file = new File(fileLocation);
        listFiles(file);

        fileIterator = files.iterator();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
        fileIterator = null;
    }

    @Override
    public File read() throws Exception, ParseException, NonTransientResourceException {
        if(fileIterator.hasNext()) {
            return fileIterator.next();
        } else {
            return null;
        }
    }

    private void listFiles(File file) {
        if(!file.exists()) {
            logger.warn("File does not exist {}", file);
        } else if(file.isFile()) {
            files.add(file);
        } else if(file.isDirectory() && recursive) {
            for(File f : file.listFiles()) {
                listFiles(f);
            }
        }
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }


}
