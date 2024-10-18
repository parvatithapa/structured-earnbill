package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurCdrResolver;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class OptusMurCdrProcessor implements ItemProcessor<ICallDataRecord, ConversionResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String SWITCH_TYPE = "Switch Type";

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

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public ConversionResult process(ICallDataRecord callDataRecord) {
        String key = callDataRecord.getKey();
        if(StringUtils.isEmpty(key)) {
            logger.debug("Skipping header/tail record");
            return null;
        }
        PricingField switchType = PricingField.find(callDataRecord.getFields(), SWITCH_TYPE);
        if(null == switchType || StringUtils.isEmpty(switchType.getStrValue())) {
            logger.debug("Skipping record {} since no switch type found for entity {} ", callDataRecord, entityId);
            return null;
        }
        callDataRecord.setEntityId(entityId);
        callDataRecord.setMediationCfgId(configId);
        MediationStepResult result = new MediationStepResult(callDataRecord);
        result.setChargeable(Boolean.FALSE);
        OptusMurCdrResolver murCdrResolver = OptusMurCdrResolver.getResolverBySwitchType(switchType.getStrValue());
        if(null == murCdrResolver) {
            logger.debug("Skipping record {} since no cdr resolver found for switch type {}", callDataRecord, switchType.getStrValue());
            return null;
        }
        JMRMediationCdrResolver cdrResolver = (JMRMediationCdrResolver) applicationContext.getBean(murCdrResolver.getResolverBeanName());
        if(null == cdrResolver) {
            logger.debug("Skipping record {} since no cdr resolver bean found for switch type {}", callDataRecord, switchType.getStrValue());
            return null;
        }
        result.setCdrType(murCdrResolver.getCdrType());
        MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, callDataRecord);
        ConversionResult conversionResult = new ConversionResult();

        // checking JMR status and creating records in database.
        if(MediationResolverStatus.SUCCESS.equals(mediationResolverStatus)) {
            conversionResult.setRecordCreated(result.tojBillingMediationRecord());
        } else {
            conversionResult.setErrorRecord(result.toJBillingMediationError());
        }

        conversionResult.setRecordProcessed(callDataRecord);
        writerConversionResult(conversionResult);
        return conversionResult;
    }

    /**
     * Stores JMR in database
     * @param result
     * @return
     */
    private ConversionResult writerConversionResult(ConversionResult result) {
        if (result.getErrorRecord() != null) {
            result.getErrorRecord().setProcessId(mediationProcessId);
            result.setErrorRecord(DaoConverter.getMediationErrorRecord(
                    errorRepository.save(DaoConverter.getMediationErrorRecordDao(result))));
        } else {
            result.getRecordCreated().setProcessId(mediationProcessId);
            result.setRecordCreated(DaoConverter.getMediationRecord(jmrRepository.save(DaoConverter.getMediationRecordDao(result))));
        }
        return result;
    }
}
