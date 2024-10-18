package com.sapienter.jbilling.server.config;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurMediationConfiguration;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.DescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.EventDateResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetDescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.ItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.InternetItemResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.ItemResolutionStep;
import com.sapienter.jbilling.server.mediation.custommediation.spc.steps.UserResolutionStep;
import com.sapienter.jbilling.server.spc.SPCMediationHelperServiceImpl;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Harshad
 * @since Dec 18, 2018
 */
@Configuration
@Import(value = { OptusMurMediationConfiguration.class })
public class SPCMediationConfiguration {

    private Map<MediationStepType, IMediationStep<MediationStepResult>> optusFixedLineSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.DATE_FORMAT),SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> optusMobileSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.DATE_FORMAT), SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> enginSConnectSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.ENGIN_SCON_TEL4G_DATE_FORMAT), SPCConstants.START_DATETIME, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> telstraMobile4GSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.P1_S_P_NUMBER_EC_ADDRESS, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.ENGIN_SCON_TEL4G_DATE_FORMAT), SPCConstants.P2_START_TIME_EC_TIMESTAMP, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.P2_DURATION_EC_VOLUME));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.P1_S_P_NUMBER_EC_ADDRESS, SPCConstants.P1_O_P_NUMBER_EC_ADDRESS));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> aaptInternetUsageSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.AAPT_INTERNET_DATE_TIME_FORMAT), SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.USAGE_DOWNLOAD, SPCConstants.USAGE_UPLOAD, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.USAGE_DOWNLOAD, SPCConstants.USAGE_UPLOAD));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> aaptVoipCtopSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.DATE_FORMAT),SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> telstraFixedLineSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE, new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.TELSTRA_DATE_FORMAT), null, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.TELSTRA_QUANTITY));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.TELSTRA_ORIGINATING_NUMBER,
                SPCConstants.TELSTRA_DESTINATION_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }
    
    private Map<MediationStepType, IMediationStep<MediationStepResult>> vocusInternetUsageSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.SCONNECT_DATA_DATE_TIME_FORMAT), SPCConstants.ACCT_START_TIME, service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.ACCT_INPUT_OCTETS, SPCConstants.ACCT_SESSION_TIME, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.ACCT_INPUT_OCTETS, SPCConstants.ACCT_SESSION_TIME));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> serviceElementsVoiceSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.FROM_NUMBER, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.SE_VOICE_DATE_FORMAT), SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new ItemQuantityResolutionStep(SPCConstants.DURATION));
        steps.put(MediationStepType.DESCRIPTION, new DescriptionResolutionStep(SPCConstants.FROM_NUMBER, SPCConstants.TO_NUMBER));
        steps.put(MediationStepType.PRICING, Context.getBean(SPCConstants.SPC_PRICING_RESOLUTION_STEP));
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> serviceElementsDataSteps(SPCMediationHelperService service) {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_ID_AND_CURRENCY, new UserResolutionStep(SPCConstants.USER_NAME, service));
        steps.put(MediationStepType.ITEM_RESOLUTION, new InternetItemResolutionStep(service));
        steps.put(MediationStepType.EVENT_DATE,
                new EventDateResolutionStep(new SimpleDateFormat(SPCConstants.SE_DATA_DATE_FORMAT), SPCConstants.EVENT_DATE, service));
        steps.put(MediationStepType.QUANTITY, new InternetItemQuantityResolutionStep(SPCConstants.DOWNLOAD, SPCConstants.UPLOAD, service));
        steps.put(MediationStepType.DESCRIPTION, new InternetDescriptionResolutionStep(SPCConstants.DOWNLOAD, SPCConstants.UPLOAD));
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
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : serviceElementsDataSteps(service).entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    @Bean
    public SPCMediationHelperServiceImpl spcMediationHelperService() {
        return new SPCMediationHelperServiceImpl();
    }

    @Bean
    public Tika metaDataExtractor() {
        return new Tika();
    }

}