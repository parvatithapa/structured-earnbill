package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.steps.validation;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Validation step for validating the format of the mediation record
 *
 * @author Panche Isajeski
 * @since 12/17/12
 */
public class MediationRecordFormatValidationStep implements IMediationStepValidation {

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {

        if(!record.getErrors().isEmpty()) {
            result.addError("JB-INVALID_FORMAT");
            return false;
        }

        return true;
    }
}
