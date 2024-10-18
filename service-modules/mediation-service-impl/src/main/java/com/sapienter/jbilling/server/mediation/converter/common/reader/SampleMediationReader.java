package com.sapienter.jbilling.server.mediation.converter.common.reader;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;

/**
 * Created by marcolin on 26/10/15.
 */
public class SampleMediationReader extends FlatFileItemReader {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SampleMediationReader.class));

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
