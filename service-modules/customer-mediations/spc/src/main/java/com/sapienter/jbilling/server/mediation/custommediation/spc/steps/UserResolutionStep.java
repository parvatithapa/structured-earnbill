package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;


import java.lang.invoke.MethodHandles;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
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
                    service.getUserIdForAssetIdentifier(accountIdentifierField.getStrValue().trim(), result.getEventDate());
            if (SPCMediationUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }

            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            String assetIdentifier = accountIdentifierField.getStrValue();
            result.setSource(assetIdentifier);
            String planId = service.getPlanId(result.getUserId(), assetIdentifier, result.getEventDate()).toString();
            Optional<Integer> orderIdOptional = service.findSubscriptionOrderByUserAssetEventDate(result.getUserId(), assetIdentifier, result.getEventDate());
            context.getRecord().addField(new PricingField(SPCConstants.PURCHASE_ORDER_ID, orderIdOptional.isPresent() ? orderIdOptional.get() : null), false);
            context.getRecord().addField(new PricingField(Constants.PLAN_ID, planId), false);
            logger.debug("USER-RESOLVED - User Id : {}, Asset Identifier : {}, plan Id : {}",
                    result.getUserId(), accountIdentifierField.getStrValue(), planId);

            if (!orderIdOptional.isPresent()) {
                result.addError("ERR-SUBSCRIPTION-ORDER-NOT-FOUND");
                return false;
            }

            Map<String, Date> map = checkForNull(orderIdOptional.get()) ? service.getActiveSinceAndActiveUntilDates(orderIdOptional.get()) : null;
            java.time.LocalDate localEventDate = checkForNull(result.getEventDate()) ? result.getEventDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

            java.time.LocalDate localActiveSinceDate = checkForNull(map) && checkForNull(map.get(SPCConstants.ACTIVE_SINCE_DATE)) ? 
                    map.get(SPCConstants.ACTIVE_SINCE_DATE).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
            java.time.LocalDate localActiveUntilDate = checkForNull(map) && checkForNull(map.get(SPCConstants.ACTIVE_UNTIL_DATE)) ? 
                    map.get(SPCConstants.ACTIVE_UNTIL_DATE).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

            if(checkForNull(localEventDate) && checkForNull(localActiveSinceDate) && localEventDate.isBefore(localActiveSinceDate)) {
                result.setEventDate(null);
                result.addError("ERR-EVENT-DATE-IS-BEFORE-ACTIVE-SINCE-DATE");
                return false;
            } else if(checkForNull(localEventDate) && checkForNull(localActiveUntilDate) && localEventDate.isAfter(localActiveUntilDate)) {
                result.setEventDate(null);
                result.addError("ERR-EVENT-DATE-IS-AFTER-ACTIVE-UNTIL-DATE");
                return false;
            }

            return true;

        } catch (Exception ex) {
            result.addError("USER-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    private <T> boolean checkForNull(T t) {
        return Optional.ofNullable(t).isPresent();
    }
}
