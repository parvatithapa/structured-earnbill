package com.sapienter.jbilling.server.config;

import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.DESCRIPTION;
import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.EVENT_DATE;
import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.ITEM_RESOLUTION;
import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.PRICING;
import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.QUANTITY;
import static com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType.USER_ID_AND_CURRENCY;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CONNECT_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.SEQUENCE_NUM;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireJobParameterValidatorImpl;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationJob;
import com.sapienter.jbilling.server.mediation.sapphire.batch.SapphireMediationProcessor;
import com.sapienter.jbilling.server.mediation.sapphire.batch.SapphireMediationReader;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.SapphireCdrCreator;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.DescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.EventDateResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.JMRPricingResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.ProductResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.QuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps.UserResolutionStep;
import com.sapienter.jbilling.server.mediation.sapphire.model.FileType;
import com.sapienter.jbilling.server.sapphire.SapphireMediationHelperServiceImpl;

@Configuration
public class SapphireMediationConfiguration {

    @PostConstruct
    public void init() {
        SapphireMediationJob.init();
    }

    @Bean
    public Unmarshaller cdrParser() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(FileType.class);
        return context.createUnmarshaller();
    }

    @Bean
    @Scope(value  = "step")
    public SapphireMediationReader sapphireMediationReader(@Value("#{jobParameters['entityId']}") Integer entityId,
            @Value("#{jobParameters['mediationCfgId']}") Integer configId, Unmarshaller cdrParser) {
        List<String> keys = Arrays.asList(SEQUENCE_NUM, CONNECT_TIME);
        SapphireCdrCreator cdrCreator = new SapphireCdrCreator(entityId, configId, keys);
        return new SapphireMediationReader(cdrCreator, cdrParser);
    }

    @Bean
    @Scope(value  = "step")
    public SapphireMediationProcessor sapphireMediationProcessor(JMRRepository jmrRepository,
            JMErrorRepository errorRepository, JMRMediationCdrResolver cdrResolver) {
        return new SapphireMediationProcessor(jmrRepository, errorRepository, cdrResolver);
    }

    @Bean
    public JMRMediationCdrResolver cdrResolver(SapphireMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(USER_ID_AND_CURRENCY, new UserResolutionStep());
        steps.put(EVENT_DATE, new EventDateResolutionStep(SapphireMediationConstants.DATE_FORMAT));
        steps.put(QUANTITY, new QuantityResolutionStep());
        steps.put(ITEM_RESOLUTION, new ProductResolutionStep(service));
        steps.put(DESCRIPTION, new DescriptionResolutionStep());
        steps.put(PRICING, new JMRPricingResolutionStep(service));
        cdrResolver.setSteps(steps);
        return cdrResolver;
    }

    @Bean
    public SapphireMediationHelperServiceImpl sapphireMediationHelperService() {
        return new SapphireMediationHelperServiceImpl();
    }

    @Bean
    public SapphireJobParameterValidatorImpl jobParameterValidator() {
        return new SapphireJobParameterValidatorImpl();
    }
}
