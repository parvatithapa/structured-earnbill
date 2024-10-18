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
import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import org.apache.log4j.Logger;

import java.util.stream.Collectors;

import java.lang.Exception;

/**
 * Created by igutierrez on 25/01/17.
 */
public class DistributelPriceResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DistributelPriceResolutionStep.class));
    private static final String DEFAULT_CSV_FILE_SEPARATOR = ",";

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            String pricing = context.getPricingFields().stream()
                    .map(pricingField -> PricingField.encode(pricingField))
                    .collect(Collectors.joining(DEFAULT_CSV_FILE_SEPARATOR));
            context.getResult().setPricingFields(pricing);
            LOG.debug("Pricing Fields -" + pricing);
            return true;
        } catch (Exception e) {
            context.getResult().addError("ERR-PRICING-FIELDS-NOT-FOUND");
            LOG.debug("Exception Occured in FC JMRPricingResolutionStep", e);
            return false;
        }
    }

}