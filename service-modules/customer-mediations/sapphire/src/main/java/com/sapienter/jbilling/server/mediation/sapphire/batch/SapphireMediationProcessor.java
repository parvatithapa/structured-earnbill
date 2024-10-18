package com.sapienter.jbilling.server.mediation.sapphire.batch;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.CdrSkipPolicy;

public class SapphireMediationProcessor implements ItemProcessor<ICallDataRecord, ConversionResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Value("#{jobExecutionContext['mediationProcessId']}")
    private UUID mediationProcessId;

    @Value("#{jobParameters['mediationCfgId']}")
    private Integer configId;

    private IMediationCdrResolver cdrResolver;

    private JMRRepository jmrRepository;

    private JMErrorRepository errorRepository;

    public SapphireMediationProcessor(JMRRepository jmrRepository, JMErrorRepository errorRepository, IMediationCdrResolver cdrResolver) {
        this.cdrResolver = cdrResolver;
        this.jmrRepository = jmrRepository;
        this.errorRepository = errorRepository;
    }

    @Override
    public ConversionResult process(ICallDataRecord cdr) throws Exception {
        if(StringUtils.isEmpty(cdr.getKey())) {
            logger.debug("skipped cdr because it is type of {} for entity id {}", CdrSkipPolicy.values(), entityId);
            return null;
        }

        if(null == cdr.getEntityId() || null == cdr.getMediationCfgId()) {
            cdr.setEntityId(entityId);
            cdr.setMediationCfgId(configId);
        }

        PricingField cdrType = cdr.getField(SapphireMediationConstants.CDR_TYPE);

        MediationStepResult result = new MediationStepResult(cdr);
        result.setPricingFields(cdr.getFields().stream().map(PricingField::encode).collect(Collectors.joining(",")));
        ConversionResult conversionResult = new ConversionResult();
        if(!SapphireMediationConstants.LONG_CALL_CDR_TYPE.equals(cdrType.getStrValue())) {
            MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, cdr);
            // checking JMR status and creating records in database.
            if(MediationResolverStatus.SUCCESS.equals(mediationResolverStatus)) {
                conversionResult.setRecordCreated(result.tojBillingMediationRecord());
            } else if(result.hasError()) {
                conversionResult.setErrorRecord(result.toJBillingMediationError());
            }
        } else {
            result.addError("LONG-CALL-NOT-SUPPORTED");
            conversionResult.setErrorRecord(result.toJBillingMediationError());
        }
        conversionResult.setRecordProcessed(cdr);
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
