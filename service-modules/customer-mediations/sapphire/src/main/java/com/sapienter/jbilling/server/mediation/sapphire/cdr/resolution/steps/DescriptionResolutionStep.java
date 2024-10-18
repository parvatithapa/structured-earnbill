package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;

public class DescriptionResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String cdrType = context.getPricingField(SapphireMediationConstants.CDR_TYPE).getStrValue();
            if(SapphireMediationConstants.AMBIGUOUS_CDR_TYPE.equals(cdrType)) {
                result.addError("ERROR-DESCRITPION-RESOLUTION");
                return false;
            }
            DescriptionResolutionStrategy descriptionResolutionStrategy = DescriptionResolutionStrategy.getStrategy(cdrType);
            String description = descriptionResolutionStrategy.generateDescription(context);
            logger.debug("Description {}", description);
            result.setDescription(description);
            return true;
        } catch(Exception ex) {
            logger.error("Description Resolution Failed!", ex);
            result.addError("ERROR-DESCRITPION-RESOLUTION");
            return false;
        }
    }

}
