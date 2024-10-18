package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;

public class UserResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField cdrType = context.getPricingField(SapphireMediationConstants.CDR_TYPE);
            UserResolutionStrategy userResolutionStrategy = UserResolutionStrategy.getStrategyByCdrType(cdrType.getStrValue());
            userResolutionStrategy.resolveUser(context);
            if(SapphireMediationConstants.AMBIGUOUS_CDR_TYPE.equals(cdrType.getStrValue())) {
                cdrType = context.getPricingField(SapphireMediationConstants.CDR_TYPE);
            }
            result.setCdrType(cdrType.getStrValue());
            if(null!= result.getUserId()) {
                logger.debug("User resolved {}", result.getUserId());
                return true;
            }
            return false;
        } catch(Exception ex) {
            logger.error("User Resolution Failed!", ex);
            result.addError("ERR-USER-RESOLUTION");
            return false;
        }
    }

}
