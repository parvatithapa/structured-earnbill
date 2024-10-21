package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * @author Neelabh
 * @since Mar 15, 2019
 */
public class InternetDescriptionResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String downloadUsage;
    private final String uploadUsage;
    private final String userNameFieldName;

    public InternetDescriptionResolutionStep(String userNameFieldName, String downloadUsage, String uploadUsage) {
        this.downloadUsage = downloadUsage;
        this.uploadUsage = uploadUsage;
        Assert.notNull(userNameFieldName, "provide userNameFieldName!");
        this.userNameFieldName = userNameFieldName;
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
            PricingField userName = PricingField.find(context.getPricingFields(), userNameFieldName);
            if(null == userName || StringUtils.isEmpty(userName.getStrValue())) {
                result.addError("ERR-USER-NAME-NOT-FOUND");
                return false;
            }
            result.setSource(userName.getStrValue());
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
