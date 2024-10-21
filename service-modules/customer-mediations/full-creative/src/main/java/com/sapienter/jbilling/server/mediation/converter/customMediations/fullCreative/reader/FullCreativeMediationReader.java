package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;

/**
 * Created by neelabh on 05/06/16.
 */
public class FullCreativeMediationReader extends FlatFileItemReader<ICallDataRecord> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FullCreativeMediationReader.class));

    @BeforeStep
    public void setMediationResourceToRead(StepExecution stepExecution) {
        try {
            String fileToReadPath = stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
            if (fileToReadPath != null) {
                setResource(new FileSystemResource(fileToReadPath));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
 
}
