package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

@Component("optusMurDataItemDescriptionResolutionStep")
class DataItemDescriptionResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BILLABLE_NUMBER_FIELD_NAME = "Billable Number";

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField billableNumberField = PricingField.find(context.getPricingFields(), BILLABLE_NUMBER_FIELD_NAME);
            if(null == billableNumberField) {
                result.addError("ERR-DESCRIPTION-NOT-FOUND");
                return false;
            }
            result.setDescription("Data Usage From "+ billableNumberField.getStrValue());
            return true;
        } catch(Exception ex) {
            result.addError("ERR-DESCRIPTION-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
