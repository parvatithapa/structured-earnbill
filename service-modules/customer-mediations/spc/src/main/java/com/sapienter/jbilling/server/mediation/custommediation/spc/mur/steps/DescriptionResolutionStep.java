package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class DescriptionResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String sourceNumber;
    private String destinationNumber;

    public DescriptionResolutionStep(String sourceNumber, String destinationNumber) {
        this.sourceNumber = sourceNumber;
        this.destinationNumber = destinationNumber;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {

            List<PricingField> fields = context.getPricingFields();
            PricingField callingField = PricingField.find(fields, sourceNumber);
            PricingField calledField = PricingField.find(fields, destinationNumber);

            if(null == callingField || null == calledField) {
                result.addError("ERR-DESCRIPTION-NOT-FOUND");
                return false;
            }

            result.setDescription("Call from " + callingField.getStrValue() + " to " + calledField.getStrValue());
            result.setSource(callingField.getStrValue());
            result.setDestination(calledField.getStrValue());
            return true;
        } catch(Exception ex) {
            result.addError("ERR-DESCRIPTION-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
