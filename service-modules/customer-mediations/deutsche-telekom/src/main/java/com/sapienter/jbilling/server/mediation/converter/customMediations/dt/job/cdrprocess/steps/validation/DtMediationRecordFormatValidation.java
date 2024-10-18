package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.steps.validation;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;


public class DtMediationRecordFormatValidation implements IMediationStepValidation {

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {

        if (!record.getErrors().isEmpty()) {
            result.addError("JB-INVALID-FORMAT");
            return false;
        }

        return true;
    }
}
