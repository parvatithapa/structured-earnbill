package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import com.sapienter.jbilling.common.Util;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class CombinedCdrToIndividualFileProcessor implements ItemProcessor<File, CombinedCdrProcessResult> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    
    private JobExecution jobExecution;
    private String outputFolder;
    private String workingFolder;
    private String decrypt;
    private String gpgPassphrase;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        jobExecution = stepExecution.getJobExecution();
        File folder = new File(outputFolder);
        if(!folder.exists()) {
            folder.mkdir();
        }

        folder = new File(workingFolder);
        createFolder(folder);

        folder = new File(decryptFolder());
        createFolder(folder);
    }

    private void createFolder(File folder) {
        if(!folder.exists()) {
            folder.mkdir();
        } else {
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public CombinedCdrProcessResult process(File packageZipFile) throws Exception {
        logger.debug("Start Processing -> {}", packageZipFile);
        CombinedCdrProcessResult result = new CombinedCdrProcessResult(packageZipFile);
        File folder = new File(extractFolder(packageZipFile));

        File file = packageZipFile;

        if("true".equals(decrypt)) {
            file = decryptZipPackage(packageZipFile, result);
        }

        if(result.isValid()) {
            extractZipPackage(folder, file, result);
        }

        if(result.isValid()) {
            validateCheckSum(folder, result);
        }

        if(result.isValid()) {
            extractCdrFiles(folder, result);
        }
        logger.debug("End Processing -> {}", packageZipFile);
        return result;
    }

    private void extractCdrFiles(File folder, CombinedCdrProcessResult result) {
        File[] zipFiles = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("zip");
            }
        });

        //there will only be 1 zip file - validateChecksum will catch this
        try {
            int nrEntries = Util.extractZipFile(zipFiles[0], new File(outputFolder + File.separator));
            if(nrEntries == 0) {
                logger.warn("No files present in zip file {} ", zipFiles[0]);
                result.addError(CombinedCdrProcessResult.ERROR_EXTRACTING_CDR_FILES);
                result.invalid();
            }
            Util.deleteRecursive(folder);
        }  catch (Exception e) {
            result.addError(CombinedCdrProcessResult.ERROR_EXTRACTING_CDR_FILES);
            result.invalid();
        }
    }

    private void validateCheckSum(File folder, CombinedCdrProcessResult result) throws Exception {
        File[] files = folder.listFiles();
        if(files.length != 2) {
            result.addError(CombinedCdrProcessResult.ZIP_PACKAGE_NOT_2_FILES);
            result.invalid();
            return;
        }

        String md5SumCalculated = null;
        String md5SumFromFile = "";

        for(File file : files) {
            if(file.getName().endsWith("zip")) {
                md5SumCalculated = Util.calcMD5Checksum(file).toLowerCase();
            } else {
                md5SumFromFile = FileUtils.readFileToString(file).toLowerCase();
                file.delete();
            }
        }

        if(!md5SumFromFile.equals(md5SumCalculated)) {
            result.addError(CombinedCdrProcessResult.ERROR_CHECKSUM);
            result.invalid();
            return;
        }
    }

    private File decryptZipPackage(File packageZipFile, CombinedCdrProcessResult result) throws Exception {
        File decryptedFile = null;
        try {
            File folder = new File(decryptFolder());
            String fileName = packageZipFile.getName();
            String decryptedFileName = fileName.endsWith(".pgp") ? fileName.substring(0, fileName.length()-4) : ("dec_+" + fileName);
            decryptedFile = new File(folder, decryptedFileName);
            String output = Util.gpgDecrypt(packageZipFile.getAbsolutePath(), decryptedFile.getAbsolutePath(), gpgPassphrase);
            if(!decryptedFile.exists()) {
                result.addError(CombinedCdrProcessResult.DECRYPTION_FAILED);
                result.invalid();
                logger.error(output);
            }
        }  catch (Exception e) {
            logger.error("Error decrypting zip files {} ", packageZipFile.getName());
            result.addError(CombinedCdrProcessResult.DECRYPTION_FAILED);
            result.invalid();
        }
        return decryptedFile;
    }

    private void extractZipPackage(File folder, File packageZipFile, CombinedCdrProcessResult result) throws Exception {
        try {
            if(!folder.exists()) {
                folder.mkdir();
            }
            int nrEntries = Util.extractZipFile(packageZipFile, folder);
            if(nrEntries == 0) {
                result.addError(CombinedCdrProcessResult.INVALID_ZIP_PACKAGE);
                result.invalid();
            }
        }  catch (Exception e) {
            result.addError(CombinedCdrProcessResult.INVALID_ZIP_PACKAGE);
            result.invalid();
        }
    }

    private String extractFolder(File packageZipFile) {
        return workingFolder + File.separator + packageZipFile.getName().replace('.', '_') + File.separator;
    }

    private String decryptFolder() {
        return workingFolder + File.separator + "decrypt" + File.separator;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setWorkingFolder(String workingFolder) {
        this.workingFolder = workingFolder;
    }

    public void setDecrypt(String decrypt) {
        this.decrypt = decrypt;
    }

    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }
}
