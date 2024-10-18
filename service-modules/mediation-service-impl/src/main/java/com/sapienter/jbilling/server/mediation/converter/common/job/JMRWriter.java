package com.sapienter.jbilling.server.mediation.converter.common.job;

import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

/**
 * Created by marcolin on 07/10/15.
 */
public class JMRWriter implements ItemWriter<ConversionResult> {

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private JMErrorRepository errorRepository;

    private UUID mediationProcessId;

    @Override
    public void write(List<? extends ConversionResult> list) throws Exception {
        for (ConversionResult result: list) {
            writerConversionResult(result);
        }
    }

    @BeforeStep
    public void setMediationProcessId(StepExecution stepExecution) {
        Object mediationProcess = stepExecution.getJobExecution().getExecutionContext()
                .get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
        if (mediationProcess != null) {
            this.mediationProcessId = (UUID) mediationProcess;
        }
    }


    public ConversionResult writerConversionResult(ConversionResult result) {
        if (result.getErrorRecord() != null) {
            result.getErrorRecord().setProcessId(mediationProcessId);
            result.setErrorRecord(DaoConverter.getMediationErrorRecord(
                    errorRepository.save(DaoConverter.getMediationErrorRecordDao(result))));
        } else {
            result.getRecordCreated().setProcessId(mediationProcessId);
            result.setRecordCreated(DaoConverter.getMediationRecord(
                    jmrRepository.save(DaoConverter.getMediationRecordDao(result))));
        }
        return result;
    }
}