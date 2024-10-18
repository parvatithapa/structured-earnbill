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
package com.sapienter.jbilling.server.mediation.converter.common.reader;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.lang.String;
import java.util.zip.DataFormatException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Created by igutierrez on 15/13/17.
 */
public class DistributelMediationReader extends FlatFileItemReader {
    private String extensionFile;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DistributelMediationReader.class));
    private static final String REGEX_FILE_EXTENSION = "(.)*\\.%s$";
    private static final String REGEX_TEMPORAL_FILE_EXTENSION = "(.)*\\.%s(.)*\\.*tmp$";

    private static final String REGEX_REPLACE_FILE_EXTENSION = "\\.%s$";
    private static final String REGEX_REPLACE_TEMPORAL_FILE_EXTENSION = "\\.%s(.)*\\.*tmp$";

    public DistributelMediationReader(){
        super();
        extensionFile = "INV";
    }
    @BeforeStep
    public void setMediationResourceToRead(StepExecution stepExecution) throws DataFormatException{
        String fileToReadPath = stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
        FileSystemResource file = new FileSystemResource(fileToReadPath);
        if(hasValidExtension(file.getFile())) {
            try {
                if (fileToReadPath != null && validateAmountRecords(file.getFile())) {
                        setResource(prepareFileToProcess(file));
                } else {
                    prepareFileWithError(file);
                    LOG.error(fileToReadPath + " - Invalid amount of records");
                    setResource(null);
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private boolean validateAmountRecords(File file) {
        String[] fileNameElement = file.getName().split("\\.");
        Long expectedRecords = 0L;
        try {
            Long lineFiles = Files.lines(Paths.get(file.getAbsolutePath())).count();
            if(isTemporalFile(file)){
                expectedRecords = Long.parseLong(fileNameElement[fileNameElement.length - 4]);
            } else {
                expectedRecords = Long.parseLong(fileNameElement[fileNameElement.length - 3]);
            }
            if (lineFiles.equals(expectedRecords.longValue())) {
                return true;
            } else {
                return false;
            }
            } catch(Exception e){
                LOG.error(e.getMessage());
                return false;
            }
    }

    public String getExtensionFile() {
        return extensionFile;
    }

    public void setExtensionFile(String extensionFile) {
        this.extensionFile = extensionFile;
    }

    private FileSystemResource prepareFileToProcess(FileSystemResource fileSystemResource) throws IOException{
        try{
            return new FileSystemResource(renameFile(fileSystemResource.getFile(), "PROCESSED").getAbsolutePath());
        }catch (IOException ex){
            throw ex;
        }
    }

    private FileSystemResource prepareFileWithError(FileSystemResource fileSystemResource) throws IOException{
        try{
            return new FileSystemResource(renameFile(fileSystemResource.getFile(), "ERROR").getAbsolutePath());
        }catch (IOException ex){
            throw ex;
        }
    }

    private File renameFile(File file, String complement) throws IOException{
        String absolutePath;
        if(isTemporalFile(file)){
            absolutePath = file.getAbsolutePath().replaceFirst(String.format(REGEX_REPLACE_TEMPORAL_FILE_EXTENSION,extensionFile), "\\." + complement);
        } else {
            absolutePath = file.getAbsolutePath().replaceFirst(String.format(REGEX_REPLACE_FILE_EXTENSION,extensionFile), "\\." + complement);
        }

        File renameFile = new File(absolutePath);
        if( !file.renameTo(renameFile)){
            throw new IOException("Error during rename file");
        }
        return renameFile;
    }

    public boolean hasValidExtension(File file){
        boolean hasValidExtension;
        if(isTemporalFile(file)){
            hasValidExtension = file.getName().matches(String.format(REGEX_TEMPORAL_FILE_EXTENSION,extensionFile));
        } else {
            hasValidExtension = file.getName().matches(String.format(REGEX_FILE_EXTENSION,extensionFile));
        }
        return hasValidExtension;
    }

    private boolean isTemporalFile(File file){
        return file.getName().matches(String.format(REGEX_TEMPORAL_FILE_EXTENSION,extensionFile));
    }
}
