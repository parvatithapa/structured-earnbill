package com.sapienter.jbilling.server.mediation.customMediations.movius.reader;

import java.io.File;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.customMediations.movius.FileExtractor;

public class MoviusMediationReader extends FlatFileItemReader<CallDataRecord> {
    
    private static final Logger LOG = LoggerFactory.getLogger(MoviusMediationReader.class);

    @BeforeStep
    public void setMediationResourceToRead(StepExecution stepExecution) {
        try {
            String fileToReadPath = stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
            String filePath = null;
            if (null!= fileToReadPath) {
                for(FileExtractor extractor : FileExtractor.values()) {
                    if(fileToReadPath.contains(extractor.getFileExtention())) {
                        filePath = extractor.decompress(new File(fileToReadPath));
                        break;
                    }
                }

                if(null == filePath) {
                    filePath = fileToReadPath;
                }   
                
                LOG.debug("Reading File From {} ", filePath);
                setResource(new FileSystemResource(filePath));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
