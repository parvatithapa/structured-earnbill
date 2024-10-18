package com.sapienter.jbilling.server.mediation.custommediation.spc.mur;

import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.DATA_EVENT_DATE_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.DATA_EVENT_DATE_FORMAT;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.JMR_DEFAULT_WRITER_BEAN;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.OPTUS_MUR_JOB_NAME;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.OPTUS_MUR_LINE_MAPPER;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.OPTUS_MUR_PROCESSOR;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.OPTUS_MUR_RECYCLE_JOB_NAME;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;

import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.MediationJobs;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.reader.MediationTokenizer;
import com.sapienter.jbilling.server.mediation.converter.common.reader.SeparatorMediationTokenizer;
import com.sapienter.jbilling.server.mediation.converter.common.reader.TokenizedFormatFactory;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch.OptusMurCdrLineMapper;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch.OptusMurCdrProcessor;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps.EventDateResolutionStep;

@Configuration
@ComponentScan(basePackages = {"com.sapienter.jbilling.server.mediation.custommediation.spc.mur"})
public class OptusMurMediationConfiguration {

    private static final String FIELD_SEPARATOR = ",";

    @PostConstruct
    void init() {
        MediationJobs.addJob(new OptusMurMediationJob(OPTUS_MUR_JOB_NAME, OPTUS_MUR_LINE_MAPPER,
                OPTUS_MUR_PROCESSOR, JMR_DEFAULT_WRITER_BEAN, OPTUS_MUR_RECYCLE_JOB_NAME));
        MediationJobs.addJob(new OptusMurMediationJob(OPTUS_MUR_RECYCLE_JOB_NAME, OPTUS_MUR_LINE_MAPPER,
                OPTUS_MUR_PROCESSOR, JMR_DEFAULT_WRITER_BEAN, null));
    }

    @Autowired
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    @Bean
    public JMRMediationCdrResolver dataCdrResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addStep(MediationStepType.USER_CURRENCY, (IMediationStep<MediationStepResult>) applicationContext.getBean("optusMurUserResolutionStep"));
        cdrResolver.addStep(MediationStepType.ITEM_RESOLUTION, (IMediationStep<MediationStepResult>) applicationContext.getBean("optusMurDataItemResolutionStep"));
        cdrResolver.addStep(MediationStepType.QUANTITY, (IMediationStep<MediationStepResult>) applicationContext.getBean("optusMurDataItemQuantityResolutionStep"));
        IMediationStep<MediationStepResult> eventResolutionStep = new EventDateResolutionStep(new SimpleDateFormat(DATA_EVENT_DATE_FORMAT), DATA_EVENT_DATE_FIELD_NAME);
        cdrResolver.addStep(MediationStepType.EVENT_DATE, eventResolutionStep);
        cdrResolver.addStep(MediationStepType.DESCRIPTION, (IMediationStep<MediationStepResult>) applicationContext.getBean("optusMurDataItemDescriptionResolutionStep"));
        cdrResolver.addStep(MediationStepType.PRICING, (IMediationStep<MediationStepResult>) applicationContext.getBean(SPCConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        return cdrResolver;
    }


    @Bean
    public MediationTokenizer murMediationTokenizer() {
        return new SeparatorMediationTokenizer(FIELD_SEPARATOR);
    }

    @Bean
    public TokenizedFormatFactory murHeaderFormat(@Qualifier("murMediationTokenizer") MediationTokenizer mediationTokenizer) {
        TokenizedFormatFactory tokenizedFormatFactory = new TokenizedFormatFactory();
        tokenizedFormatFactory.setFormatFilename("/cdr-formats/mur/spc-mur-header-format.xml");
        tokenizedFormatFactory.setTokenizer(mediationTokenizer);
        return tokenizedFormatFactory;
    }

    @Bean
    public TokenizedFormatFactory murTailFormat(@Qualifier("murMediationTokenizer") MediationTokenizer mediationTokenizer) {
        TokenizedFormatFactory tokenizedFormatFactory = new TokenizedFormatFactory();
        tokenizedFormatFactory.setFormatFilename("/cdr-formats/mur/spc-mur-tail-format.xml");
        tokenizedFormatFactory.setTokenizer(mediationTokenizer);
        return tokenizedFormatFactory;
    }

    @Bean
    public TokenizedFormatFactory murDataFormat(@Qualifier("murMediationTokenizer") MediationTokenizer mediationTokenizer) {
        TokenizedFormatFactory tokenizedFormatFactory = new TokenizedFormatFactory();
        tokenizedFormatFactory.setFormatFilename("/cdr-formats/mur/spc-mur-data-cdr-fomat.xml");
        tokenizedFormatFactory.setTokenizer(mediationTokenizer);
        return tokenizedFormatFactory;
    }

    @Bean
    public LineMapper<ICallDataRecord> optusMurLineMapper() {
        Map<String, Format> cdrTypeAndFormatMap = new HashMap<>();
        cdrTypeAndFormatMap.put("H", (Format) applicationContext.getBean("murHeaderFormat"));
        cdrTypeAndFormatMap.put("T", (Format) applicationContext.getBean("murTailFormat"));
        cdrTypeAndFormatMap.put("G", (Format) applicationContext.getBean("murDataFormat"));
        return new OptusMurCdrLineMapper(cdrTypeAndFormatMap, FIELD_SEPARATOR);
    }

    @Bean
    @Scope("step")
    public FlatFileItemReader<ICallDataRecord> optusMurReader(@Qualifier("optusMurLineMapper") LineMapper<ICallDataRecord> lineMapper,
            @Value("#{jobParameters['filePath']}") String filePath) throws Exception {
        FlatFileItemReader<ICallDataRecord> optusReader = new FlatFileItemReader<>();
        optusReader.setLineMapper(lineMapper);
        optusReader.setStrict(false);
        optusReader.setResource(new FileSystemResource(filePath));
        optusReader.afterPropertiesSet();
        return optusReader;
    }

    @Bean
    @Scope("step")
    public ItemProcessor<ICallDataRecord, ConversionResult> optusMurCdrProcessor() {
        return new OptusMurCdrProcessor();
    }
}
