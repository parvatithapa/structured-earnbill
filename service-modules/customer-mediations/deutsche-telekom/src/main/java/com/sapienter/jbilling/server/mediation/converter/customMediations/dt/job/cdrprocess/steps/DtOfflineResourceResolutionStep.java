package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.steps;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DtOfflineResourceResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String resourceIdField = "ResourceID";

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            String resourceId = context.getPricingField(resourceIdField).getStrValue().trim();
            context.getResult().setResourceId(resourceId);

        } catch (Exception e) {
            context.getResult().addError("ERR-RESOURCE");
            logger.info("ResourceID not found {}", context.getPricingFields());
            return false;
        }
        return true;
    }
}
