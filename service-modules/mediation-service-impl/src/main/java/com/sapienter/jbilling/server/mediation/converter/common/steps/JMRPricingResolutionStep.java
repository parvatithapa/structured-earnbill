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
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public class JMRPricingResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMRPricingResolutionStep.class));
    private static final String DEFAULT_CSV_FILE_SEPARATOR = ",";

    @Override
    public boolean executeStep(MediationStepContext context) {
        List<PricingField> relevantPricingFields = filterPricingFields(context.getPricingFields());
        if (!relevantPricingFields.isEmpty()) {
            List<String> pricingList = new ArrayList<String>();
            for (PricingField field : relevantPricingFields) {
                pricingList.add(PricingField.encode(field));
            }

            String pricing = Util.concatCsvLine(
                    pricingList, DEFAULT_CSV_FILE_SEPARATOR);
            context.getResult().setPricingFields(pricing);

            LOG.debug("Pricing found: " + pricing);
            return true;
        }

        LOG.debug("Pricing is empty: ");
        return true;
    }

    protected List<PricingField> filterPricingFields(List<PricingField> fields) {
        return fields;
    }

}
