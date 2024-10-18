package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractItemResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.math.BigDecimal;

/**
 * Created by marcomanzi on 5/30/14.
 */
public class SampleItemResolutionStep extends AbstractItemResolutionStep<MediationStepResult>
{

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            Integer itemId = PricingField.find(context.getRecord().getFields(), "item-id").getIntValue();
            itemId = resolveItemById(context.getEntityId(), itemId);
            Integer quantity = PricingField.find(context.getRecord().getFields(), "duration").getIntValue();
            MediationStepResult result = context.getResult();
            result.setItemId(itemId);
            result.setQuantity(new BigDecimal(quantity));
            result.setDescription("Example description for cdr with itemId:" + itemId + ", userId:" + result.getUserId());
            return true;
        } catch (Exception e) {
            context.getResult().addError("ERR-ITEM_NOT-FOUND");
            return false;
        }

    }

}
