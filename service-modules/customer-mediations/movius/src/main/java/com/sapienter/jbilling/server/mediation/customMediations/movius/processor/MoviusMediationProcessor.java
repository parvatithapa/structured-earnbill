package com.sapienter.jbilling.server.mediation.customMediations.movius.processor;

import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.customMediations.movius.CDRResolverType;
import com.sapienter.jbilling.server.util.Context;

public class MoviusMediationProcessor implements ItemProcessor<CallDataRecord, ConversionResult> {

    private static final String CDR_TYPE_FIELD_NAME = "cdr-type";
    
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    
    @Value("#{jobParameters['mediationCfgId']}")
    private Integer configId;
    
    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private JMErrorRepository errorRepository;

    @Value("#{jobExecutionContext['mediationProcessId']}")
    private UUID mediationProcessId;
    
    public Integer getEntityId() {
        return entityId;
    }

    public Integer getConfigId() {
        return configId;
    }

    public UUID getMediationProcessId() {
        return mediationProcessId;
    }

    private JMRMediationCdrResolver getCDRResolverByCDRType(PricingField cdrTypeField) {
        if(null == cdrTypeField || StringUtils.isEmpty(cdrTypeField.getStrValue())) {
            return null;
        }

        JMRMediationCdrResolver cdrResolver = null ;
        for(CDRResolverType cdrResolverType : CDRResolverType.values()) {
            if(cdrTypeField.getStrValue().contains(cdrResolverType.getCdrType())) {
                cdrResolver = Context.getBean(cdrResolverType.getCdrResolverBeanName());
                break;
            }
        }
        return cdrResolver;
    }

    @Override
    public ConversionResult process(CallDataRecord callDataRecord) throws Exception {
        if(null == callDataRecord.getKey() || callDataRecord.getKey().isEmpty()) {
            return null;
        }
        
        callDataRecord.setEntityId(entityId);
        callDataRecord.setMediationCfgId(configId);
        
        MediationStepResult result = new MediationStepResult(callDataRecord);
        PricingField cdrTypeField = PricingField.find(callDataRecord.getFields(), CDR_TYPE_FIELD_NAME);
        JMRMediationCdrResolver cdrResolver = getCDRResolverByCDRType(cdrTypeField);

        if( null == cdrResolver) {
            return null;
        }

        result.setCdrType(cdrTypeField.getStrValue());
        MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, callDataRecord);
        ConversionResult conversionResult = new ConversionResult();
        
        // checking JMR status and creating records in database.
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
