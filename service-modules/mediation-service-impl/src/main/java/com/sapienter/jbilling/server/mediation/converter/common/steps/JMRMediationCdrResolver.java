/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.steps;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.validation.DuplicateRecordValidationStep;
import com.sapienter.jbilling.server.mediation.converter.common.validation.MediationResultValidationStep;
import com.sapienter.jbilling.server.util.Context;

/**
 * Basic mediation cdr resolver. Contains multiple steps for resolving the CDR-s into JMR
 * <p/>
 * It splits the work of resolution into multiple mediation steps that will be executed
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public class JMRMediationCdrResolver implements IMediationCdrResolver {


    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Set<IMediationStep<MediationStepResult>> preProcessingSteps = new HashSet<>();

    private Map<MediationStepType, IMediationStep<MediationStepResult>> steps = new LinkedHashMap<>();
    private Map<MediationStepType, IMediationStepValidation> validationSteps = new LinkedHashMap<>();

    public JMRMediationCdrResolver() {
        initSteps();
    }

    private void initSteps() {

        // pre-defined validation steps
        validationSteps.put(MediationStepType.DUPLICATE_RECORD_VALIDATION, Context.getBean(DuplicateRecordValidationStep.BEAN_NAME));
        validationSteps.put(MediationStepType.MEDIATION_RESULT_VALIDATION, new MediationResultValidationStep());

        // default step values
        steps.put(MediationStepType.USER_CURRENCY, getUserResolutionStep());
        steps.put(MediationStepType.EVENT_DATE, getEventDateResolutionStep());
        steps.put(MediationStepType.ORDER_LINE_ITEM, getItemResolutionStep());
        steps.put(MediationStepType.PRICING, getPricingResolutionStep());
    }

    @Override
    public MediationResolverStatus resolveCdr(MediationStepResult result,
                                              ICallDataRecord record) {
        if (hasValidationProblem(MediationStepType.MEDIATION_RECORD_FORMAT_VALIDATION,
                String.format("Mediation record %s does not have a valid format.", record),
                record, result)) {
            return MediationResolverStatus.ERROR;
        }

        MediationStepContext context = new MediationStepContext(result, record, record.getEntityId());

        for (IMediationStep<MediationStepResult> entry : preProcessingSteps) {
            entry.executeStep(context);
        }

        if (hasValidationProblem(MediationStepType.DUPLICATE_RECORD_VALIDATION,
                String.format("Duplicate mediation record %s", record), record, result)) {
            return  MediationResolverStatus.DUPLICATE;
        }

        if (hasValidationProblem(MediationStepType.PHONE_NUMBER_VALIDATION,
                String.format("Mediation record %s does not have a valid phone number.", record),
                record, result)) {
            return MediationResolverStatus.ERROR;
        }

        if (hasValidationProblem(MediationStepType.CDR_TYPE_VALIDATION,
                String.format("Mediation record %s does not have a valid cdr type.", record),
                record, result)) {
            return MediationResolverStatus.ERROR;
        }

        // generic CDR process steps
        for (Map.Entry<MediationStepType, IMediationStep<MediationStepResult>> entry : steps.entrySet()) {
            entry.getValue().executeStep(context);
        }
        logger.warn("values set in result are: description: {}  ItemId: {} quantity: {} userId: {} eventDate: {}", result.getDescription(), result.getItemId(),
                result.getQuantity(), result.getUserId(),result.getEventDate());

        if (hasValidationProblem(MediationStepType.MEDIATION_RESULT_VALIDATION,
                String.format("Invalid mediation result %s returned on resolving the CDR record %s.", result, record),
                record, result)) {
            return MediationResolverStatus.ERROR;
        }

        return MediationResolverStatus.SUCCESS;
    }

    public boolean hasValidationProblem(MediationStepType stepType, String message,
                                        ICallDataRecord record, MediationStepResult result) {
        IMediationStepValidation validator = validationSteps.get(stepType);
        if (null != validator && !validator.isValid(record, result)) {
            logger.debug(message);
            return true;
        }
        return false;
    }


    public void setSteps(Map<MediationStepType, IMediationStep<MediationStepResult>> steps) {
        this.steps = steps;
    }

    public void clearPreProcessingSteps() {
        preProcessingSteps.clear();
    }

    public void addPreProcessingStep(IMediationStep<MediationStepResult> step) {
        preProcessingSteps.add(step);
    }

    public void setPreProcessingSteps(Set<IMediationStep<MediationStepResult>> steps) {
        this.preProcessingSteps = steps;
    }

    public void clearSteps() {
        steps.clear();
    }

    public void addStep(MediationStepType type, IMediationStep<MediationStepResult> step) {
        steps.put(type, step);
    }

    public void addValidationStep(MediationStepType type, IMediationStepValidation validation) {
        validationSteps.put(type, validation);
    }

    public Map<MediationStepType, IMediationStepValidation> getValidationSteps() {
        return validationSteps;
    }

    public void setValidationSteps(Map<MediationStepType, IMediationStepValidation> validationSteps) {
        this.validationSteps = validationSteps;
    }

    public void clearValidationSteps() {
        validationSteps.clear();
    }

    public void removeValidationSteps(MediationStepType type) {
        validationSteps.remove(type);
    }

    protected AbstractUserResolutionStep<MediationStepResult> getUserResolutionStep() {
        return new JMRUserLoginResolutionStep();
    }

    protected AbstractMediationStep<MediationStepResult> getEventDateResolutionStep() {
        return new JMREventDateResolutionStep();
    }

    protected AbstractItemResolutionStep<MediationStepResult> getItemResolutionStep() {
        return new JMRItemResolutionStep();
    }

    protected AbstractMediationStep<MediationStepResult> getPricingResolutionStep() {
        return new JMRPricingResolutionStep();
    }
}
