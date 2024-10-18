package com.sapienter.jbilling.server.config;

import static com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusMetaFieldName.*;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusHelperService;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.CDRRecordKeyAppendStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.DescriptionResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.EventDateResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.ItemQuantityResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.ItemResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.PhoneNumberValidationStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps.UserResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.incomingcall.steps.IncomingCallItemResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.incomingcall.steps.IncomingCallJMRBillableDeciderStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.incomingcall.steps.IncomingCallUserResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.incomingsmsdetails.steps.IncomingSMSDetailUserResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.incomingsmsdetails.steps.IncomingSMSDetailsJMRBillableDeciderStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingcall.steps.OutgoinCallItemResolutionStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingcall.steps.OutgoingCallJMRBillableDeciderStep;
import com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingsms.steps.OutgoingSMSJMRBillableDeciderStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.movius.MoviusHelperServiceImpl;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.util.Context;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
@Configuration
public class MoviusConfiguration {
    
    private Map<MediationStepType, IMediationStep<MediationStepResult>> outGoingCallSteps() {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_CURRENCY,   new UserResolutionStep(MoviusConstants.CALLING_ORG_ID));
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep(MoviusConstants.CALLING_NUMBER, MoviusConstants.CALLED_NUMBER_OC, MoviusConstants.CALL_FROM));
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep(MoviusConstants.CALL_DURATION_IN_MINUTES, MoviusConstants.CDR_TYPE));
        steps.put(MediationStepType.ITEM_RESOLUTION, new OutgoinCallItemResolutionStep());
        steps.put(MediationStepType.JMR_BILLABLE,    new OutgoingCallJMRBillableDeciderStep());
        steps.put(MediationStepType.PRICING,         Context.getBean(MoviusConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep(new SimpleDateFormat(MoviusConstants.DATE_FORMAT), MoviusConstants.TIMESTAMP));
        steps.put(MediationStepType.POST_PROCESS,    new CDRRecordKeyAppendStep());
        return steps;
    }
    
    private Map<MediationStepType, IMediationStep<MediationStepResult>> outGoingSmsDetailsSteps() {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_CURRENCY,   new UserResolutionStep(MoviusConstants.ORG_ID.toLowerCase()));
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep(MoviusConstants.PRIMARY_NUMBER, MoviusConstants.TO_NUMBER, MoviusConstants.SMS_FROM));
        //TODO : Add quantity field to resolve quantity for sms details
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep(MoviusConstants.SMS_QUANTITY, MoviusConstants.CDR_TYPE));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(SMS_ITEM_ID.getFieldName()));
        steps.put(MediationStepType.PRICING,         Context.getBean(MoviusConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep(new SimpleDateFormat(MoviusConstants.DATE_FORMAT), MoviusConstants.TIMESTAMP));
        steps.put(MediationStepType.POST_PROCESS,    new CDRRecordKeyAppendStep());
        return steps;
    }


    private Map<MediationStepType, IMediationStep<MediationStepResult>> outGoingSmsSteps() {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_CURRENCY,   new UserResolutionStep(MoviusConstants.ORG_ID.toLowerCase()));
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep(MoviusConstants.PRIMARY_NUMBER, MoviusConstants.TO_NUMBER, MoviusConstants.SMS_FROM));
        //TODO : Add quantity field to resolve quantity for sms details
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep(MoviusConstants.SMS_QUANTITY, MoviusConstants.CDR_TYPE));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(SMS_ITEM_ID.getFieldName()));
        steps.put(MediationStepType.JMR_BILLABLE,    new OutgoingSMSJMRBillableDeciderStep());
        steps.put(MediationStepType.PRICING,         Context.getBean(MoviusConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep(new SimpleDateFormat(MoviusConstants.DATE_FORMAT), MoviusConstants.TIMESTAMP));
        steps.put(MediationStepType.POST_PROCESS,    new CDRRecordKeyAppendStep());
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> incomingCallSteps() {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_CURRENCY,   new IncomingCallUserResolutionStep());
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep(MoviusConstants.CALLING_NUMBER, MoviusConstants.CALLED_NUMBER_IC, MoviusConstants.CALL_FROM));
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep(MoviusConstants.CALL_DURATION_IN_MINUTES, MoviusConstants.CDR_TYPE));
        steps.put(MediationStepType.ITEM_RESOLUTION, new IncomingCallItemResolutionStep());
        steps.put(MediationStepType.JMR_BILLABLE,    new IncomingCallJMRBillableDeciderStep());
        steps.put(MediationStepType.PRICING,         Context.getBean(MoviusConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep(new SimpleDateFormat(MoviusConstants.DATE_FORMAT), MoviusConstants.TIMESTAMP));
        steps.put(MediationStepType.POST_PROCESS,    new CDRRecordKeyAppendStep());
        return steps;
    }

    private Map<MediationStepType, IMediationStep<MediationStepResult>> inComingSmsDetailsSteps() {
        Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
        steps.put(MediationStepType.USER_CURRENCY,   new IncomingSMSDetailUserResolutionStep());
        steps.put(MediationStepType.DESCRIPTION,     new DescriptionResolutionStep(MoviusConstants.FROM_NUMBER, MoviusConstants.PRIMARY_NUMBER, MoviusConstants.INCOMING_SMS_FROM));
        //TODO : Add quantity field to resolve quantity for incoming sms details
        steps.put(MediationStepType.ORDER_LINE_ITEM, new ItemQuantityResolutionStep(MoviusConstants.SMS_QUANTITY, MoviusConstants.CDR_TYPE));
        steps.put(MediationStepType.ITEM_RESOLUTION, new ItemResolutionStep(SMS_ITEM_ID.getFieldName()));
        steps.put(MediationStepType.JMR_BILLABLE,    new IncomingSMSDetailsJMRBillableDeciderStep());
        steps.put(MediationStepType.PRICING,         Context.getBean(MoviusConstants.DEFAULT_PRICING_RESOLUTION_STEP));
        steps.put(MediationStepType.EVENT_DATE,      new EventDateResolutionStep(new SimpleDateFormat(MoviusConstants.DATE_FORMAT), MoviusConstants.TIMESTAMP));
        steps.put(MediationStepType.POST_PROCESS,    new CDRRecordKeyAppendStep());
        return steps;
    }
    /**
     * CDR Resolver Bean for Out going call cdr type
     * @return
     */
    @Bean
    public JMRMediationCdrResolver outGoingCallsCDRResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.removeValidationSteps(MediationStepType.DUPLICATE_RECORD_VALIDATION);
        // Phone Number Validation Step
        cdrResolver.addValidationStep(MediationStepType.PHONE_NUMBER_VALIDATION, new PhoneNumberValidationStep(MoviusConstants.CALLED_NUMBER_OC));
        cdrResolver.clearSteps();
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : outGoingCallSteps().entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }
    
    /**
     * CDR Resolver Bean for Out going sms details cdr type
     * @return
     */
    @Bean
    public JMRMediationCdrResolver outGoingSmsDetailsCDRResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.removeValidationSteps(MediationStepType.DUPLICATE_RECORD_VALIDATION);
        // Phone Number Validation Step
        cdrResolver.addValidationStep(MediationStepType.PHONE_NUMBER_VALIDATION, new PhoneNumberValidationStep(MoviusConstants.TO_NUMBER));
        cdrResolver.clearSteps();
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : outGoingSmsDetailsSteps().entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }
    
    /**
     * CDR Resolver Bean for Out going sms cdr type
     * @return
     */
    @Bean
    public JMRMediationCdrResolver outGoingSmsCDRResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.removeValidationSteps(MediationStepType.DUPLICATE_RECORD_VALIDATION);
        // Phone Number Validation Step
        cdrResolver.addValidationStep(MediationStepType.PHONE_NUMBER_VALIDATION, new PhoneNumberValidationStep(MoviusConstants.TO_NUMBER));
        cdrResolver.clearSteps();
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : outGoingSmsSteps().entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for incoming call cdr resolver
     * @return
     */
    @Bean
    public JMRMediationCdrResolver incomingCallsCDRResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.removeValidationSteps(MediationStepType.DUPLICATE_RECORD_VALIDATION);
        // Phone Number Validation Step
        cdrResolver.addValidationStep(MediationStepType.PHONE_NUMBER_VALIDATION, new PhoneNumberValidationStep(MoviusConstants.CALLED_NUMBER_IC));
        cdrResolver.clearSteps();
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : incomingCallSteps().entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }

    /**
     * CDR Resolver Bean for In Coming sms details cdr type
     * @return
     */
    @Bean
    public JMRMediationCdrResolver inComingSmsDetailsCDRResolver() {
        JMRMediationCdrResolver cdrResolver = new JMRMediationCdrResolver();
        cdrResolver.removeValidationSteps(MediationStepType.DUPLICATE_RECORD_VALIDATION);
        // Phone Number Validation Step
        cdrResolver.addValidationStep(MediationStepType.PHONE_NUMBER_VALIDATION, new PhoneNumberValidationStep(MoviusConstants.PRIMARY_NUMBER));
        cdrResolver.clearSteps();
        for(Entry<MediationStepType, IMediationStep<MediationStepResult>> stepEntry : inComingSmsDetailsSteps().entrySet()) {
            cdrResolver.addStep(stepEntry.getKey() , stepEntry.getValue());
        }
        return cdrResolver;
    }
    
    @Bean
    public MoviusHelperService moviusHelperServiceBean() {
        return new MoviusHelperServiceImpl();
    }
    
    @Bean
    public PhoneNumberUtil phoneUtil() {
        return PhoneNumberUtil.getInstance();
    }

}
