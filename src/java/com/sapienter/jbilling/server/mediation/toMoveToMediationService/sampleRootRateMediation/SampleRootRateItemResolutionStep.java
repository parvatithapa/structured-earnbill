package com.sapienter.jbilling.server.mediation.toMoveToMediationService.sampleRootRateMediation;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractRootRouteItemResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.math.BigDecimal;

/**
 * Created by marcomanzicore on 25/11/15.
 */
public class SampleRootRateItemResolutionStep extends AbstractRootRouteItemResolutionStep<MediationStepResult> {
    @Override
    public boolean executeStep(MediationStepContext context) {
        Integer itemId = resolveItemIdByRootRoute(context.getRecord().getMediationCfgId(),
                context.getPricingFields(), context.getEntityId());
        if (itemId == null) {
            context.getResult().addError("ERR-ITEM_NOT-FOUND");
            return false;
        }
        Integer quantity = PricingField.find(context.getRecord().getFields(), "duration").getIntValue();
        if (quantity == null) {
            context.getResult().addError("ERR-QUANTITY");
            return false;
        }

        MediationStepResult result = context.getResult();
        result.setItemId(itemId);
        result.setQuantity(new BigDecimal(quantity));
        result.setDescription("Example description for cdr with itemId:" + itemId + ", userId:" + result.getUserId());
        return true;
    }
}
