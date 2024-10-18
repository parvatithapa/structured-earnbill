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

package com.sapienter.jbilling.server.mediation.converter.common.steps;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.Util;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pricing are filtered. Only pricing fields with names in {@code pricingFieldNames} are converted.
 */
public class JMRFilteredPricingResolutionStep extends  AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger logger = new FormatLogger(Logger.getLogger(JMRFilteredPricingResolutionStep.class));

    private Set<String> pricingFieldNames = new HashSet<>(0);

    private static final String DEFAULT_CSV_FILE_SEPARATOR = ",";

    @Override
    public boolean executeStep(MediationStepContext context) {
        List<String> pricingList = new ArrayList<String>();

        for(PricingField field : context.getPricingFields()) {
            if(pricingFieldNames.contains(field.getName())) {
                pricingList.add(PricingField.encode(field));
            }
        }

        if(!pricingList.isEmpty()) {
            String pricing = Util.concatCsvLine(
                    pricingList, DEFAULT_CSV_FILE_SEPARATOR);
            context.getResult().setPricingFields(pricing);
            logger.debug("Pricing found: %s", pricing);
        }

        return true;
    }

    public void setPricingFieldNames(Set<String> pricingFieldNames) {
        this.pricingFieldNames = pricingFieldNames;
    }
}
