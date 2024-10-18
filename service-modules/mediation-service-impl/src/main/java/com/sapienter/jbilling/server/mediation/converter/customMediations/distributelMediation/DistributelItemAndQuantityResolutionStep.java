/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractUserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import org.apache.commons.lang.StringUtils;


import java.lang.String;
import java.math.BigDecimal;
import java.util.List;


/**
 * Created by igutierrez on 25/01/17.
 */
public class DistributelItemAndQuantityResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {
    private String distributelGenericProduct;
    private String distributelQuantityProduct;

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            boolean passed = true;
            List<PricingField> pricingFields = context.getPricingFields();

            MediationStepResult result = context.getResult();

            BigDecimal quantity = new BigDecimal(Integer.parseInt(distributelQuantityProduct));
            Integer itemId = null;
            String itemDescription = PricingField.find(pricingFields, DistributelMediationConstant.DESCRIPTIVE_TEXT).getStrValue();
            if (StringUtils.isNotEmpty(distributelGenericProduct) && StringUtils.isNotEmpty(distributelQuantityProduct)) {
                itemId = (Integer) resolveItemByInternalNumber(context.getEntityId(), distributelGenericProduct).get(MediationStepResult.ITEM_ID);
            }

            if (quantity == null || BigDecimal.ZERO.compareTo(quantity) == 0) {
                result.addError("ERR-DURATION");
                passed = false;
            }

            if (itemId == null || itemId == 0) {
                result.addError("ERR-ITEM");
                passed = false;
            }

            if (StringUtils.isEmpty(itemDescription)) {
                result.addError("ERR-ITEM");
                passed = false;
            }

            result.setItemId(itemId);
            result.setQuantity(quantity);
            result.setDescription(itemDescription);

            return passed;
        } catch (Exception e) {
            context.getResult().addError("ERR-ITEM_NOT-FOUND");
            return false;
        }
    }


    public String getDistributelGenericProduct() {
        return distributelGenericProduct;
    }

    public void setDistributelGenericProduct(String distributelGenericProduct) {
        this.distributelGenericProduct = distributelGenericProduct;
    }

    public String getDistributelQuantityProduct() {
        return distributelQuantityProduct;
    }

    public void setDistributelQuantityProduct(String distributelQuantityProduct) {
        this.distributelQuantityProduct = distributelQuantityProduct;
    }
}