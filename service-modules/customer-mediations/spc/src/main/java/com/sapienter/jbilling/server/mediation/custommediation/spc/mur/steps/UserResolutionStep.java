package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;

@Component("optusMurUserResolutionStep")
class UserResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BILLABLE_NUMBER_FIELD_NAME = "Billable Number";

    @Autowired
    private SPCMediationHelperService service;

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField billableNumberField = PricingField.find(context.getPricingFields(), BILLABLE_NUMBER_FIELD_NAME);

            if (null == billableNumberField) {
                result.addError("BILLABLE-NUMBER-NOT-FOUND");
                return false;
            }

            String billableNumber = billableNumberField.getStrValue();
            if(StringUtils.isEmpty(billableNumber)) {
                result.addError("BILLABLE-NUMBER-NOT-FOUND");
                return false;
            }

            if(!service.isIdentifierPresent(billableNumberField.getStrValue())) {
                result.addError("ERR-ASSET-NOT-ASSIGNED-TO-ANY-CUSTOMER");
                logger.error("Asset {} found but is not assigned to any customer", billableNumber);
                return false;
            }

            Map<String, Integer> userCurrencyMap = service.getUserIdForAssetIdentifier(billableNumber);
            if (SPCMediationUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }
            logger.debug("user resolved {} for entity {}", userCurrencyMap, context.getEntityId());
            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            result.setSource(billableNumber);
            return true;

        } catch (Exception ex) {
            result.addError("USER-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
