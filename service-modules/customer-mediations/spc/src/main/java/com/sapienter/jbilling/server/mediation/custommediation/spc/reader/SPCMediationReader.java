package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.InvalidCDRFileNameFormatException;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Neelabh
 * @since Dec 18, 2018
  */
public class SPCMediationReader extends FlatFileItemReader<CallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @BeforeStep
    public void setMediationResourceToRead(StepExecution stepExecution) {
        try {
            String fileToReadPath = stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
            logger.debug("Reading File From {} ", fileToReadPath);
            boolean isFileValid = false;
            for(MediationServiceType serviceType : MediationServiceType.values()) {
                if(fileToReadPath != null && fileToReadPath.contains(serviceType.getFileNamePrefix())) {
                    isFileValid = true;
                    break;
                }
            }
            if(!isFileValid) {
                logger.debug("Skipping CDR process due to invalid file naming format");
                throw new InvalidCDRFileNameFormatException("Invalid CDR file name");
            }
            setResource(new FileSystemResource(fileToReadPath));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
}
