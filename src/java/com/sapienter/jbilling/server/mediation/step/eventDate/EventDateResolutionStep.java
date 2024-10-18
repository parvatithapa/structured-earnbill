//TODO MODULARIZATION: MEDIATION 2.0 USED IN UPDATE CURRENT ORDER
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

package com.sapienter.jbilling.server.mediation.step.eventDate;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Resolves event dates based on input pricing fields
 *
 * @author Panche Isajeski
 * @since 12/17/12
 */
public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EventDateResolutionStep.class));

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {

        PricingField start = PricingField.find(fields, "start");
        if (start != null) result.setEventDate(start.getDateValue());

        PricingField startTime = PricingField.find(fields, "start_time");
        if (startTime != null) result.setEventDate(startTime.getDateValue());

        LOG.debug("Set result date " + result.getEventDate());
        return  true;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }
}
