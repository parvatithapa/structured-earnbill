package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Created by marcolin on 07/10/15.
 */
public class FullCreativeCallDataRecordToConversionResult implements ItemProcessor<CallDataRecord, ConversionResult> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private JMRMediationCdrResolver resolver;
    private JobExecution jobExecution;
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

    public void setResolver(JMRMediationCdrResolver resolver) {
        this.resolver = resolver;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        jobExecution = stepExecution.getJobExecution();
        UUID mediationProcess = (UUID)stepExecution.getJobExecution().getExecutionContext()
                .get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
        if (mediationProcess != null) {
            this.mediationProcessId = mediationProcess;
        }
    }

    @Override
    public ConversionResult process(CallDataRecord callDataRecord) throws Exception {
        if(StringUtils.isEmpty(callDataRecord.getKey())) {
            return null;
        }
        logger.debug("Processing CDR {}", callDataRecord);
        return processCallDataRecord(Integer.parseInt(jobExecution.getJobParameters().getString("entityId")),
                Integer.parseInt(jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY)),
                callDataRecord, resolver);
    }

    public  ConversionResult processCallDataRecord(Integer entityId, Integer mediationCfgId,
            CallDataRecord callDataRecord, IMediationCdrResolver cdrResolver) {
        callDataRecord.setEntityId(entityId);
        callDataRecord.setMediationCfgId(mediationCfgId);

        MediationStepResult result = new MediationStepResult(callDataRecord);
        MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, callDataRecord);
        ConversionResult conversionResult = new ConversionResult();

        /**
         * if validation of step resolution results fails
         * change the mediation step resolver status to ERROR.
         */
        if (!validateStepResolution(result, entityId)) {
            mediationResolverStatus = MediationResolverStatus.ERROR;
        }

        if(MediationResolverStatus.SUCCESS.equals(mediationResolverStatus)) {
            conversionResult.setRecordCreated(result.tojBillingMediationRecord());
        } else if(result.hasError()) {
            conversionResult.setErrorRecord(result.toJBillingMediationError());
        }

        conversionResult.setRecordProcessed(callDataRecord);
        writerConversionResult(conversionResult);
        return conversionResult;
    }

    private  boolean validateStepResolution(MediationStepResult result, Integer entityId){

        List<Integer> companies = new ArrayList<>();
        boolean isMediationConfigurationGlobal = getMediationHelperService().isMediationConfigurationGlobal(result.getMediationCfgId());

        companies.add(entityId);
        if (isMediationConfigurationGlobal) {
            companies.addAll(getMediationHelperService().getChildEntitiesIds(entityId));
        }

        Integer userCompanyId = calculateOwningCompany(result, entityId);

        if (null != result.getUserId() && !companies.contains(userCompanyId)) {
            //resolved user does not belong to any of the companies in the hierarchy
            result.addError("JB_RESOLVED_USER_NOT_BELONG_TO_COMPANY");
            return false;
        }

        if (null != result.getItemId()) {
            Integer itemId = Integer.parseInt(result.getItemId());
            boolean visible = isProductVisibleToCompany(itemId,userCompanyId);
            if (!visible) {
                result.addError("JB_RESOLVED_ITEM_IS_NOT_VISIBLE_FOR_RESOLVED_USER");
                return false;
            }
        }
        return true;
    }

    private  boolean isProductVisibleToCompany(Integer itemId,Integer entityId) {
        Integer parentCompanyId = getMediationHelperService().getParentCompanyId(entityId);
        return MediationCacheManager.isProductVisibleToCompany(itemId, entityId, parentCompanyId);
    }

    private  Integer calculateOwningCompany(MediationStepResult result, Integer entityId) {
        if (null != result.getUserId()) {
            return getMediationHelperService().getUserCompanyByUserId(result.getUserId());
        } else {
            return entityId;
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