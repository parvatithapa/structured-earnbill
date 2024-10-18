package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;


import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;
import com.sapienter.jbilling.server.util.Constants;

/**
 * @author Harshad
 * @since Dec 19, 2018
 */
public class UserResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String accountIdentifier;

    private SPCMediationHelperService service;

    public UserResolutionStep(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public UserResolutionStep(String accountIdentifier, SPCMediationHelperService service) {
        this.accountIdentifier = accountIdentifier;
        this.service = service;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField accountIdentifierField = PricingField.find(context.getPricingFields(), accountIdentifier);

            if (null == accountIdentifierField) {
                result.addError("ACCOUNT-IDENTIFIER-FIELD-NOT-FOUND");
                return false;
            }

            Map<String, Integer> userCurrencyMap =
                    service.getUserIdForAssetIdentifier(accountIdentifierField.getStrValue().trim());
            if (SPCMediationUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }

            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            String planId = service.getPlanId(result.getUserId(),accountIdentifierField.getStrValue()).toString();
            context.getRecord().addField(new PricingField(Constants.PLAN_ID, planId), false);
            logger.debug("USER-RESOLVED - User Id : {}, Asset Identifier : {}, plan Id : {}",
                    result.getUserId(), accountIdentifierField.getStrValue(), planId);
            return true;

        } catch (Exception ex) {
            result.addError("USER-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
