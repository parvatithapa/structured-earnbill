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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Generate a unique UUID key for certain CDRs. The pricing field {@code pricingField} must have a value starting with
 * one of {@code pricingFieldValuePrefixSet}
 */
public class DtOfflineUpdateKeyStep extends AbstractMediationStep<MediationStepResult> {

    private String pricingField = "ProductID";
    private Set<String> pricingFieldValuePrefixSet = new HashSet<>(0);

    @Override
    public boolean executeStep(MediationStepContext context) {
        String fieldValue = context.getPricingField(pricingField).getStrValue().trim();
        for(String prefix: pricingFieldValuePrefixSet) {
            if(fieldValue.startsWith(prefix)) {
                String key = UUID.randomUUID().toString();
                context.getRecord().setKey(key);
                context.getResult().setCdrRecordKey(key);
            }
        }
        return true;
    }

    public void setPricingField(String pricingField) {
        this.pricingField = pricingField;
    }

    public void setPricingFieldValuePrefixSet(Set<String> pricingFieldValuePrefixSet) {
        this.pricingFieldValuePrefixSet = pricingFieldValuePrefixSet;
    }
}
