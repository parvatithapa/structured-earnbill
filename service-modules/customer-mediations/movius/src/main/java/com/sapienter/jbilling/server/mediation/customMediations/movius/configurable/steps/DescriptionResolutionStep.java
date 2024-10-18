package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class DescriptionResolutionStep  extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(DescriptionResolutionStep.class);
    private static final String DESTINATION_NUMBER_FIELD_NAME = "Destination Number";
    private String sourceNumber;
    private String destinationNumber;
    private String description;
    
    public DescriptionResolutionStep(String sourceNumber, String destinationNumber, String description) {
        this.sourceNumber = sourceNumber;
        this.destinationNumber = destinationNumber;
        this.description = description;
    }
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            List<PricingField> fields = context.getPricingFields();
            PricingField calledField = PricingField.find(fields, destinationNumber);
            PricingField callingField = PricingField.find(fields, sourceNumber);
            
            if(null == callingField || null == calledField) {
                result.addError("ERR-DESCRIPTION-NOT-FOUND");
                return false;
            }
            result.setDescription(description + " Source Number: " + callingField.getStrValue() + " Destination Number: " + calledField.getStrValue());
            result.setSource(callingField.getStrValue());
            result.setDestination(calledField.getStrValue());

            fields.add(new PricingField(DESTINATION_NUMBER_FIELD_NAME, result.getDestination()));

            return true;
        } catch(Exception ex) {
            result.addError("ERR-DESCRIPTION-NOT-RESOLVED");
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }

}
