package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;

/**
 * Created by neelabh on 05/06/16.
 */
public class FullCreativeMediationReader extends FlatFileItemReader<ICallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @BeforeStep
    public void setMediationResourceToRead(StepExecution stepExecution) {
        try {
            JobParameters jobParameters = stepExecution.getJobParameters();
            String fileToReadPath = jobParameters.getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
            String entityId = jobParameters.getString(MediationServiceImplementation.PARAMETER_MEDIATION_ENTITY_ID_KEY);
            UUID mediationProcessId = (UUID)stepExecution.getJobExecution()
                    .getExecutionContext()
                    .get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
            logger.debug("mediation job parameters {}", jobParameters);
            if (StringUtils.isNotEmpty(fileToReadPath)) {
                File file = Paths.get(fileToReadPath).toFile();
                setResource(new FileSystemResource(file));
                logger.debug("file [{}] size is [{}] for entity id [{}] for mediation process [{}]", file.getName(),
                        FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(file)), entityId, mediationProcessId);
            }
        } catch (Exception ex) {
            logger.error("error in setMediationResourceToRead for ", ex);
        }
    }

}
