package com.sapienter.jbilling.server.config;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tika.Tika;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationCacheInterceptor;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurMediationConfiguration;
import com.sapienter.jbilling.server.mediation.custommediation.spc.reader.SPCRecycleRowMapper;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.DescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.EventDateResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetDescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetItemResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.ItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.ItemResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.SPCRecordConversionValidationStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.UserResolutionStep;
import com.sapienter.jbilling.server.spc.SPCMediationHelperServiceImpl;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.spc.SpcReportHelperService;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Harshad
 * @since Dec 18, 2018
 */
@Configuration
@Import(value = { OptusMurMediationConfiguration.class })
public class SPCMediationConfiguration {

    private Map<MediationStepType, IMediationStep<MediationStepResult>> optusFixedLineSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.DATE_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> optusMobileSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(SPCConstants.DATE_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> enginSConnectSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(SPCConstants.ENGIN_SCON_TEL4G_DATE_FORMAT,
                SPCConstants.START_DATETIME, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> telstraMobile4GSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.ENGIN_SCON_TEL4G_DATE_FORMAT, SPCConstants.P1_INITIAL_START_TIME_EC_TIMESTAMP, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.P1_S_P_NUMBER_EC_ADDRESS, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.P2_DURATION_EC_VOLUME, SPCConstants.P2_DATA_VOLUME_EC_VOLUME, SPCConstants.P2_MESSAGES_EC_VOLUME));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.P1_S_P_NUMBER_EC_ADDRESS, SPCConstants.P1_O_P_NUMBER_EC_ADDRESS));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> aaptInternetUsageSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.AAPT_INTERNET_DATE_TIME_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.USAGE_DOWNLOAD, SPCConstants.USAGE_UPLOAD, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.USER_NAME,
                SPCConstants.USAGE_DOWNLOAD, SPCConstants.USAGE_UPLOAD));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> aaptVoipCtopSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.DATE_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> telstraFixedLineSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(SPCConstants.TELSTRA_DATE_FORMAT, null, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.TELSTRA_ASSET_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.TELSTRA_QUANTITY));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.TELSTRA_ASSET_NUMBER,
                SPCConstants.TELSTRA_DESTINATION_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> vocusInternetUsageSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.SCONNECT_DATA_DATE_TIME_FORMAT, SPCConstants.ACCT_STOP_TIME, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.ACCT_INPUT_OCTETS, SPCConstants.ACCT_SESSION_TIME, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.USER_NAME,
                SPCConstants.ACCT_INPUT_OCTETS, SPCConstants.ACCT_SESSION_TIME));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> serviceElementsVoiceSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.SE_VOICE_DATE_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> serviceElementsDataSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(SPCConstants.SE_DATA_DATE_FORMAT, SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.DOWNLOAD, SPCConstants.UPLOAD, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.USER_NAME,
                SPCConstants.DOWNLOAD, SPCConstants.UPLOAD));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> telstraMonthlyFixedLineSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(SPCConstants.TELSTRA_DATE_FORMAT, null, service));
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.TELSTRA_QUANTITY));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER,
                SPCConstants.TELSTRA_DESTINATION_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    /**
     * CDR Resolver Bean for Optus fixed line CDRs (record type are 50 & 51)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver optusFixedLineCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : optusFixedLineSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Optus mobile CDRs (all record types)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver optusMobileCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : optusMobileSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Engin & SConnect CDRs (all record types)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver enginSConnectCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : enginSConnectSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for AAPT Voip CTOP CDRs (record type are pwtdet)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver aaptVoipCtopCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for (Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : aaptVoipCtopSteps(service)
                .entrySet()) {
            cdrResolver.addStep(stepEntry.getKey(), stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for AAPT Internet Usage (all record types)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver aaptInternetUsageCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : aaptInternetUsageSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Vocus Internet Usage (all record types)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver vocusInternetUsageCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : vocusInternetUsageSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Telstra 4G mobile (Only UDR record types)
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver telstraMobile4GCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : telstraMobile4GSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Telstra fixed line CDRs
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver telstraFixedLineCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : telstraFixedLineSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Service Elements Voice CDRs
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver serviceElementsVoiceCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : serviceElementsVoiceSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }


    /**
     * CDR Resolver Bean for Service Elements Internet CDRs
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver serviceElementsDataCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : serviceElementsDataSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for Telstra monthly fixed line CDRs
     * @param service
     * @return
     */
    @Bean
    public JMRMediationCdrResolver telstraMonthlyFixedLineCDRResolver(SPCMediationHelperService service) {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.clearSteps();
        cdrResolver.addValidationStep(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION, new SPCRecordConversionValidationStep());
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : telstraMonthlyFixedLineSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public SPCMediationHelperServiceImpl spcMediationHelperService() {
        return new SPCMediationHelperServiceImpl();
    }

    @Bean
    public Tika metaDataExtractor() {
        return new Tika();
    }

    @Bean
    public SpcHelperService spcHelperService() {
        return new SpcHelperService();
    }

    @Bean
    public SpcReportHelperService spcReportHelperService() {
        return new SpcReportHelperService();
    }
    @Bean
    SPCMediationCacheInterceptor spcMediationCacheInterceptor() {
        return new SPCMediationCacheInterceptor();
    }

    @Bean
    @Scope(value = "step")
    ItemReader<CallDataRecord> spcCDRRecycleReader(@Qualifier("jBillingMediationDataSource") DataSource dataSource,
            @Qualifier("spcRecycleRowMapper") SPCRecycleRowMapper recycleRowMapper,
            @Value("#{jobParameters['recycleProcessId']}") UUID recycleProcessId) {
        JdbcPagingItemReader<CallDataRecord> databaseReader = new JdbcPagingItemReader<>();
        databaseReader.setDataSource(dataSource);
        databaseReader.setRowMapper(recycleRowMapper);
        databaseReader.setSaveState(false);
        databaseReader.setFetchSize(1000);
        databaseReader.setPageSize(1000);
        PagingQueryProvider queryProvider = createQueryProvider();
        databaseReader.setQueryProvider(queryProvider);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", "TO_BE_RECYCLED");
        parameters.put("processId", asBytes(recycleProcessId));
        databaseReader.setParameterValues(parameters);
        return databaseReader;
    }

    private PagingQueryProvider createQueryProvider() {
        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause("FROM jbilling_mediation_error_record");
        queryProvider.setWhereClause("WHERE process_id = :processId AND status = :status");
        queryProvider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));
        return queryProvider;
    }

    private static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
