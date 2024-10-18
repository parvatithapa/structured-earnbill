package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Created by marcomanzi on 2/17/14.
 */
public class SampleEventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            context.getResult().setEventDate(PricingField.find(context.getPricingFields(), "event-date").getDateValue());
            return true;
        } catch (Exception e) {
            context.getResult().addError("Event Not Found");
            return false;
        }
    }
}
