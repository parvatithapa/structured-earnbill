package com.sapienter.jbilling.server.mediation.customMediations.movius.incomingcall.steps;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;

public class IncomingCallUserResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final String CALLED_ORG_ID_FIELD_NAME = "Called Org Id";

    private static final Logger LOG = LoggerFactory.getLogger(IncomingCallUserResolutionStep.class);

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String calledOrgId = PricingField.find(context.getPricingFields(), CALLED_ORG_ID_FIELD_NAME).getStrValue();
            Map<Integer, Integer> userCurrencyMap = MoviusUtil.getUserIdByOrgIdMetaField(context.getEntityId(), calledOrgId);
            if(MoviusUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }

            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            return true;
        } catch (Exception e) {
            result.addError("USER-NOT-RESOLVED");
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

}
