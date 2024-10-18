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

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.jmrProcess.DtMediationQuantityResolutionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

/**
 * Resolves quantity, currently a vanilla implementation with no enrichment
 * Enrichment happens once the JMR has been resolved, before order creation.
 * @see DtMediationQuantityResolutionServiceImpl
 */
public class DtOfflineQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String quantityField = "AccumulateFactorValue";

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            BigDecimal qty = context.getPricingField(quantityField).getDecimalValue();

            context.getResult().setOriginalQuantity(qty);
            // this is resolved later when rating unit/scheme may be applied
            context.getResult().setQuantity(BigDecimal.ZERO);
            return true;

        } catch (Exception e) {
            LOG.info("Quantity not found {}", context.getPricingFields());
            context.getResult().addError("ERR-QUANTITY");
            LOG.error("ERR-QUANTITY", e);
            return false;
        }
    }

    public void setQuantityField(String quantityField) {
        this.quantityField = quantityField;
    }
}
