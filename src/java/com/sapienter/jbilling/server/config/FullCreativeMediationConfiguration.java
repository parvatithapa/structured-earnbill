package com.sapienter.jbilling.server.config;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.reader.MediationTokenizer;
import com.sapienter.jbilling.server.mediation.converter.common.reader.TokenizedFormatFactory;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.common.validation.MediationResultValidationStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps.DescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps.EventDateResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps.ItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps.ItemResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps.UserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs.FullCreativeCallDataRecordToConversionResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader.FullCreativeMediationReader;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader.FullCreativeMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader.FullCreativeRecordSeparatorPolicy;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.validation.steps.CdrTypeValidationStep;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.mediation.mrim.RatingSchemeDAS;

@Configuration
public class FullCreativeMediationConfiguration {

    private static final String DIRECTION_FIELD_NAME = "Direction";

    @Autowired
    private ApplicationContext applicationContext;

    private Map<MediationStepType, IMediationStep<MediationStepResult>> fcMediationSteps(MediationHelperService mediationHelperService, RatingSchemeDAS ratingSchemeDAS) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new EnumMap<>(MediationStepType.class);
        steps.put(MediationStepType.USER_CURRENCY,   new UserResolutionStep("DNIS", mediationHelperService));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep("Date", "Time", new SimpleDateFormat("MM/dd/yy hh:mm:ss a")));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(DIRECTION_FIELD_NAME));
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep("Duration (s)", ratingSchemeDAS, mediationHelperService));
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep("Caller_ID", "DNIS", DIRECTION_FIELD_NAME));
        @SuppressWarnings("unchecked")
        IMediationStep<MediationStepResult> pricingFieldStep = (IMediationStep<MediationStepResult>) applicationContext.getBean("fcJMRPricingResolutionStep");
        steps.put(MediationStepType.PRICING, pricingFieldStep);
        return steps;
    }

    private Map<MediationStepType, IMediationStepValidation> fcValidationSteps() {
        Map<MediationStepType, IMediationStepValidation> validationSteps = new LinkedHashMap<>();
        IMediationStepValidation duplicateMediationStepValidation = (IMediationStepValidation) applicationContext.getBean("fcDuplicateRecordValidationStep");
        validationSteps.put(MediationStepType.DUPLICATE_RECORD_VALIDATION, duplicateMediationStepValidation);
        validationSteps.put(MediationStepType.CDR_TYPE_VALIDATION, new CdrTypeValidationStep(DIRECTION_FIELD_NAME));
        validationSteps.put(MediationStepType.MEDIATION_RESULT_VALIDATION, new MediationResultValidationStep());
        return validationSteps;
    }

    @Bean
    public JMRMediationCdrResolver fcCdrResolver(MediationHelperService mediationHelperService, RatingSchemeDAS ratingSchemeDAS) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.setSteps(fcMediationSteps(mediationHelperService, ratingSchemeDAS));
        cdrResolver.setValidationSteps(fcValidationSteps());
        return cdrResolver;
    }

    @Bean
    public FullCreativeCallDataRecordToConversionResult fcCdrProcessor(@Qualifier("fcCdrResolver") JMRMediationCdrResolver resolver,
            MediationHelperService mediationHelperService) {
        FullCreativeCallDataRecordToConversionResult cdrProcessor = new FullCreativeCallDataRecordToConversionResult();
        cdrProcessor.setMediationHelperService(mediationHelperService);
        cdrProcessor.setResolver(resolver);
        return cdrProcessor;
    }

    @Bean
    public TokenizedFormatFactory fcTokenizedFormat(@Qualifier("fcMediationTokenizer") MediationTokenizer mediationTokenizer) {
        TokenizedFormatFactory tokenizedFormatFactory = new TokenizedFormatFactory();
        tokenizedFormatFactory.setFormatFilename("/custom-mediations/single-machine/full-creative/cdr-formats/inbound-call-cdr-format.xml");
        tokenizedFormatFactory.setTokenizer(mediationTokenizer);
        return tokenizedFormatFactory;
    }

    @Bean
    public FullCreativeMediationRecordLineConverter fcRecordLineConverter(@Qualifier("fcTokenizedFormat") Format format) {
        FullCreativeMediationRecordLineConverter lineConverter = new FullCreativeMediationRecordLineConverter();
        lineConverter.setFormat(format);
        lineConverter.setDateTimeFormatter("MM/dd/yyyy HH:mm:ss");
        return lineConverter;
    }

    @Bean
    public FullCreativeMediationReader fcMediationReader(@Qualifier("fcRecordSeparatorPolicy")FullCreativeRecordSeparatorPolicy recordSeparatorPolicy,
            @Qualifier("fcRecordLineConverter") FullCreativeMediationRecordLineConverter lineMapper) {
        FullCreativeMediationReader fcReader = new FullCreativeMediationReader();
        fcReader.setLinesToSkip(0);
        fcReader.setStrict(false);
        fcReader.setLineMapper(lineMapper);
        fcReader.setRecordSeparatorPolicy(recordSeparatorPolicy);
        return fcReader;
    }

}
