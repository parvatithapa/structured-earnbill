/*
` JBILLING CONFIDENTIAL
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

package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.steps;

import com.sapienter.jbilling.server.item.PricingField;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper.MediationHelperService;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves item based on Product Identifier
 */
public class ItemByIdentifierResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private MediationHelperService mediationHelperService;
    private String productField = "ProductID";
    private String extendedField = "ExtendParams";

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            PricingField productPF = context.getPricingField(productField);
            String productId = productPF.getStrValue().trim();
            String extendedParams = context.getPricingField(extendedField).getStrValue().trim();

            Map.Entry<Integer, String> itemEntry = mediationHelperService
                    .resolveItemById(context.getEntityId(), productId, extendedParams);

            if(itemEntry != null) {
                context.getResult().setItemId(itemEntry.getKey());

                PricingField newProductPF = new PricingField(productPF);
                newProductPF.setStrValue(itemEntry.getValue());
                context.getRecord().addField(newProductPF, false);  // isKey immaterial here

                return true;
            } else {
                context.getResult().addError("ERR-ITEM-NOT-FOUND");
                logger.info("Item not found {}", context.getPricingFields());
                return false;
            }
        } catch (Exception e) {
            context.getResult().addError("ERR-ITEM-NOT-FOUND");
            logger.info("Item not found {}", context.getPricingFields());
            return false;
        }
    }

    public void setMediationHelperService(MediationHelperService mediationHelperService) {
        this.mediationHelperService = mediationHelperService;
    }
}
