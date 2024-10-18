package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Neelabh
 * @since Mar 15, 2019
 */
public class InternetDescriptionResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String downloadUsage;
    private String uploadUsage;

    public InternetDescriptionResolutionStep(String downloadUsage, String uploadUsage) {
        this.downloadUsage = downloadUsage;
        this.uploadUsage = uploadUsage;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField downloadField = PricingField.find(context.getPricingFields(), downloadUsage);
            PricingField uploadField = PricingField.find(context.getPricingFields(), uploadUsage);

            if(null == downloadField || null == uploadField) {
                result.addError("ERR-DATA-USAGE-DESCRIPTION-NOT-FOUND");
                return false;
            }

            result.setSource(downloadField.getStrValue());
            result.setDestination(uploadField.getStrValue());
            String descriptionText = "Download usage " + downloadField.getStrValue() + " and " + 
                                     "Upload usage " + uploadField.getStrValue() + " bytes";

            result.setDescription(descriptionText);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-DATA-USAGE-DESCRIPTION-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
