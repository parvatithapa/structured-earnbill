package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.steps.validation;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.common.validation.DuplicateRecordValidationStep;

import java.util.HashSet;
import java.util.Set;

/**
 * We will only check duplicates for certain CDRs - we will filter based on a pricing field value
 */
public class DtOfflineDuplicateRecordValidationStep extends DuplicateRecordValidationStep {

    private String pricingField = "ProductID";
    private Set<String> pricingFieldValuePrefixSet = new HashSet<>(0);

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {

        String fieldValue = record.getField(pricingField).getStrValue();
        for(String prefix: pricingFieldValuePrefixSet) {
            if(fieldValue.startsWith(prefix)) {
                return true;
            }
        }

        return super.isValid(record, result);
    }

    public void setPricingField(String pricingField) {
        this.pricingField = pricingField;
    }

    public void setPricingFieldValuePrefixSet(Set<String> pricingFieldValuePrefixSet) {
        this.pricingFieldValuePrefixSet = pricingFieldValuePrefixSet;
    }

}
