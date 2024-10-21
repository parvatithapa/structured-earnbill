package com.sapienter.jbilling.server.mediation.converter.common.validation;


import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Validation step for validating if the record
 * has been correctly resolved from the converter
 *
 * @author Panche Isajeski
 * @since 12/17/12
 */
public class MediationResultValidationStep implements IMediationStepValidation {

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {

        return validate(result, result.getUserId(), "JB-USER-NOT-RESOLVED") &&
                validate(result, record.getKey(), "JB-INVALID-NULL-RECORD-KEY") &&
                validate(result, result.getCurrencyId(), "JB-CAN-NOT-RESOLVE-USER-CURRENCY") &&
                validate(result, result.getEventDate(), "JB-EVENT-DATE-NOT-RESOLVED") &&
                validate(result, result.getItemId(), "ERR-ITEM-NOT-FOUND") &&
                validate(result, result.getQuantity(), "ERR-QUANTITY") &&
                validate(result, result.getDescription(), "ERR-DESCRIPTION-NOT-FOUND");
    }

    private boolean validate(MediationStepResult result, Object value, String errorMessage) {
        if (value == null) {
            result.addError(errorMessage);
            return false;
        }
        if (value instanceof String && StringUtils.trimToNull((String) value) == null) {
            result.addError(errorMessage);
            return false;
        }
        return true;
    }
}
