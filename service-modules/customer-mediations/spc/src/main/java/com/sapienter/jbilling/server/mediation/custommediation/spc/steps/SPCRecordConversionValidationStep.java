package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Created by Neelabh
 */
public class SPCRecordConversionValidationStep implements IMediationStepValidation {

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {
        if(!record.getErrors().isEmpty()) {
            result.addError("JB-INVALID-CDR-DATA");
            return false;
        }
        return true;
    }
}
