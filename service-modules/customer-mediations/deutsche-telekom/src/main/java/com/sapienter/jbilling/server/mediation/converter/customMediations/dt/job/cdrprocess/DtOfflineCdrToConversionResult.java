package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper.MediationHelperService;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class DtOfflineCdrToConversionResult implements ItemProcessor<ICallDataRecord, ConversionResult>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IMediationCdrResolver resolver;
    private int entityId = 0;
    private int mediationConfigId = 0;
    private MediationHelperService mediationHelperService;

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private JMErrorRepository errorRepository;

    private UUID mediationProcessId;

    public MediationHelperService getMediationHelperService() {
	    return mediationHelperService;
	}

	public void setMediationHelperService(MediationHelperService mediationHelperService) {
	    this.mediationHelperService = mediationHelperService;
	}

    public void setResolver(IMediationCdrResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    //    @BeforeStep
    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        entityId = Integer.parseInt(jobExecution.getJobParameters().getString("entityId"));
        mediationConfigId = Integer.parseInt(jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY));
        logger.info("Mediation Config Id before step : {}", mediationConfigId);
        mediationHelperService.loadProductMapCaches(entityId);
        UUID mediationProcess = (UUID)stepExecution.getJobExecution().getExecutionContext()
                .get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
        if (mediationProcess != null) {
            this.mediationProcessId = mediationProcess;
        }
    }

    @Override
    public ConversionResult process(ICallDataRecord callDataRecord) throws Exception {
    	if(null == callDataRecord.getKey() || callDataRecord.getKey().isEmpty()) {
    		return null;
    	}
        return processCallDataRecord(entityId, mediationConfigId, callDataRecord, resolver);
    }
 
    public  ConversionResult processCallDataRecord(Integer entityId, Integer mediationCfgId,
                                                   ICallDataRecord callDataRecord, IMediationCdrResolver cdrResolver) {
        callDataRecord.setEntityId(entityId);
        callDataRecord.setMediationCfgId(mediationCfgId);

        MediationStepResult result = new MediationStepResult(callDataRecord);
        result.setMediationProcessId(mediationProcessId);

        MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, callDataRecord);
        ConversionResult conversionResult = new ConversionResult();

        switch (mediationResolverStatus) {
            case SUCCESS:
                conversionResult.setRecordCreated(result.tojBillingMediationRecord());
                break;
            default:
                conversionResult.setErrorRecord(result.toJBillingMediationError());
                break;
        }
        conversionResult.setRecordProcessed(callDataRecord);
        writerConversionResult(conversionResult);
        return conversionResult;
    }

	public void writerConversionResult(ConversionResult result) {
            if (result.getErrorRecord() != null) {
                saveErrorRecord(result);
            } else {
                try {
                    result.getRecordCreated().setProcessId(mediationProcessId);
                    result.setRecordCreated(DaoConverter.getMediationRecord(
                            jmrRepository.saveAndFlush(DaoConverter.getMediationRecordDao(result))));

                } catch (DataIntegrityViolationException e) {
                    handleDataAccessExceptions(result, e.getCause());
                } catch (DataAccessException e) {
                    handleDataAccessExceptions(result, e);
                }
            }
    }


    private void saveErrorRecord(ConversionResult result) {
        try {
            result.getErrorRecord().setProcessId(mediationProcessId);
            result.setErrorRecord(DaoConverter.getMediationErrorRecord(
                    errorRepository.save(DaoConverter.getMediationErrorRecordDao(result))));

        } catch (DataIntegrityViolationException e) {
            result.setErrorRecord(null);
            logger.warn("Issue with SQL execution for key: {}", result.getErrorRecord().getRecordKey());
            logger.error("Couldn't process error record", e);
        }
    }


    private void handleDataAccessExceptions(ConversionResult result, Throwable e) {
        JbillingMediationRecord record = result.getRecordCreated();
        result.setRecordCreated(null);
        
        if (e.getClass().isAssignableFrom(DataException.class)) {
            logger.warn("DataException for record key {}", record.getRecordKey());
            logger.error("Fatal issue with SQL execution", e);
            
            throw new DtCDRConversionDataException(e.getCause(), record, "[JB-DATA-EXCEPTION]");

        } else if (e.getClass().isAssignableFrom(ConstraintViolationException.class)) {
            // CDR with the same record key may get inserted between duplicate validation and now.
            logger.warn("Constraint violation, duplicate key: {}", record.getRecordKey());
            throw new DtCDRConversionDataException(e.getCause(), record, "[JB-DUPLICATE]");
        }
    }
}
